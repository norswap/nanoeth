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

    // =============================================================================================
    // region Fields and Constructor
    // =============================================================================================

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
     * For empty slots in branch nodes, {@code null} is used.
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
            @Nullable @Retained byte[][] childrenCaps) {
        this(type, keySegment, value, childrenCaps, null);
    }

    // ---------------------------------------------------------------------------------------------

    public AbridgedNode (
            Type type,
            @Nullable Nibbles keySegment,
            @Nullable @Retained byte[] value,
            @Nullable @Retained byte[][] childrenCaps,
            RLP rlp) {
        this.type = type;
        this.keySegment = keySegment;
        this.value = value;
        this.childrenCaps = childrenCaps;
        this.cap = computeCap(rlp != null ? rlp : compose());
    }

    // endregion
    // =============================================================================================
    // region Parsing
    // =============================================================================================

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
                ? new AbridgedNode(LEAF, nibbles, getBytes(rlp, 1), null, rlp)
                : new AbridgedNode(EXTENSION, nibbles, null,
                    new byte[][] { getChildCap(rlp.itemAt(1)) }, rlp);
        }

        if (items.length == 17) {
            var children = new byte[16][];
            for (int i = 0; i < 16; ++i)
                children[i] = getChildCap(rlp.itemAt(i));
            return new AbridgedNode(BRANCH, null, getBytes(rlp, 16), children, rlp);
        }

        throw new RLPParsingException("wrong sequence size for patricia tree node: " + items.length);
    }

    // ---------------------------------------------------------------------------------------------

    private static byte[] getChildCap (RLP rlp) {
        return rlp.isBytes()
                ? rlp.bytes()
                : rlp.encode();
    }

    // endregion
    // =============================================================================================

    private final static RLP EMPTY_BYTE_ARRAY = RLP.bytes(new byte[0]);

    // ---------------------------------------------------------------------------------------------

    /**
     * Packages the given cap as a RLP byte array if it's a hash (length 32) or as an
     * already-encoded RLP sequence if it's not.
     */
    private static RLP wrappedCap (byte[] cap) {
        return cap.length == 32
            ? RLP.bytes(cap)
            : RLP.encoded(cap);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Implement the node cap function n (equation 194 in the yellowpaper), given the RLP
     * representation of a node (see {@link #compose()}.
     * <p>
     * Access the cap of the abridged node via {@link #cap()}.
     */
    public static byte[] computeCap (RLP rlp) {
        byte[] encoding = rlp.encode();
        return encoding.length < 32
                ? encoding
                : Hashing.keccak(encoding).bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * This method implements the structural composition function c (equation 197 and previous in
     * the yellowpaper). The returned layout contains the information stored in an {@link
     * AbridgedNode}. See the README of this package for more information.
     */
    public RLP compose() {
        switch (type) {
            case LEAF:
                return RLP.sequence(keySegment.hexPrefix(true), value);
            case EXTENSION:
                return RLP.sequence(keySegment.hexPrefix(false), wrappedCap(childrenCaps[0]));
            case BRANCH:
                var sequence = new Object[17];
                for (int i = 0; i < 16; i++) {
                    sequence[i] = childrenCaps[i] == null
                        ? EMPTY_BYTE_ARRAY
                        : wrappedCap(childrenCaps[i]);
                }
                sequence[16] = value == null ? EMPTY_BYTE_ARRAY : value;
                return RLP.sequence(sequence);
            default:
                throw new Error("unreachable");
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the result of the node cap function n (equation 194 in the yellowpaper), which is the
     * RLP encoding of the result of {@link #compose()} if its size is less than 32, or a Keccak
     * hash thereof otherwise.
     * <p>
     * Computed by {@link #computeCap(RLP)}.
     */
    public byte[] cap() {
        return cap;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the Merkle root of the Merkle tree rooted at this node. This implements the TRIE
     * function in the yellowpaper (equation 195).
     */
    public MerkleRoot merkleRoot() {
        // The same logic appears in PatriciaNode
        return cap.length == 32
            ? new MerkleRoot(cap)
            : new MerkleRoot(Hashing.keccak(cap));
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
     * (passed in to avoid repeat computation).
     * <p>
     * Returns null if no such child is reachable.
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
