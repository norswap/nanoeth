package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaLeafNode;
import com.norswap.nanoeth.utils.Hashing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Naive implementation of the Modified Merkle Patricia Tree, as per appendix D of the yellowpaper.
 * <p>
 * See trees.patricia package README for more information on patricia trees.
 * <p>
 * This uses the in-memory implementation (from the {@code memory} sub-package) by default. However
 * the implementation can be changed by sub-classing. In particular, to use a more efficient
 * implementation it is only necessary to implement the {@link PatriciaNode} interface, then
 * subclass the present class, overriding the {@link #createLeafNode(Nibbles, byte[])} method to
 * return an instance of the new node implementation.
 */
@Wrapper
public class PatriciaTree {

    // ---------------------------------------------------------------------------------------------

    /**
     * Merkle root of an empty tree, which the Keccak hash of an empty byte sequence.
     * <p>
     * Namely, this byte sequence is {@code
     * "0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"}
     */
    public final static MerkleRoot EMPTY_TREE_ROOT
        = new MerkleRoot(Hashing.keccak(RLP.bytes(new byte[0]).encode()));

    // ---------------------------------------------------------------------------------------------

    /** The root of the tree. */
    public final @Nullable PatriciaNode root;

    // ---------------------------------------------------------------------------------------------

    /** Creates an empty patricia tree (root = null). */
    public PatriciaTree () {
        this(null);
    }

    // ---------------------------------------------------------------------------------------------

    /** Creates a patricia tree with the given root node. */
    public PatriciaTree (@Nullable PatriciaNode root) {
        this.root = root;
    }

    // ---------------------------------------------------------------------------------------------

    /** Lookup the data associated with the given key. */
    public byte[] lookup (byte[] key) {
        return root != null
            ? root.lookup(new Nibbles(key, 0, key.length * 2))
            : null;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed tree, after associating the given data with the given key.
     * If the key is empty, returns itself.
     */
    public PatriciaTree add (byte[] key, byte[] data) {
        if (key.length == 0) return this;
        return root != null
            ? new PatriciaTree(root.add(new Nibbles(key), data))
            : new PatriciaTree(createLeafNode(new Nibbles(key), data));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed tree, after removing the entry for the given key (if any).
     */
    public PatriciaTree remove (byte[] key) {
        return root != null
            ? new PatriciaTree(root.remove(new Nibbles(key)))
            : this;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a Merkle proof for the given key, or null if there is no entry for the given key
     * in the tree.
     */
    public MerkleProof prove (byte[] key) {
        if (root == null) return null;
        var builder = new MerkleProofBuilder(key);
        root.buildProof(new Nibbles(key), builder);
        return builder.build();
    }

    // ---------------------------------------------------------------------------------------------

    protected PatriciaNode createLeafNode(Nibbles key, byte[] data) {
        return new MemPatriciaLeafNode(key, data);
    }

    // ---------------------------------------------------------------------------------------------

    /** Collects all (key, value) entries in the tree in a map, and returns it. */
    public Map<byte[], byte[]> toMap() {
        if (root == null)
            return Collections.emptyMap();
        var map = new HashMap<byte[], byte[]>();
        root.collectEntries(new Nibbles(new byte[0]), map);
        return map;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the Merkle root of the tree, i.e. the TRIE function in the yellowpaper (equation 195).
     */
    public MerkleRoot merkleRoot() {
        return root == null
            ? EMPTY_TREE_ROOT
            : root.merkleRoot();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof PatriciaTree && Objects.equals(root, ((PatriciaTree) o).root);
    }

    @Override public int hashCode() {
        return Objects.hash(root);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return root == null
            ? "(empty patricia tree)"
            : root.toString();
    }

    // ---------------------------------------------------------------------------------------------
}
