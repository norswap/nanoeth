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

## Understanding the value of the first byte of an RLP-encoded item

- Single byte encoding (`[0, 127]`): 128 items which are encoded as themselves (single byte)
  
- Direct bytes size encoding (`[128, 183]`): 56 items representing the size of a byte sequence in
  the `[0, 55]` range
  
- Indirect bytes size encoding (`[184, 191]`): 8 items representing the size of a byte sequence
  in the `[1, 8]` range, encoding other byte sequences whose size is in the [55, 2^64[ range
  
- Direct items size encoding (`[192, 247]`): 56 items representing the serialized size of an item
  sequence in the `[0, 55]` range
  
- Indirect items size encoding (`[248, 255]`): 8 items representing the size of a byte sequence
  in the `[1, 8]` range, encoding item sequences whose serialized size is in the `[55, 2^64]`
  range.

Note that the "serialized size" of an item sequence matches s(x) in the yellowpaper. It is the
sum of the RLP-encoded size of every item in the sequence, and hence exclude the space needed to
encode the size of the item sequence itself.

The values 128, 184, 192 and 248 mark boundaries on the value of the first byte, indicating a
change in encoding. We repreent them in the implementation as constants whose name ends with
`LIMIT`

The values 128, 183, 192 and 247 (note the occasional off-by-one) are similarly special, as
they represent a base that is added to a size to encode it. We represent them in the
implementation as constants whose name ends with `SUMMAND`.