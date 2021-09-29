# Client Sync

(Sync is not implemented, this is just informative.)

Table of contents:
- [Sync Modes](#sync-modes)
- [Syncing An Evolving Chain](#syncing-an-evolving-chain)
- [Faster Sync with Snap](#faster-sync-with-snap)

## Sync Modes

Go-ethereum (geth) has [three main sync modes][geth-sync]:

1. **Full sync**. In this mode a client downloads, verifies and executes every block. No state sync
   is needed because the full state is derived from the execution. The problem is that a full sync
   on the Ethereum mainnet takes on the order of weeks to perform.
2. **Fast sync**. All blocks are downloaded, the proof of work is validated, as well as the fact
   that the "parent hash" in each header does match the hash of the parent block's header. Once we
   get close to the current block, we switch to full sync, but for that we need to download the
   state. More on how this is done [below](#syncing-an-evolving-chain). In November 2020, it took
   around 10 hours to sync the state (ignoring blocks!) using snap sync.
3. **Snap sync**. Using this [protocol][snap], a large range of state addresses can be queried at
   the same time. The internal state tree nodes for that range can then be reconstructed internally.
   In November 2020, it took around 2 hours to sync the state (ignoring blocks!) using snap sync.

There is also **beam sync**, which is only available on Nethermind. In beam sync, state tree nodes
are downloaded lazily (on demand) from the network. In practice for Nethermind, specifying "beam
sync" means doing a fast sync but starting to validate the tip of the chain immediately using beam
sync. In theory, using it alone enables fully stateless clients: clients that do not keep any state
locally (or only as a cache), pulling nodes from the network instead. The concern with enabling beam
sync on its own is that it puts too much demand on benevolent stateful nodes. One proposed solution,
being worked on in the [Trin] client, is to organize stateless clients as a distributed hash table.

Quick overview of what other clients offer:

- [Nethermind][nm-sync]: full, fast, beam+fast
- Erigon: only a modified ["staged" version][staged] of full?
- [Besu][besu-sync]: full, fast

[geth-sync]: https://geth.ethereum.org/docs/getting-started#sync-modes
[snap]: https://github.com/ethereum/devp2p/blob/master/caps/snap.md
[Trin]: https://github.com/ethereum/trin
[nm-sync]: https://docs.nethermind.io/nethermind/ethereum-client/sync-modes
[besu-sync]: https://besu.hyperledger.org/en/stable/Reference/CLI/CLI-Syntax/#sync-mode
[staged]: https://github.com/ledgerwatch/erigon/blob/devel/eth/stagedsync/README.md

## Do we need all blocks?

A quick remark: geth always fetches all blocks (including block bodies — Nethermind allows just
getting the headers). This is not strictly necessary.

Theoretically, you at least need to download all headers to validate the chain. But you don't need
to keep these headers around in storage. You will need to keep the N most recent blocks to handle
re-orgs (the bigger N, the bigger the reorg you can handle without resorting to sync).

If you can find a block hash off-chain that you trust, you don't even need to validate the chain —
you can just start by trusting this block and downloading the state for it.

I speculate there are two reasons why geth always downloads all blocks (including block bodies),
even though skipping old blocks makes sync faster and reduces storage requirements for clients:

1. Skipping old blocks means that are less clients able to assist new clients with a "proper"
   sync (i.e. one that at least validates proof-of-work + the chain of hashes).
2. Starting from a trusted hash is problematic because you need to get this hash from somewhere.
   Inevitably people will trust one or two places to supply this hash, which opens a big centralized
   attack vector. In practice, it probably won't matter becaue the network is already running.
   Still, this is a bad norm to promote for what a trustless decentralized system.

## Syncing an Evolving Chain

A key concept when trying to understand non-full sync modes work is that the chain keeps growing
while we are syncing. This means that the state keeps changing.

To read ahead you should understand how the state is structured in Ethereum, i.e. as a set
of *modified Merkle patricia tree*. Refers to the [README of the patricia package][patricia] to
learn more.

[patricia]: /src/com/norswap/nanoeth/trees/patricia

The "state" is really the state tree + a set of account storage trees. In what follows, we'll act
as though there is only a single tree, i.e. each account storage tree is a subtree rooted in the
account data (leafs) of the state tree. Conceptually, this is how storage of the state in a key-value
store works (i.e. keys are either `<account_address>` or `<account_address> + <storage_key>`).

(Note that since storage is "memory", I'll often refer to a "storage key" as an "address".)

Fast sync works because it download tree nodes, and not simply key-value pairs. Tree nodes can
be identified by their cap value (see [README][patricia], but you can think of it has a hash derived
from their content). Each node includes the cap value of its children.

Say that you start with a node that is the root of the state at a given block. You use this node
to get its children, then you get the children of those, etc, until you have all nodes. You now have
the full state, *for the particular block you started at*. But the chain moved in the meantime!

However, the chain will not completely overwrite its own state over a few hours (or even a few
weeks). And so you can restart the process from the new root node, and you'll find that you already
have most of the nodes you need. After doing that, you might still be behind, but repeat the process
enough and you'll eventually catch up — because with each iteration you're missing less and less
nodes.

## Faster Sync with Snap

An alternative to fast sync is to "just" get key-value pairs, and to rebuild the tree from them.

But how do you know which keys to query? You can't query the whole 2^256 addresses for every smart
contract on the chain! Either you're back to walking the tree using nodes as in fast-sync, or you
need to request a **range** of addresses, and the remote node will tell you which addresses have
data, along with the associated values.

Getting address ranges is interesting because the problem with snap sync is precisely this iterative
tree-walking process: it involves a lot of latency. Imagine you have to fetch a small subtree of
size 256. Given that our patricia tree are hexary (each branch node has up to 16 children), it's
minimum depth is 3. This means we'll need at least three network round-trips to get all
of these values, which we could easily queried in parallel otherwise.

Getting address ranges is challenging however. The problem is again that you need to walk the tree
to get the addresses in the range. This time, the client that sends you the range has to do it. It's
easier for him (he does not incur the overhead of network roundtrips), but it turns out it's still
quite hard on the disk.

The solution involved a [significant amount of engineering][snap-pr] in order to make addresses
iterable in reasonable time (i.e. without walking the tree). This involves keeping a "snapshot" of
the state (hence: *snap* sync): "essentially a semi-recent full flat dump of the account and storage
contents". See [the link above][snap-pr] for details on how these snapshots are created (and
updated) iteratively. This iteration system is great, and has other uses besides. The bad news is
that this increases the state storage overhead (~ +50% compared to just storing tree nodes).

Note that the term "snapshot" is somewhat misleading, because in practice, the "snapshot" is
constantly evolving. It is however based on an old-ish block (128 blocks behind the tip of the
chain).

This also means that when running snap sync, you will get a mix of data from different state roots.
This means that the higher-level nodes in the state tree you generate are most likely going to be
nodes that have never existed under any state root! This is fine. After you've "completed" snap sync
(i.e. requested and gotten the whole address range), you can switch to fast sync to update to the
changed values. You will need to do this anyway, because as mentionned the snapshot is based on an
old block.

[snap-pr]: https://github.com/ethereum/go-ethereum/pull/20152