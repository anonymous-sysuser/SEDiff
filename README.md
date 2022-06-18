# SEDiff Artifact
Performing symbolic execution unavoidably encounters internal functions (e.g. library functions) that provide basic operations such as string processing.
Many symbolic execution engines construct internal function models that abstract function behaviors for scalability and compatibility concerns.  
Due to the high complexity of constructing the models, developers intentionally summarize only partial behaviors of a function, namely modeled functionalities, in the models.  
The correctness of the internal function models is critical because it would impact all applications of symbolic execution, e.g. bug detection and model checking.

SEDiff is a scope-aware differential testing framework.
SEDiff designs a novel algorithm to automatically map the modeled functionalities to the code in the original implementations.  
It then applies scope-aware grey-box differential fuzzing to relevant code in the original implementations. 
SEDiff also equips a new scope-guided input generator and a tailored bug checker that help efficiently and correctly detect erroneous inconsistencies.  

## Repository Structure
```sh
SEDiff
├── LICENSE
├── LLVMCallSitePrinter 
├── PHPCallSitePrinter
├── README.md
├── afl
├── data
└── static
```

## Instructions
- The `LLVMCallSitePrinter` and `PHPCallSitePrinter` are used to analyze callsite information in LLVM bitcode and PHP code.
- The `static` is the static analysis component of SEDiff.
- The `afl` is the differential fuzzing component of SEDiff.

Please refer to the notes within these folders for detailed instructions.

## License
This artifact is generally under MIT License. 
However, the tools it is built on retain their original licenses.
