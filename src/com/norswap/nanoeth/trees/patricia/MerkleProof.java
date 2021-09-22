package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaDigestNode;
import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaExtensionNode;
import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaLeafNode;
import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaNode;
import com.norswap.nanoeth.utils.Assert;

/**
 * A proof that a key-value pair belongs to a tree. Note that the tree is question is not stored
 * in this object.
 * <p>
 * Build this object using {@link PatriciaTree#prove(byte[])} method.
 */
public final class MerkleProof {
    // ---------------------------------------------------------------------------------------------

    public final byte[] key;
    public final byte[] value;

    // ---------------------------------------------------------------------------------------------

    /**
     * The sizes in nibbles of the key fragments for all nodes on the Merkle tree branch from the
     * leaf (or final branch node) node containing the value to the root (all inclusive, and in that
     * order). For branch nodes, this is always 1.
     */
    public final byte[] sizes;

    /**
     * The "digests" for the "children" of all nodes on the Merkle tree branch from the leaf node
     * (or final branch node) containing the value to the root (all inclusive, and in that order).
     * This will be null for the leaf node and extension nodes. For branch nodes, this is a {@code
     * byte[][]} of size 17, where index 16 holds the value for that node (or {@code null} if the
     * node has no value). Other indices have value {@code null} if there is no child at that
     * nibble, {@code new byte[0]} if this is where the child in the branch must be inserted, and
     * the digest (result of the {@link MemPatriciaNode#cap()} method) for the child otherwise.
     */
    public final byte[][][] digests;

    // ---------------------------------------------------------------------------------------------

    MerkleProof (
            @Retained byte[] key,
            @Retained byte[] value,
            @Retained byte[] sizes,
            @Retained byte[][][] digests) {
        Assert.that(sizes.length == digests.length, "sizes and digests should have the same length");
        this.key = key;
        this.value = value;
        this.sizes = sizes;
        this.digests = digests;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new branch node from the {@code digests} array (which also includes the node data)
     * and insert {@code child} at the position marked by an empty byte array in {@code digests}.
     * <p>
     * If the branch node holds the proven value, {@code child} will be null and no insertion marker
     * (empty byte array) will be present.
     */
    private MemPatriciaBranchNode createBranchNode (byte[][] digests, MemPatriciaNode child) {
        assert digests.length == 17;
        var children = new MemPatriciaNode[16];
        byte[] data = digests[16];
        for (int j = 0; j < 16; j++) {
            if (digests[j] == null) continue;
            assert digests[j].length > 0 || child != null;
            children[j] = digests[j].length == 0
                ? child
                : new MemPatriciaDigestNode(digests[j]);
        }
        return new MemPatriciaBranchNode(children, data);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Checks that the current proof is valid for the tree with the given Merkle root.
     */
    public boolean check (MerkleRoot root) {

        // We walk the "nodes" (sizes & digests) in the branch from the leaf to the root.
        // While doing so, we build up an in-memory tree around the branch, using
        // MemPatriciaDigestNode for the siblings in "branch nodes" (! not nodes on the branch, but
        // "forking" nodes).
        // This enables us to reuse the existing logic in the in-memory tree implementation to
        // re-compute the root of the tree to which the branch belongs.

        MemPatriciaNode node = null;
        var nibbles = new Nibbles(key);
        assert sizes.length > 0;

        for (int i = 0; i < sizes.length; i++) {
            if (digests[i] != null)
                node = createBranchNode(digests[i], node);
            else if (i == 0)
                node = new MemPatriciaLeafNode(nibbles.suffix(sizes[0]), value);
            else {
                assert node instanceof MemPatriciaBranchNode;
                var branch = (MemPatriciaBranchNode) node;
                node = new MemPatriciaExtensionNode(nibbles.suffix(sizes[i]), branch);
            }
            node.cap(); // optional - optimize data locality - should benchmark
            nibbles = nibbles.dropLast(sizes[i]);
        }

        return root.equals(node.merkleRoot());
    }

    // ---------------------------------------------------------------------------------------------
}
