# RLP

Implementation of the Recursive Length Prefix (RLP) format, as specified in appendix B of the
[Ethereum yellowpaper][yellow].

[yellow]: https://ethereum.github.io/yellowpaper/paper.pdf

Encoding goes from an `RLPItem` to a `Bytes` object (from `com.norswap.nanoeth.data`), and
decoding goes in the reverse direction.

- Encoding is performed via `RLPItem.encode()` or `RLP.encode(RLPItem)`
- Decoding is performed via `RLP.decode(Bytes)`

`RLPItem` has two forms:
- `RLPBytes` - wraps a `Bytes` object
- `RLPSequence` - a sequence of other `RLPItem`