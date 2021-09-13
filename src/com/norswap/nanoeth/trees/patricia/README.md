# Modified Merkle Patricia Tree

Let's first unpack the name. A modified merkle patricia tree (henceforth: "patricia tree") is:

1. A [trie (aka a radix tree)][radix], a tree datastructure implementing a dictionary (map)
   interface, where keys are sequences of items. In its basic form, each layer in the tree consumes
   an item from the sequence (the tree depth is therefore equal to the largest key's size). Each
   node may have up to as many nodes as there are possible items. The name "patricia tree" is, as
   far as I've been able to understand, just as seldom used synonym for "trie". Not sure why it was
   picked.
2. In particular, Ethereum's patricia trees are *hexary* tries, meaning each node has up to 16
   children. The keys are simply arrays of items, therefore, each "item" in the sequence is a 4 bit
   block (also known as *nibble*). This goes neatly to the hex string (e.g. 0xF00) where each
   character maps to a nibble.
3. The "Merkle" part of the name, means that patricia trees, in addition to being hexary tries, are
   also [Merkle trees][merkle]. A Merkle tree is a tree where each node is labelled with a
   cryptographic hash. This enables making merkle proofs, which let a prover show that a value is
   part of the tree to a verifier that only knows the hash of the root node of the tree (called the
   *Merkle root*). More on this below.
4. Finally, the trees are "modified" because a few optimizations are made. In particular, these
   patricia trees allow "compressing" chains of nodes with only a single children into leaf nodes (
   when the last child is a leaf node with data) or extension nodes (when the last child has
   multiple children). More on this below as well.

[radix]: https://en.wikipedia.org/wiki/Radix_tree
[merkle]: https://en.wikipedia.org/wiki/Merkle_tree

## Architecture

nanoeth currently bundles an in-memory implementation of the patricia tree (in the
`trees.patricia.memory`) package.

However, this implementation is not realistic in practice, as the mainnet account tree is itself
bigger than 20GB — serialization to disk is therefore a must.

The architecture of the package enables different implementations to be swapped in. A tree is
represented by the `PatriciaTree` class, which uses the memory implementation by default. However,
it's possible to change the implementation by subclassing this class and overriding the
`createLeafNode` method to return a new implementaiton of the `PatriciaNode` interface.

In general, users of the tree only need to interact with the `PatriciaTree` class, however, in
keeping with the [design principles](/guide/principles.md), the in-memory implementation can be
interacted with for learning purposes.

## Uses

The modified Merkle patricia tree is used in a few places in Ethereum:

- To implement the state trie, which maps 20-byte addresses to address data.
    - Its Merkle root is stored in each block (representing the post-state of the block).
- To implement the account storage trie (one per contract address), which maps the 32-byte keccak
  hash of 32-byte storage keys to their associated values.
    - Its Merkle root is stored in state trie's account data for the contract address.
    - Together with the state trie, those are the two main relevant uses of patricia trees for which
      we need to optimize.
- Each block stores the Merkle root of a trie constructed from the block's transaction.
    - The keys are the RLP-encoding of the transaction indices, the values are the RLP-encoding of
      the transactions.
- Each block stores the Merkle root of a trie constructed from the block's transaction receipts.
    - The keys are the RLP-encoding of the receipts indices, the values are the RLP-encoding of
      the receipts.

## Realistic Implementations

### In-Memory Speed-Ups

There are two angles to consider here. As we have already mentionned, a realistic implementation
must be able to store and retrieve tree to/from disk (and for latency reasons, this disk has to
be a SSD if one hopes to validate mainnet in real time).

However, even if we consider only in-memory implementations, there are alternative design that
are potentially more performance.

The in-memory tree implementation is a "real tree" implementation. Meaning the tree data structure
is actually built. This means that, accessing the data takes an expected login the worst case,
accessing data takes `log_16(number of keys)` hops in the tree, probably causing costly accesses
to memory not already in the CPU cache. For a non-trivial number of key, this is expected
to be slower than using a hash table mapping keys to values directly (which structurally needs much
fewer hops: you only need to index the bucket array that underpins the hash table).

Of course, the hash table does not give the Merkle root, which is the point of using the tree.
The key is to realize we don't need to compute the Merkle root after every tree change. So we
can batch update the state and storage trees only once after all the transactions in a block have
been processed. This already saves us the overhead on tree reads.

This can be optimized further by building a trie of the modified entries, then batch inserting
these entries by performing "trie fusion". This avoids walking the same prefix multiple times. An
even faster way to achieve the same thing is to sort the entries by lexicographically ordering
the keys. This makes sure that all the descendants of a node (nodes starting with the same prefix)
are contiguous, and enables efficient recursive insertions in the tree.

### Disk Storage

A detailed exploration of disk-friendly implementation will come when I come around to implementing
it. For now, it suffices to say that existing Ethereum clients typically use a key-value store (like
LevelsDB or RocksDB) to associate keys with values, and to associate internal nodes (identified by
the prefix needed to reach them) to their children their cached Merkle root (1).

(1) Strictly speaking, the result of the yellow paper node composition, as the Merkle root itself
(TRIE function) can introduce additional hashing when the node composition function does not itself
output a hash. 
