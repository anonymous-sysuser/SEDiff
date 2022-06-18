/*
   american fuzzy lop - LLVM-mode instrumentation pass
   ---------------------------------------------------

   Written by Laszlo Szekeres <lszekeres@google.com> and
              Michal Zalewski <lcamtuf@google.com>

   LLVM integration design comes from Laszlo Szekeres. C bits copied-and-pasted
   from afl-as.c are Michal's fault.

   Copyright 2015, 2016 Google Inc. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at:

     http://www.apache.org/licenses/LICENSE-2.0

   This library is plugged into LLVM when invoking clang through afl-clang-fast.
   It tells the compiler to add code roughly equivalent to the bits discussed
   in ../afl-as.h.

 */

#define AFL_LLVM_PASS

#include "../../config.h"
#include "../../debug.h"

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fstream>
#include <iostream>

#include "llvm/ADT/Statistic.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/IR/LegacyPassManager.h"
#include "llvm/IR/Module.h"
#include "llvm/Support/Debug.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"
#include "llvm/IR/DebugInfo.h"

#include "llvm/Support/raw_ostream.h"

using namespace llvm;

namespace {

  class AFLCoverage : public ModulePass {

    public:

      static char ID;
      AFLCoverage() : ModulePass(ID) { }

      bool runOnModule(Module &M) override;

      // StringRef getPassName() const override {
      //  return "American Fuzzy Lop Instrumentation";
      // }

  };

}
class FileReader{
public:
    FileReader(std::string filename);
    ~FileReader();
    bool readLine(std::string * str);
private:
    std::ifstream  *infile;
    std::string filename;
};

FileReader::FileReader(std::string filename) :filename(filename){
    infile = new std::ifstream(filename, std::ios::in);
    assert(infile->is_open());
}

FileReader::~FileReader(){
    infile->close();
    delete infile;
}

bool FileReader::readLine(std::string * str){
    if(!infile->eof()){
        getline(*infile,*str);
        return true;
    }else{
        return false;
    }
}

std::vector<std::string> favoredBBList = {};
int readFavoredBBListFlag = 0;

static void rtrim(std::string &s) {
    s.erase(std::find_if(s.rbegin(), s.rend(), [](unsigned char ch) {
        return !std::isspace(ch);
    }).base(), s.end());
}

void readFavoredBBList() {
    std::string filePath = "static.txt";
    std::string eachLine;
    FileReader fr(filePath);

    while(fr.readLine(&eachLine)) {
        rtrim(eachLine);
        if(eachLine != "")
            favoredBBList.push_back(eachLine);
        /* outs()  << eachLine << "\n";  */
    }
}


/* check if the BB should be favored by reading static analysis results */
bool checkFavorBB(std::string *funcName, BasicBlock *BB){
    /*
    std::string BBName;
    //BBName = BB->getName();
    //outs() << BBName << "::" << *funcName <<"<\n";
    //if (BBName.empty()) {
        std::string str;
        raw_string_ostream OS(str);
        BB->printAsOperand(OS, false);
        std::string functionName = BB->getParent()->getName();
        BBName = functionName + OS.str();
        BB->setName(BBName);
    //}
    return std::find(favoredBBList.begin(), favoredBBList.end(), BBName) != favoredBBList.end();
    */
  for (Instruction &I : *BB) {
      const DILocation *DIL = I.getDebugLoc();
      if(!DIL) {
        outs()  << "empty\n";
        continue;
      }

        std::string Filename = DIL->getFilename().str();
        std::size_t found = Filename.rfind("/"); // find the last delimer
        std::string instLoc = Filename.substr((found == std::string::npos)? 0 : found +1) + ":" + std::to_string(DIL->getLine());
        //outs() << instLoc << "\n";
        if(std::find(favoredBBList.begin(), favoredBBList.end(), instLoc) != favoredBBList.end())
            return true;
  }
  return false;
}

char AFLCoverage::ID = 0;

bool AFLCoverage::runOnModule(Module &M) {

  LLVMContext &C = M.getContext();

  IntegerType *Int8Ty  = IntegerType::getInt8Ty(C);
  IntegerType *Int32Ty = IntegerType::getInt32Ty(C);

  /* Show a banner */

  char be_quiet = 0;

  if (isatty(2) && !getenv("AFL_QUIET")) {

    SAYF(cCYA "afl-llvm-pass " cBRI VERSION cRST " by <lszekeres@google.com>\n");

  } else be_quiet = 1;

  /* Decide instrumentation ratio */

  char* inst_ratio_str = getenv("AFL_INST_RATIO");
  unsigned int inst_ratio = 100;

  if (inst_ratio_str) {

    if (sscanf(inst_ratio_str, "%u", &inst_ratio) != 1 || !inst_ratio ||
        inst_ratio > 100)
      FATAL("Bad value of AFL_INST_RATIO (must be between 1 and 100)");

  }

  /* Get globals for the SHM region and the previous location. Note that
     __afl_prev_loc is thread-local. */

  GlobalVariable *AFLMapPtr =
      new GlobalVariable(M, PointerType::get(Int8Ty, 0), false,
                         GlobalValue::ExternalLinkage, 0, "__afl_area_ptr");

  GlobalVariable *AFLHybridPtr = 
      new GlobalVariable(M, PointerType::get(Int32Ty, 0), false,
                        GlobalValue::ExternalLinkage, 0, "__afl_hybrid_ptr");

  GlobalVariable *AFLPrevLoc = new GlobalVariable(
      M, Int32Ty, false, GlobalValue::ExternalLinkage, 0, "__afl_prev_loc",
      0, GlobalVariable::GeneralDynamicTLSModel, 0, false);

  /* Read the favored BB list */
  if(readFavoredBBListFlag == 0) {
      readFavoredBBListFlag = 1;
      readFavoredBBList();
  }

  /* Instrument all the things! */

  int inst_blocks = 0;
  for (auto &F : M){
      std::string functionName = F.getName();
    for (auto &BB : F) {

      BasicBlock::iterator IP = BB.getFirstInsertionPt();
      IRBuilder<> IRB(&(*IP));


      /* Make up cur_loc */

      unsigned int cur_loc = AFL_R(MAP_SIZE);

      ConstantInt *CurLoc = ConstantInt::get(Int32Ty, cur_loc);

      /* Load prev_loc */

      LoadInst *PrevLoc = IRB.CreateLoad(AFLPrevLoc);
      PrevLoc->setMetadata(M.getMDKindID("nosanitize"), MDNode::get(C, None));
      Value *PrevLocCasted = IRB.CreateZExt(PrevLoc, IRB.getInt32Ty());

      /* Load SHM pointer */

      LoadInst *MapPtr = IRB.CreateLoad(AFLMapPtr);
      MapPtr->setMetadata(M.getMDKindID("nosanitize"), MDNode::get(C, None));
      Value *MapPtrIdx =
          IRB.CreateGEP(MapPtr, IRB.CreateXor(PrevLocCasted, CurLoc));

      /* Update bitmap */

      LoadInst *Counter = IRB.CreateLoad(MapPtrIdx);
      Counter->setMetadata(M.getMDKindID("nosanitize"), MDNode::get(C, None));
      Value *Incr = IRB.CreateAdd(Counter, ConstantInt::get(Int8Ty, 1));
      IRB.CreateStore(Incr, MapPtrIdx)
          ->setMetadata(M.getMDKindID("nosanitize"), MDNode::get(C, None));

      /* Our jobs here */
      if (checkFavorBB(&functionName, &BB)) {
          // do instrumentation; update the hybrid
          LoadInst *HybridPtr = IRB.CreateLoad(AFLHybridPtr);
          HybridPtr->setMetadata(M.getMDKindID("nosanitize"), MDNode::get(C, None));
          Value *HybridPtrIdx =
              IRB.CreateGEP(HybridPtr, IRB.CreateXor(PrevLocCasted, CurLoc));

          LoadInst *HybridCounter = IRB.CreateLoad(HybridPtrIdx);
          HybridCounter->setMetadata(M.getMDKindID("nosanitize"), MDNode::get(C, None));
          Value *HybridPtrIncr = IRB.CreateAdd(HybridCounter, ConstantInt::get(Int32Ty, 1)); // current weight is 1 by default. Can adjust it.
          IRB.CreateStore(HybridPtrIncr, HybridPtrIdx)
              ->setMetadata(M.getMDKindID("nosanitize"), MDNode::get(C, None));
      }


      if (AFL_R(100) >= inst_ratio) continue;
      /* Set prev_loc to cur_loc >> 1 */

      StoreInst *Store =
          IRB.CreateStore(ConstantInt::get(Int32Ty, cur_loc >> 1), AFLPrevLoc);
      Store->setMetadata(M.getMDKindID("nosanitize"), MDNode::get(C, None));

      inst_blocks++;

    }
  }

  /* Say something nice. */

  if (!be_quiet) {

    if (!inst_blocks) WARNF("No instrumentation targets found.");
    else OKF("Instrumented %u locations (%s mode, ratio %u%%).",
             inst_blocks, getenv("AFL_HARDEN") ? "hardened" :
             ((getenv("AFL_USE_ASAN") || getenv("AFL_USE_MSAN")) ?
              "ASAN/MSAN" : "non-hardened"), inst_ratio);

  }

  return true;

}


static void registerAFLPass(const PassManagerBuilder &,
                            legacy::PassManagerBase &PM) {

  PM.add(new AFLCoverage());

}


static RegisterStandardPasses RegisterAFLPass(
    PassManagerBuilder::EP_OptimizerLast, registerAFLPass);

static RegisterStandardPasses RegisterAFLPass0(
    PassManagerBuilder::EP_EnabledOnOptLevel0, registerAFLPass);
