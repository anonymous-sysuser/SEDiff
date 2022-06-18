# LLVMCallSitePrinter

An LLVM (opt) pass to print out the callsites of internal functions. 

## Installation
1. Put the LLVM pass under `llvm/lib/Transforms/`.
2. add a `sub_directory` in `llvm/lib/Transforms/CMakeLists.txt`.
```sh
add_subdirectory(LLVMCallSitePrinter)
```
3. Compile LLVM: https://llvm.org/docs/GettingStarted.html

## Use
1. Compile the target software into LLVM bitcode.
2. Run the LLVM pass.
```sh
cd llvm-project/build
./bin/opt -load lib/LLVMCallSitePrinter.so -LLVMCallSitePrinter < test.bc
```

## Run on CoreUtils 8.21
1. Compile CoreUtils into LLVM bitcode.
We use a docker from https://github.com/kferles/llvm-coreutils.
2. Run
```sh
for filename in /path/to/coreutils/llvm/*.bc
do
  ./bin/opt -load lib/LLVMCallSitePrinter.so -LLVMCallSitePrinter < "$filename" > /dev/null;
done
```

