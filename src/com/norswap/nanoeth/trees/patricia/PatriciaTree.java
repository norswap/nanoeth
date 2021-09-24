package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.PatriciaNode.Step;
import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaLeafNode;
import com.norswap.nanoeth.utils.Hashing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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

    /** Lookup the value associated with the given key. */
    public byte[] lookup (byte[] key) {
        return root != null
            ? root.lookup(new Nibbles(key, 0, key.length * 2))
            : null;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed tree, after associating the given value with the given key.
     * If the key is empty, returns itself.
     */
    public PatriciaTree add (byte[] key, byte[] value) {
        if (key.length == 0) return this;
        return root != null
            ? new PatriciaTree(root.add(new Nibbles(key), value))
            : new PatriciaTree(createLeafNode(new Nibbles(key), value));
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

    protected PatriciaNode createLeafNode (Nibbles key, byte[] value) {
        return new MemPatriciaLeafNode(key, value);
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
        var step = root.step(keySuffix);
        f.accept(step);
        while (step.child != null) {
            keySuffix = keySuffix.dropFirst(step.sharedPrefix);
            step = step.child.step(keySuffix);
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
