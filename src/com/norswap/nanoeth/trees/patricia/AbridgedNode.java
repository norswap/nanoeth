package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPParsingException;
import com.norswap.nanoeth.trees.patricia.PatriciaNode.Type;
import com.norswap.nanoeth.utils.Hashing;

import static com.norswap.nanoeth.rlp.RLPParsing.getBytes;
import static com.norswap.nanoeth.rlp.RLPParsing.getItems;
import static com.norswap.nanoeth.trees.patricia.PatriciaNode.Type.*;

/**
 * Represents a {@link PatriciaNode} that has been composed via the yellowpaper's structural node
 * composition function c (equation 197).
 * <p>
 * In particular, such a representation only tracks the "cap" (the result of the yellowpaper node
 * cap function n) for its children with size {@code <= 32}, which is why we call this
 * representation "abridged node".
 */
public final class AbridgedNode {

    // ---------------------------------------------------------------------------------------------

    public final PatriciaNode.Type type;

    // ---------------------------------------------------------------------------------------------

    /**
     * The key segment or suffix associated with this node.
     * <p>
     * Only for leaf or extension nodes.
     */
    public final @Nullable Nibbles keySegment;

    // ---------------------------------------------------------------------------------------------

    /**
     * The value associated with this node.
     * <p>
     * Only for branch nodes (where it can be null) and leaf nodes.
    */
    public final @Nullable byte[] value;

    // ---------------------------------------------------------------------------------------------

    /**
     * The cap values (i.e. result of the yellowpaper's node cap function n - equation 196) of all
     * children of this node, if it is a branch node (size 16) or an extension node (size 1).
     * <p>
     * For empty slots in branch nodes, an array of size 0 is used.
     */
    public final @Nullable byte[][] childrenCaps;

    // ---------------------------------------------------------------------------------------------

    /**
     * Result of the yellowpaper cap function n (equation 194) for this node.
     */
    public final byte[] cap;

    // ---------------------------------------------------------------------------------------------

    public AbridgedNode (
            Type type,
            @Nullable Nibbles keySegment,
            @Nullable @Retained byte[] value,
            @Nullable @Retained byte[][] childrenCaps,
            @Retained byte[] cap) {
        this.type = type;
        this.keySegment = keySegment;
        this.value = value;
        this.childrenCaps = childrenCaps;
        this.cap = cap;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates an abridged node representation from a RLP object in the format returned by
     * {@link PatriciaNode#compose()}.
     */
    public static AbridgedNode parse (RLP rlp) throws RLPParsingException {
        var items = getItems(rlp);

        if (items.length == 2) {
            var bytes = getBytes(rlp, 0);
            var nibbles = Nibbles.fromHexPrefix(bytes);
            return ((bytes[0] & 0x20) != 0) // is the node a leaf?
                ? new AbridgedNode(LEAF, nibbles, getBytes(rlp, 1), null, cap(rlp))
                : new AbridgedNode(EXTENSION, nibbles, null,
                    new byte[][] { getChildCap(rlp.itemAt(1)) }, cap(rlp));
        }

        if (items.length == 17) {
            var children = new byte[16][];
            for (int i = 0; i < 16; ++i)
                children[i] = getChildCap(rlp.itemAt(i));
            return new AbridgedNode(BRANCH, null, getBytes(rlp, 16), children, cap(rlp));
        }

        throw new RLPParsingException("wrong sequence size for patricia tree node: " + items.length);
    }

    // ---------------------------------------------------------------------------------------------

    private static byte[] getChildCap (RLP rlp) {
        return rlp.isBytes()
                ? rlp.bytes()
                : rlp.encode();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Implement the cap function, given the RLP representation of a node.
     */
    private static byte[] cap (RLP rlp) {
        byte[] encoding = rlp.encode();
        return encoding.length < 32
            ? encoding
            : Hashing.keccak(encoding).bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the Merkle root of the Merkle tree rooted at this node. This implements the TRIE
     * function in the yellowpaper (equation 195).
     */
    public MerkleRoot merkleRoot() {
        var cap = cap();
        return cap.length == 32
            ? new MerkleRoot(cap)
            : new MerkleRoot(Hashing.keccak(cap));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the result of the node cap function n (equation 194 in the yellowpaper), which is the
     * RLP encoding of the result of {@link PatriciaNode#compose()} if its size is less than 32, or
     * a Keccak hash thereof otherwise.
     */
    public byte[] cap() {
        return cap;
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the number of nibbles of {@code keySuffix} that this node can consume. */
    public int consumablePrefix (Nibbles keySuffix) {
        if (keySuffix.length() == 0)
            return 0;
        return switch (type) {
            case LEAF, EXTENSION
                -> keySegment.sharedPrefix(keySuffix);
            case BRANCH
                -> childrenCaps[keySuffix.get(0)] == null ? 0 : 1;
        };
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the cap of the child node reachable through {@code keySuffix}. The {@code
     * consumablePrefix} parameter is the result of the {@link #consumablePrefix(Nibbles)} function
     * (passed in to avoid repeat computation). Return null if no such child is reachable.
     */
    public @Nullable byte[] capForSuffix (int consumablePrefix, Nibbles keySuffix) {
        if (keySuffix.length() == 0)
            return null;
        return switch (type) {
            case LEAF
                -> null;
            case EXTENSION
                -> consumablePrefix == keySegment.length()
                    ? childrenCaps[0]
                    : null;
            case BRANCH
                -> childrenCaps[keySuffix.get(0)];
        };
    }

    // ---------------------------------------------------------------------------------------------
}
