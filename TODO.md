# TODO

- main roadmap
  - memoize Merkle trees
  - Merkle proofs
  - either
    - implement EVM
    - implement Verkle trees
  - write developper guide detailing the architecture
  - implement/document EIP-1559 logic & validate London block tests

- more testing
  - transactions: test non-0 & high chain IDs
  - add (transaction-level) test cases for EIP2930 transactions
  - add (transaction-level) test cases for EIP1559 transactions
    - use them to verify DebugUtils.dumpTxFromJson

- niceties
  - implement a REPL
  - intra-javadoc links?
  - auto-generate package.java from README.md?
  - update to Java 16 and pattern matching in MemPatriciaNode#prepend (and other places maybe)
  - rethink how configuration (Config etc) works
    - maybe let Blocks.DB be part of the context?
      
- yellowpaper fixes
  - merkle tree extension node index