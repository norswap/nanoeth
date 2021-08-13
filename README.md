# nanoeth

nanoeth is a small code model for Ethereum. You could describe it as:
- an executable specification for the Ethereum Yellowpaper
- a toy Ethereum client without networking capabilities

**Links**
- [Javadoc](http://norswap.com/nanoeth/)
- [Versions Package](/src/com/norswap/nanoeth/versions/) (includes a list of EIPs)
- [RLP Package](/src/com/norswap/nanoeth/rlp/)
- [Signature Package](/src/com/norswap/nanoeth/signature/)
- [Transaction Package](/src/com/norswap/nanoeth/transactions/)

[Javadoc]: https://javadoc.io/doc/com.norswap/nanoeth/
[Mirror]: https://jitpack.io/com/github/norswap/nanoeth/-SNAPSHOT/javadoc/

**Goals**
- specify Ethereum behaviour in terms code and unit tests
- enable playing with example data interactively
- clear mapping with the Yellowpaper
- simplicity & clarity
- using a wealth of (wrapper) types to achieve the above objectives and make the codebase easy to
  navigate

**Non-Goals**
- efficiency

**Limitations**
- Limited to handling byte sequences of less than `2^31 - 8` (a conservative approximation of the
  max Java array size). In theory, Ethereum should handle byte arrays up to size 2^64. In pratice,
  real clients don't do much better, and it would be too costly to pay for storage greater than the
  max Java array size.