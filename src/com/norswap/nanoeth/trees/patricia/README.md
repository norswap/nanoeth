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

(Note we currently do not do these things, since we do not yet build the state tree.)

It should be noted that the current in-memory implementation does memoize the "Merkle root" (1) of
every node in the tree. With this crucial optimization, we avoid traversing the whole tree when
computing the new top-level Merkle root after a change to the tree.

(1) Strictly speaking, the result of the yellow paper node composition, as the Merkle root itself
(TRIE function) can introduce additional hashing when the node composition function does not itself
output a hash.

### Disk Storage

A detailed exploration of disk-friendly implementation will come when I come around to implementing
it. For now, it suffices to say that existing Ethereum clients typically use a key-value store (like
LevelsDB or RocksDB) to associate keys with values, and to associate internal nodes (identified by
the prefix needed to reach them) to their children their cached Merkle root (1).

## Abridged Tree Nodes

Central to our work with Merkle tree are two functions defined in the yellow paper: the structural
node composition function `n` (equation 197 and previous), and the node cap function `c` (equation
194). These are, imho pretty terrible names.

The composition function returns an RLP layout for a node. However, this is a not a recursive
layout. Instead, each child is represented by the result of the node cap function.

What is the result of the node cap function? It's either the RLP encoding of the result of the node
composition function, if it is small enough (< 32 bytes), or the Keccak hash thereof otherwise.
I typically call the result of this function "the cap", though I might slip and call it "a digest"
instead.

So instead of recursively including the RLP for children, we typically include their hash instead.
The < 32 bytes "optimization" is a design wart, designed to save memory, but saving [meaningless
amounts of memory in practice][rlp-bad].

These functions are implemented by the methods `PatriciaNode#compose()` and `PatriciaNode#cap()`.

[rlp-bad]: https://medium.com/@gballet/structure-of-a-binary-state-tree-part-1-48c587836d2f#is-rlp-really-needed

This representation doesn't really have a name, but I call it "abridged representation". It has
three major uses:

1. computing the Merkle root
2. transmitting nodes over the network
3. transmitting Merkle proofs

Point 1 is pretty trivial: the Merkle root of a tree is its cap value if it's a hash (i.e. 32
bytes long), or the hash of its cap value. You can actually compute the "Merkle root" of any node
in this fashion.

Onwards to point 2. You might wonder why transmit tree nodes over the network and not just value.
First, this has to do with the naive way to do state synchronization, which was used until recently:
to sync the current (or a recent) state tree, request the root node, **identified by its Merkle
root** (people will often just say "its hash"). Get its abridged representation from other nodes on
the network. This gives you the cap value of its children, from which you can compute their Merkle
root, which you can then request on the network. Proceed until you have the whole tree.

But wait! This takes time, and meanwhile, the state is evolving! No worries, just restart the
process from the new state root. If you stored the nodes you got earlier in some kind of key-value
store, you already have most of the nodes (since each block only touches a very small part of the
tree). Repeat this process a couple time and you will eventually catch up to the most recent tree
root.

The process I just described is how "fast" sync works in geth. Fast sync was the default
until June 2021, where it was replaced by ["snap" sync][snap]. Snap sync mostly gets a bunch of
key-value pairs in a single go, and builds up the internal node locally. This avoids the latency of
waiting for each node to get its children, but on the flip side comes with [significant
implementation complexity][snap-complex].

[snap]: https://github.com/ethereum/devp2p/blob/master/caps/snap.md
[snap-complex]: https://github.com/ethereum/go-ethereum/pull/20152

Besides sync, the ability to transmit nodes over the network is also useful for stateless
clients that do not maintain a full view of the state, but get state values from the network
whenever needed. These clients do also need to get internal nodes from the network in order to be
able to verify Merkle roots!

And this leads us to point 3. A Merkle proof is a way to prove that a key-value pair belongs to a
Merkle tree with a given Merkle root. To make the proof, it suffices to supply every node (in
abridged form) on the **branch** of the key: the path from the root of the tree to the leaf or
branch node that contains the value. It's also possible to prove the absence of key from a tree — in
which case the branch stops at the deepest node that would have to be modified to insert a mapping
for the given key.

To verify the proof, simply check that the first node matches the Merkle root, then that the cap
value of each node matches its parent, then that the final node matches the proven value.

In contract to abridged nodes, I call a "full tree node" a node that has references to its children.

nanoeth represents abridged nodes by the class `AbridgedNode` and full nodes by the class
`PatriciaNode`. An `AbridgedNode` can be created from a RLP object received on the network, or via
`PatriciaNode#abridged()`.