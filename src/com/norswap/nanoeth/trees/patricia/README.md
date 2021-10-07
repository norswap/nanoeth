# Modified Merkle Patricia Trees

Table of contents:
- [What is a Modified Merkle Patricia Tree?](#what-is-a-modified-merkle-patricia-tree)
- [Uses](#uses)
  - [Merkle Proofs](#merkle-proofs)
- [Node Cap Values](#node-cap-values)
- [Tree Layout: Linked vs Store-Backed Trees](#tree-layout-linked-vs-store-backed-trees)
  - [Sending Nodes on the Network](#sending-nodes-on-the-network)
  - [Store-Backed Trees](#store-backed-trees)
- [Architecture](#architecture)
- [Optimized Usage](#optimized-usage)
- [Disk Storage](#disk-storage)

## What is a Modified Merkle Patricia Tree?

Let's first unpack the name. A modified merkle patricia tree (henceforth: "patricia tree" even
though that theoretically refers to a much broader class of trees) is:

1. A [trie (aka a radix tree)][radix], a tree data structure implementing a dictionary (map)
   interface, where keys are sequences of items. In its basic form, each layer in the tree consumes
   an item from the sequence (the tree depth is therefore equal to the largest key's size). Each
   node may have up to as many nodes as there are possible items. The name "patricia tree" is, as
   far as I've been able to understand, just a seldom-used synonym for "trie". Not sure why it was
   picked.
2. In particular, Ethereum's patricia trees are *hexary* tries, meaning each node has up to 16
   children. The keys are simply arrays of items, therefore, each "item" in the sequence is a 4 bit
   block (also known as *nibble*). This goes neatly to the hex string (e.g. 0xF00) where each
   character maps to a nibble.
3. The "Merkle" part of the name, means that patricia trees, in addition to being hexary tries, are
   also [Merkle trees][merkle]. A Merkle tree is a tree where each node is labelled with a
   cryptographic hash of its content (in reality: the "cap value", see below). This enables making
   merkle proofs, which let a prover show that a value is part of the tree to a verifier that only
   knows the hash of the root node of the tree (called the *Merkle root*). More on this below.
4. Finally, the trees are "modified" because a few optimizations are made. In particular, these
   patricia trees allow "compressing" chains of nodes with only a single children into leaf nodes
   (when the last child is a leaf node with data) or extension nodes (when the last child has
   multiple children). More on this below as well.

[radix]: https://en.wikipedia.org/wiki/Radix_tree
[merkle]: https://en.wikipedia.org/wiki/Merkle_tree

## Uses

The modified Merkle patricia tree is used in a few places in Ethereum:

- To implement the state trie, which maps 20-byte addresses to address data (nonce, balance, code
  hash and root of storage tree for contract addresses).
    - Its Merkle root is stored in each block (representing the post-state of the block).
- To implement the account storage trie (one per contract address), which maps the 32-byte keccak
  hash of 32-byte storage keys to their associated values.
    - Its Merkle root is stored in state trie's account data for the contract address.
    - Together with the state trie, those are the two main relevant uses of patricia trees for which
      we need to optimize.
- Each block stores the Merkle root of a trie constructed from the block's transaction.
    - The keys are the RLP-encodings of the transaction indices, the values are the RLP-encodings of
      the transactions.
- Each block stores the Merkle root of a trie constructed from the block's transaction receipts
  (most importantly, the receipts is where logs (the structure underlying Solidity's events) are
  stored).
    - The keys are the RLP-encoding of the receipts indices, the values are the RLP-encoding of
      the receipts.

Essentially, we use a patricia tree to get hashes (the Merkle root) on big data sets. The use of a
Merkle tree is particularly interesting for the state and account storage trees, because the Merkle
root can be computed incrementally: if the balance of a single account changes, we don't need to
recompute the hash for every node in the tree, only those of the nodes in the branch from the root
to the leaf containing the balance (provided we memoized the cap value (~hash) of each existing
node, which we do).

(In general, we'll call "a branch" such a sequence of nodes that goes from a root to a leaf.)

Merkle trees are also very useful for the ability to create Merkle proofs.

### Merkle Proofs

Given a Merkle root, a Merkle proof proves that a key-value pair belongs to the tree that was used
to generate the Merkle root. It can also prove that a key does not have an entry in the tree.

Such a proof is simply made out of the nodes on the branch from the root to the leaf containing the
value (or to the deepest node that would need to be modified to add the key, in case of a proof of
absence). Obviously, these nodes do not recursively include their children (otherwise we'd be
sending the whole tree). Instead, they contain the cap value (~ hash) of their children.

To verify such a proof, it suffices to:

1. verify the Merkle root for the first (root) node.
2. verify that the branch matches the key.
3. recompute the cap value of every node, starting from the leaf (which includes the proven value),
   and verify that it matches the cap value recorded in the parent node.

Interestingly, Ethereum does not make much direct use of Merkle proofs. It is used at the networking
layer, in the [snap sync] protocol, to prove the first and last key-value pair in a range. This
enables proving that the sender did send all key-value pairs in the range, and did not omit any. They
are also used in the [LES] and [PIP] protocols, but those are seldom-used protocols for
light/stateless clients, with [new alternatives] being worked on.

More generally, Merkle proofs are used implicitly. Whenever you request a tree node using the (main)
[ETH] protocol, you can verify that the answer is correct by checking that the node conforms to
the constraints of the Merkle tree. This is also how stateless clients use Merkle proofs: as a means
to check the nodes they request from the network, which they never actually insert into a full tree.

Merkle proofs can also be used to prove receipts (& hence logs) belong to the Merkle tree whose root
is stored in a block.

[snap sync]: /src/network/sync/README.md
[LES]: https://github.com/ethereum/devp2p/blob/master/caps/les.md
[PIP]: https://github.com/ethereum/devp2p/blob/master/caps/pip.md
[ETH]: https://github.com/ethereum/devp2p/blob/master/caps/eth.md
[new alternatives]: https://github.com/ethereum/trin

## Node Cap Values

We mentionned multiple times that nodes are identified by their cap values, which are more or less
a hash of their contents.

This cap value is obtained by:

1. RLP-encoding a node using the *structural node composition `c`* from
   the yellowpaper (equation 197 and previous).
2. running this encoding through the *node cap function `n`* (equation 194). This function returns
   the Keccak hash of the encoding if it is 32 bytes or bigger, and the encoding itself otherwise,
   which is what I call the "node cap value".

A few remarks:
- These are pretty terrible function names.
- "Node cap value" is my own term — this value doesn't seem to have a common name. Geth confusingly
  conflates it with a hash, some you have some field called `hash` that can contain a node cap
  value, while some other can only hold a real 32 byte Keccak hash.
- I might sometimes slip from my own name and call the node cap value a "digest" instead.
- The < 32 bytes condition is a ill-conceived optimization, designed to save memory, but saving
  [meaningless amounts of memory in practice][rlp-bad].
- `c` and `n` functions are mutually recursive: the return value of `c` includes the node cap value of the
  children of the node.
- The `c` and `n` functions are implemented by the methods `PatriciaNode#compose()` and
  `PatriciaNode#cap()`, respectively.

Finally, an important remark: the output of the `c` function is also how nodes are being transmitted
over the network. It's a "non-recursive" representation of a node (because the children are not
included, only their cap values). Consequences of this are expounded in the next section.

[rlp-bad]: https://medium.com/@gballet/structure-of-a-binary-state-tree-part-1-48c587836d2f#is-rlp-really-needed

## Tree Layout: Linked vs Store-Backed Trees

If you've implemented a tree before, you probably know the traditional way to do it: each node
includes references (pointers) to its children. I will call this representation a "linked tree". For
multiple reasons, this representation doesn't work very well on Ethereum:

1. The Ethereum state (account trie + state storage tries) takes ~20-50GB of space (depending on how
   nodes are represented / laid out). This is too much to fit in RAM on most computers.
2. When transmitted over the network, trie nodes are encoded in a "non-recursive" representation.

### Sending Nodes on the Network

Let's talk about the networking aspect. You can't send memory pointers over the network, and so you
have two solutions: (A) you specify the encoding of a node to recursively include the encoding of
its children, or (B) you replace the pointer by some kind of unique node identifier.

(A) is not practical because, again, sending the state root node would mean sending tens of GB of
data in a single message. This is not realistic: this would put too much demand on another single
node (instead of getting data from many nodes), and there is too much risk that the connection would
be interrupted before the end of the transfer. Also it means that the other side must keep a
snapshot of the state that it is sending you, something that costs storage space (not to mention
implementation complexity).

So in practice, we have to go with (B). The identifier is simply the node cap value, as described
[above](#node-cap-values).

Let's zoom back a second. Why do we even need to send nodes over the network — as opposed to sending
key-value pairs? The short answer is (1) to perform sync and (2) to verify the validity of the
values received over the network via Merkle proofs. For sync, please refer to the [sync README],
which explains the challenges associated with sending key-value pairs (which is something that is
done anyway in snap sync, though with some caveats). For Merkle proofs, please refer to [the
corresponding section above](#merkle-proofs).

[sync README]: /src/network/sync/README.md

### Store-Backed Trees

So state trees are too big to be represented as a classical linked tree. A solution there is to
include "lazy nodes" in the linked tree, i.e. nodes whose children are stored on disk, and only
loaded on disk on-demand (expanding the tree with new concrete nodes).

An almost identical solution is to use a "store-backed tree". Instead of having pointers to children
nodes, you simply store the node cap of the children. Then, when you want to access the children, you
consult the *store*, which is a `cap 🡢 node` map. In reality it is an interface that can have a
variety of implementations: from a classical in-memory hash table, to being backed by a key-value
store; or a mix of both, like an in-memory cache in front of a key-value store.

This store-backed representation also has the advantage that it's the same representation returned
by the structural node composition function `c`, which is also how nodes are transmitted over the
network.

nanoeth implements both linked trees and store-backed trees, sharing 90% of the logic between both.
I decided to keep linked trees in because they are much easier to debug. They are also a bit faster
whenever the disk is not involved (e.g. when computing the transaction hash). More explanations in
the next section.

## Architecture

The "user" (i.e. other packages) will mostly interact with patricia tree through the `PatriciaTree`
class. Each tree is associated with a `NodeStore` instance. Node stores have two responsabilities:

1. supply a `cap 🡢 PatriciaNode` interface (where `PatriciaNode` is the parent class of all
   patricia tree nodes).
2. offer various node constructors that return subclasses of `PatriciaNode`.

Currently, we include two `NodeStore` implementation:

- `MapNodeStore` (in the `store` package), which stores all nodes in Java `HashMap`. Its constructors return
  `PatriciaNode` implementations defined in the `store` package.
- `TreeNodeStore` (in the `linked` package), which does not implement the node storage/retrieval
  interface — because it is used by linked tree, which do not need a backing store. Its constructors
  return `PatriciaNode` implementations defined in the `linked` package.

New `NodeStore` implementations can be created, but its methods should only be called by
`PatriciaNode` implementations. Both stores and node implementations assume that they are used with
the associated store/node types.

In general, users of the tree mostly need to interact with the `PatriciaTree` class
([documentation][ptree-doc]), however, in keeping with the [design
principles](/guide/principles.md), a lot of other classes and methods can be interacted with for
learning purposes.

An instance of `PatriciaTree` is in principle immutable, though it contains a reference to a store,
which is mutable. That being said, cap values should statistically never collide, so the mutability
of the store is really a question of cache eviction policy: the store will contain any node added to
any tree backed by the store, minus those that have been explicitly removed from the store. We may
want to remove nodes from a store to prune old states.

Whenever a "mutating" operation is performed against the tree, the method returns the new tree.
If nodes were added/removed, the corresponding `NodeStore` method was called. As implied above,
calling `NodeStore#removeNode` does not guarantee that the node will be removed from the store
(instead, we may just record the fact to enable future future). That being said, the `MapNodeStore`
does immediately remove nodes from the store.

Besides trees, nodes are also immutable. Similarly, node operations return a modified version of
node.

Note that store-backed nodes do not keep a reference to stores (only a tree does). As a result,
most node methods take a store as parameter.

[ptree-doc]: http://norswap.com/nanoeth/com/norswap/nanoeth/trees/patricia/PatriciaTree.html

A useful function is `PatriciaNode.parse(RLP)` which creates a store-backed node from
some RLP (typically received from the network).

Such nodes can be collected in an array to create a `MerkleProof` which can be checked with
`MerkleProof#verify()`. Such a Merkle proof can also be constructed by calling
`PatriciaTree#prove(byte[])`.

## Optimized Usage

First of all, it should be noted that the current in-memory implementation does memoize the node cap
of every node in the tree. With this crucial optimization, we avoid traversing the whole tree when
computing the new top-level Merkle root after a change to the tree.

Note that we should not write the state trees when executing transactions — instead we should write
to a cache, which we only write to memory after confirming the validity of the block. However, this
cache can be used to load all the nodes we will need to generate the new tree nodes (i.e. the
siblings of every node on every branch to a written key). This will make generating the new
nodes/tree extremely fast as we do not need to hit the disk. The nodes will still need to be written
to the disk however.

The fastest way to insert all entries from the temporary cache into the tree starts by sorting the
entries by lexicographically ordering of the keys. This makes sure that all the descendants of a
node (nodes starting with the same prefix) are contiguous. This enables efficient batch-insertion in
the tree, saving us the trouble of walking the same branch prefix multiple times.

(Note we currently do not do these last two things, since we do not yet build the state tree.)

## Disk Storage

A detailed exploration of disk-friendly implementation will come when I come around to implementing
it. For now, it suffices to say that existing Ethereum clients typically use a key-value store (like
LevelsDB or RocksDB) to associate keys with values, and to associate internal nodes (identified by
the prefix needed to reach them) to their children their cached Merkle root (1).

Also note that for latency reasons, this disk has to  be a SSD if one hopes to validate mainnet in
real time.