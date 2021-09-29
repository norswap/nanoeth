package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPParsingException;
import com.norswap.nanoeth.trees.patricia.store.StorePatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.store.StorePatriciaExtensionNode;
import com.norswap.nanoeth.utils.Hashing;
import java.util.Map;

import static com.norswap.nanoeth.rlp.RLPParsing.getBytes;
import static com.norswap.nanoeth.rlp.RLPParsing.getItems;

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

    // =============================================================================================

    /**
     * Parses a node from a RLP object in the format returned by {@link #compose()}.
     * <p>
     * The returned will be a store-backed node ({@link StorePatriciaBranchNode} or {@link
     * StorePatriciaExtensionNode}) or a {@link PatriciaLeafNode}.
     * <p>
     * It's the caller responsibility to add the node to a store, if necessary. Similarly, this also
     * doesn't validate that the cap value of children (if any) are valid with respect to any
     * specific store.
     */
    public static PatriciaNode parse (RLP rlp) throws RLPParsingException {
        var items = getItems(rlp);

        if (items.length == 2) {
            var bytes = getBytes(rlp, 0);
            var nibbles = Nibbles.fromHexPrefix(bytes);
            return ((bytes[0] & 0x20) != 0) // is the node a leaf?
                ? new PatriciaLeafNode(nibbles, getBytes(rlp, 1))
                : new StorePatriciaExtensionNode(nibbles, getChildCap(rlp.itemAt(1)));
        }

        if (items.length == 17) {
            var children = new byte[16][];
            for (int i = 0; i < 16; ++i)
                children[i] = getChildCap(rlp.itemAt(i));
            return new StorePatriciaBranchNode(getBytes(rlp, 16), children);
        }

        throw new RLPParsingException("wrong sequence size for patricia tree node: " + items.length);
    }

    // ---------------------------------------------------------------------------------------------

    private static byte[] getChildCap (RLP rlp) {
        return rlp.isBytes()
                ? rlp.bytes()
                : rlp.encode();
    }

    // =============================================================================================

    /** Memoization for {@link #cap()}. */
    protected @Nullable byte[] cap;

    // ---------------------------------------------------------------------------------------------

    /** Returns the value associated with the node, or null if no value is associated. */
    public abstract @Nullable byte[] value();

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
    public abstract BranchStep step (NodeStore store, Nibbles keySuffix);

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
     * This method implements the node cap function n (equation 194 in the yellowpaper).
     * <p>
     * This method memoizes its result. This is an important optimization which avoids traversing
     * the whole tree whenever recomputing the Merkle root after a change to the tree.
     */
    public final byte[] cap() {
        if (cap != null)
            return cap;
        byte[] encoding = compose().encode();
        return cap = encoding.length < 32
            ? encoding
            : Hashing.keccak(encoding).bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Packages the given cap as a RLP byte array if it's a hash (length 32) or as an
     * already-encoded RLP sequence if it's not.
     */
    static RLP wrappedCap (byte[] cap) {
        return cap.length == 32
            ? RLP.bytes(cap)
            : RLP.encoded(cap);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * This method implements the structural composition function c (equation 197 and previous in
     * the yellowpaper). See the README of this package for more information.
     */
    public abstract RLP compose();

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the Merkle root of the Merkle tree rooted at this node. This implements the TRIE
     * function in the yellowpaper (equation 195).
     */
    public final MerkleRoot merkleRoot() {
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
