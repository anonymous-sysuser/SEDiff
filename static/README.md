# Static Component
This part is a bit complicated. Sorry I do not fully automate it.

## Instructions
1. Refer to the [doc](juxta/README.md) to install Juxta. We use Juxta for the data-flow analysis of original implementations of internal functions.
2. Put the compatible original implementations of internal functions into `juxta/analyzer/` and refer to `juxta/wrapper.sh`to compilation options.
3. Install a symbolic execution and prepare for the data-flow analysis. This requires some manual efforts and configurations. 
    - We use Navex as the target symbolic execution engine here.
    - Check the [documents](https://github.com/aalhuz/navex) to install our [navex](navex) instance.
    - The code `juxta/analyzer/code.py` creates the normalized formulas for the data-flow analysis.
4. Run the `analyzer/ckcross.py` checker for the mapping. It generates a result file, which describes the code that should be specially instrumented for dynamic differential fuzzing.
