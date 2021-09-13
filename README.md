# nanoeth

nanoeth is a simple implementation of Ethereum's execution layer (it is concerned with processing
and validating transactions and blocks, as originally specified in the Yellowpaper).

You could describe nanoeth as:
- an executable specification for the Ethereum Yellowpaper
- a toy Ethereum client without networking capabilities

nanoeth is meant to be simple, clear, well-documented, and interactive. See the [Design
Principles](guide/principles.md) page for more information.

**General Links**
- [Javadoc](http://norswap.com/nanoeth/)

**Components**
- [Versions](/src/com/norswap/nanoeth/versions/) (includes a list of EIPs)
- [RLP](/src/com/norswap/nanoeth/rlp/)
- [Signatures](/src/com/norswap/nanoeth/signature/)
- [Transactions](/src/com/norswap/nanoeth/transactions/)
- [Receipts](/src/com/norswap/nanoeth/receipts/) (including logs & bloom filters)
- [Merkle Patrica Tree](/src/com/norswap/nanoeth/trees/patricia)

**Running Tests**

To run nanoeth, you will need Java 15 or more. I recommend installing through [sdkman].

nanoeth tests depend on the [ethereum/tests][tests] repository. Please clone this repository, then
do `echo ethereumTests=PATH_TO_REPO >> gradle.properties` (substituting `PATH_TO_REPO` for the
actual local path where you cloned the repository).

You can then run the tests with `./gradlew test` (`gradlew.bat test` on Windows). This will run
the Gradle wrapper that will fetch the appropriate version of Gradle to run the test, so the first
run might have some extra delays.

[tests]: https://github.com/ethereum/tests
[sdkman]: https://sdkman.io/install