#!/bin/bash
ROOT=$(pwd)
FSCK=$ROOT/llvm/tools/clang/tools/scan-build/fss-build
CLANG=$ROOT/bin/llvm/bin/clang
APP=$ROOT/analyzer/test/
OUT=$ROOT/juxta-data/
$FSCK --use-analyzer=$CLANG --fss-output-dir=$OUT make -C $APP CC=$CLANG

