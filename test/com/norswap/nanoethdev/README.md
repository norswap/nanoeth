# Dev Tools

This package contains scripts that are useful for development work.

This is bundled with the tests because these scripts are currently very small, and the overhead of
compiling them along with the tests is small, while the complexity overhead of making a separate
project for them is large. Having the scripts under the `test/` directory (the "test source root")
makes it easy to run them from IDEs.