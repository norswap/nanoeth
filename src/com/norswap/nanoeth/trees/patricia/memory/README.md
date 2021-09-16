## In-memory Modified Merkle Patricia Tree

This package implements the modified Merkle patricia tree in-memory, by implementing
`PatriciaNode` in `MemPatriciaNode` and its subclasses.

This is the default implementation used by nanoeth in `PatriciaTree`.

This is not realistic in general, as the mainnet state trie is > 20GB.

See [the trees.patricia package README](../README.md) for more details.

In particular, regarding the in-memory implementation:

> It should be noted that the current in-memory implementation does memoize the "Merkle root" (1) of
> every node in the tree. With this crucial optimization, we avoid traversing the whole tree when
> computing the new top-level Merkle root after a change to the tree.
>
> (1) Strictly speaking, the result of the yellow paper node composition, as the Merkle root itself
> (TRIE function) can introduce additional hashing when the node composition function does not
> itself output a hash.