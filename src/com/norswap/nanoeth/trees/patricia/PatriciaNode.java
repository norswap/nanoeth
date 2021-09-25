package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.utils.Hashing;
import java.util.Map;

/**
 * Abstract base class for Modified Merkle Patricia Tree nodes, to be compatible with {@link
 * PatriciaTree}.
 * <p>
 * nanoeth includes a in-memory patricia tree implementation (in the {@code memory} sub-package),
 * but such an implementation is not realistic in practice as the mainnet account trie is itself
 * larger than 20GB. The inclusion of this interface enables more efficient implementation to be
 * plugged into nanoeth, by subclassing {@link PatriciaTree}.
 */
public abstract class PatriciaNode {

    // ---------------------------------------------------------------------------------------------

    /** See {@link #step(NodeStore, Nibbles)}. */
    public static final class Step {
        public final PatriciaNode node;
        public final PatriciaNode child;
        public final int sharedPrefix;
        public final int nibblesLeft;

        public Step (PatriciaNode node, PatriciaNode child, int sharedPrefix, int nibblesLeft) {
            this.node = node;
            this.child = child;
            this.sharedPrefix = sharedPrefix;
            this.nibblesLeft = nibblesLeft;
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** Memoization for {@link #cap()}. */
    protected byte[] cap;

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a qudruplet of:
     * <ol><li>
     * this node.
     * </li><li>
     * the child in which to look for the value for the given key suffix (or equivalently, the child
     * to modify to insert a value to be associated with the given key suffix). Can be null if no
     * such child exists.
     * </li><li>
     * the number of nibbles shared between this node and the key suffix. For leaf and extension
     * node, this will be the length of the shared prefix the key suffix and the node's key
     * fragment. For branch nodes, this will be 0 if the returned node is {@code this} and 1
     * otherwise.
     * </li><li>
     * the number of nibbles left in the key suffix after deducting the shared nibbles.
     * </li></ol>
     */
    public abstract Step step (NodeStore store, Nibbles keySuffix);

    // ---------------------------------------------------------------------------------------------

    /**
     * Lookup the entry with the given key suffix, the suffix of a sequence of nibbles, where the
     * missing prefix was used to reach the present node.
     * <p>
     * This must handle empty nibble sequences.
     */
    public abstract byte[] lookup (NodeStore store, Nibbles keySuffix);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed node, after associating the given key with the given key suffix.
     * <p>
     * This must handle empty nibble sequences.
     * <p>
     * The method must add its return value to the store, and remove the current node from the
     * store. Any other node added/removed from the tree in the process must similarly be
     * added/removed from the store.
     */
    public abstract PatriciaNode add (NodeStore store, Nibbles keySuffix, byte[] value);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed node, after removing the entry for the given key suffix (if any), or
     * returns {@code null} if the removal of the key means that the node itself must disappear.
     * <p>
     * This must handle empty nibble sequences.
     * <p>
     * The method must add its return value to the store, and remove the current node from the
     * store. Any other node added/removed from the tree in the process must similarly be
     * added/removed from the store.
     */
    public abstract PatriciaNode remove (NodeStore store, Nibbles keySuffix);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns an {@link AbridgedNode} with the abridged information for the node.
     */
    public abstract AbridgedNode abridged();

    // ---------------------------------------------------------------------------------------------

    /**
     * This method implements the node cap function n (equation 194 in the yellowpaper). This
     * is computed via {@link AbridgedNode#computeCap(RLP)}.
     * <p>
     * This method memoizes its result. This is an important optimization which avoids traversing
     * the whole tree whenever recomputing the Merkle root after a change to the tree.     *
     */
    public final byte[] cap() {
        if (cap == null)
            cap = abridged().cap();
        return cap;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the Merkle root of the Merkle tree rooted at this node. This implements the TRIE
     * function in the yellowpaper (equation 195).
     */
    public final MerkleRoot merkleRoot() {
        // The same logic appears in AbridgedNode
        var cap = cap();
        return cap.length == 32
            ? new MerkleRoot(cap)
            : new MerkleRoot(Hashing.keccak(cap));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Adds all the entries store under this node to {@code map}, given that the prefix to reach
     * this node is given by {@code prefix}.
     */
    public abstract void collectEntries (NodeStore store, Nibbles prefix, Map<byte[], byte[]> map);

    // ---------------------------------------------------------------------------------------------
}
