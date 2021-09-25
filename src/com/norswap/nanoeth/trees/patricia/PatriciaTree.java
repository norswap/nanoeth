package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.PatriciaNode.Step;
import com.norswap.nanoeth.utils.Hashing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Implementation of the Modified Merkle Patricia Tree, as per appendix D of the yellowpaper.
 * <p>
 * A patricia tree is associated with a {@link NodeStore key-value store} used to access nodes.
 * <p>
 * See trees.patricia package README for more information on patricia trees.
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

    /** The store associated to this tree). */
    public final NodeStore store;

    // ---------------------------------------------------------------------------------------------

    /** Creates an empty patricia tree (root = null). */
    public PatriciaTree (NodeStore store) {
        this(store, null);
    }

    // ---------------------------------------------------------------------------------------------

    /** Creates a patricia tree with the given root node. */
    public PatriciaTree (NodeStore store, @Nullable PatriciaNode root) {
        this.store = store;
        this.root = root;
    }

    // ---------------------------------------------------------------------------------------------

    /** Lookup the value associated with the given key, or null if no such value exists. */
    public @Nullable byte[] lookup (byte[] key) {
        return root != null
            ? root.lookup(store, new Nibbles(key, 0, key.length * 2))
            : null;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed tree, after associating the given value with the given key.
     * If the key is empty, returns itself.
     * <p>
     * The store might be modified as a result, refer to the documentation for the used store.
     */
    public PatriciaTree add (byte[] key, byte[] value) {
        if (key.length == 0) return this;
        return root != null
            ? new PatriciaTree(store, root.add(store, new Nibbles(key), value))
            : new PatriciaTree(store, store.leafNode(new Nibbles(key), value));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed tree, after removing the entry for the given key (if any).
     * <p>
     * The store might be modified as a result, refer to the documentation for the used store.
     */
    public PatriciaTree remove (byte[] key) {
        return root != null
            ? new PatriciaTree(store, root.remove(store, new Nibbles(key)))
            : this;
    }

    // ---------------------------------------------------------------------------------------------

    /** Collects all (key, value) entries in the tree in a map, and returns it. */
    public Map<byte[], byte[]> toMap() {
        if (root == null)
            return Collections.emptyMap();
        var map = new HashMap<byte[], byte[]>();
        root.collectEntries(store, new Nibbles(new byte[0]), map);
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

    /**
     * Calls {@code f} with every node on the branch for {@code keySuffix} (represented by a {@link
     * PatriciaNode#step step}, where this branch is the node path from this node towards the node
     * that holds the value associated to the key suffix, or to the deepest node that would have to
     * be modified in order to associated a value with the key suffix.
     * <p>
     * This will always at least call {@code f} with the root, if there is one (i.e. the tree is
     * not empty).
     */
    public final void forBranch (byte[] key, Consumer<Step> f) {
        if (root == null) return;
        var keySuffix = new Nibbles(key);
        var step = root.step(store, keySuffix);
        f.accept(step);
        while (step.child != null) {
            keySuffix = keySuffix.dropFirst(step.sharedPrefix);
            step = step.child.step(store, keySuffix);
            f.accept(step);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a Merkle proof for the given key, either proving its association to its value, or
     * the absence of value.
     */
    public MerkleProof prove (byte[] key) {
        if (root == null)
            return new MerkleProof(key, null, new AbridgedNode[0]);

        byte[][] value = new byte[1][]; // one more dimension to assign from lambda
        var nodes = new ArrayList<AbridgedNode>();

        forBranch(key, step -> {
            var abridged = step.node.abridged();
            nodes.add(abridged);
            if (step.nibblesLeft == 0)
                value[0] = abridged.value;
        });

        return new MerkleProof(key, value[0], nodes.toArray(AbridgedNode[]::new));
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
