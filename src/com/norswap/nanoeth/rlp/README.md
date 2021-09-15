# RLP

Implementation of the Recursive Length Prefix (RLP) encoding, as specified in appendix B of the
[Ethereum yellowpaper][yellow].

[yellow]: https://ethereum.github.io/yellowpaper/paper.pdf

Strictly speaking, the specified process allows to construct the "RLP encoding" (a byte array) of a
structure made of (potentially empty) sequences and byte arrays.

We do not deal in pure nested sequences and byte arrays but in Java object, where we might want to
store some additional information. So we need an intermediate stage where we construct the "RLP
layout" of an object. Here is an example for transactions:

```
Transaction   --[ layout ]-->  RLP Layout   --[ encode ]--> bytes[]
             <--[ parse  ]--               <--[ decode ]--
```

Object that have a RLP layout will implement the `RLPLayoutable` class and implement the
`rlpLayout()` method. Such object can also typically be created from a RLP layout method, often
using a static factory method (for instance `Transaction#from(RLP)`) — we call this "parsing" the
RLP layout. The class `RLPParsing` includes utility methods to help parse RLP layouts.

The RLP layout itself is reprsented by a `RLP` object. Each `RLP` object represents a sequence of
other RLP objects (`RLP[]`), a byte array (`byte[]`), or (as a special case), an encoded RLP item.

Encoding goes from an `RLP` object to a `byte[]` object, and  decoding goes in the reverse direction.

- Encoding is performed via `RLP#encode()`.
- Decoding is performed via `RLP.decode(byte[])`

The encoding & decoding logic proper is in the package-local `RLPEncoding` class.

We allow `RLP` to wrap RLP encodings to enable incremental RLP encoding. Because RLP is a recursive
format, we do not need to know the layout of already-encoded items in order to compute the RLP
encoding of layouts that include them. This is notably useful to incrementally compute Merkle root
in the modified Mekle patricia tree (`trees.patricia` package). The encoded bytes can be accessed
via `RLP#encode()`, while the layout can be retrieved via `RLP#inflate()`.

## Understanding the value of the first byte of an RLP encoding 

- Single byte encoding (`[0, 127]` aka `[0x0, 0x7f]`): 128 items which are encoded as themselves (
  single byte).

- Direct bytes size encoding (`[128, 183]` aka `[0x80, 0xb7]`): 56 items representing the size of a
  byte sequence in the `[0, 55]` range.

- Indirect bytes size encoding (`[184, 191]` aka `[0xb8, 0xbf]`): 8 items representing the size of a
  byte sequence in the `[1, 8]` range, encoding other byte sequences whose size is in
  the `[55, 2^64[` range.

- Direct items size encoding (`[192, 247]` aka `[0xc0, 0xf7]`): 56 items representing the serialized
  size of an item sequence in the `[0, 55]` range.

- Indirect items size encoding (`[248, 255]` aka `[0xf8, 0xff]`): 8 items representing the size of a
  byte sequence in the `[1, 8]` range, encoding item sequences whose serialized size is in
  the `[55, 2^64]` range.

Note that the "serialized size" of an item sequence matches s(x) in the yellowpaper. It is the
sum of the RLP-encoded size of every item in the sequence, and hence exclude the space needed to
encode the size of the item sequence itself.

The values 128, 184, 192 and 248 mark boundaries on the value of the first byte, indicating a
change in encoding. We repreent them in the implementation as constants whose name ends with
`LIMIT`

The values 128, 183, 192 and 247 (note the occasional off-by-one) are similarly special, as
they represent a base that is added to a size to encode it. We represent them in the
implementation as constants whose name ends with `SUMMAND`.

### Zero Length Sequences & Zero Values

Something that might not be immediately obvious from the above is that a zero-length byte sequence
is encoded as the single byte 128, while a zero-length item sequence is encoded as the single byte
192.

Something even less obvious, and not actually related to the RLP encoding itself, is that Ethereum
encodes the number 0 as a zero-length byte sequence (instead of encoding it as a single 0 byte
value). This happens for contract calls with zero transferred value, for instance.
