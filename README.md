# nanoeth

nanoeth is a small code model for Ethereum. You could describe it as:
- an executable specification for the Ethereum Yellowpaper
- a toy Ethereum client without networking capabilities

**Goals**
- specify Ethereum behaviour in terms code and unit tests
- enable playing with example data interactively
- clear mapping with the Yellowpaper
- simplicity & clarity
- using a wealth of (wrapper) types to achieve the above objectives and make the codebase easy to
  navigate

**Non-Goals**
- ability to handle the real blockchain (i.e. the model will have limitations for the sake of
  convenience and clarity)
- efficiency

**Limitations**
- RLP encoding is limited to data that can be serialized in less than `2^31 - 8` bytes (a
  conservative approximation of the max Java array size. Managing the true limit would require
  careful managing disk storage.