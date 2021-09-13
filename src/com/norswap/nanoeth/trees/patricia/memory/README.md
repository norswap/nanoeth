## In-memory Modified Merkle Patricia Tree

This package implements the modified Merkle patricia tree in-memory, by implementing
`PatriciaNode` in `MemPatriciaNode` and its subclasses.

This is the default implementation used by nanoeth in `PatriciaTree`.

This is not realistic in general, as the mainnet state trie is > 20GB.

See [the trees.patricia package README](../README.md) for more details.