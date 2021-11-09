# TODO

- bandersnatch
  - clean up by rewriting to the schem under crypto/curve package
    - reread Tonelli Shanks, implement TODO (& learn) there and in curve/Bandersnatch regarding it
    - implement point operations in curve/Bandersnatch
    - implement BouncyCastleCurve
    - make a backup and transition operations to use curve/Curve instead of crypto/Curve
    - test things with a BouncyCastleCurve tuned for Bandersnatch
    - then test with the real deal?
      - or rather, include the real as a sub and validate every intermediate step
  - implement test.py?
- verkle
  - once fixed
    - only compute g, h at the end
    - get rid of second vector?
    - use barycentric formula

- main roadmap
  - Natural: force radix to avoid mistakes 
  - validate node types?
  - fix ugly dump code
  - implement Verkle trees
  - implement EVM
  - write developper guide detailing the architecture

- niceties
  - update to Java 16 and pattern matching in MemPatriciaNode#prepend (and other places maybe)
    - data classes?
    - unreachable switches on enums
  - rethink how configuration (Config etc) works
    - maybe let Blocks.DB be part of the context?

- future
  - implement a REPL
      
- yellowpaper fixes
  - merkle tree extension node index