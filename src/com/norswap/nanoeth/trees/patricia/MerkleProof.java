package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.trees.patricia.store.MapNodeStore;
import com.norswap.nanoeth.trees.patricia.store.MissingNode;

import java.util.Arrays;

import static com.norswap.nanoeth.trees.patricia.PatriciaTree.EMPTY_TREE_ROOT;

/**
 * A proof that a key-value pair belongs to a tree. Note that the tree is question is not stored
 * in this object.
 */
public final class MerkleProof {
    // ---------------------------------------------------------------------------------------------

    /** Key being proven. */
    public final byte[] key;

    // ---------------------------------------------------------------------------------------------

    /** Value whose association with the key is being proven, null if proving absence of value. */
    public final byte[] value;

    // ---------------------------------------------------------------------------------------------

    /**
     * Merkle tree nodes (in abridged form) on the branch from the root to the node (leaf or branch)
     * that contains the value associated with the key, <b>or</b> to the deepest node that would
     * have to be modified in order to insert a value for key (if {@link #value} is null).
     */
    public final PatriciaNode[] branch;

    // ---------------------------------------------------------------------------------------------

    public MerkleProof (byte[] key, byte[] value, PatriciaNode[] branch) {
        this.key = key;
        this.value = value;
        this.branch = branch;
    }

    // ---------------------------------------------------------------------------------------------

    public boolean verify (MerkleRoot root) {
        if (branch.length == 0)
            // absence of value is trivially correct in an empty tree
            return value == null && root.equals(EMPTY_TREE_ROOT);

        var store = new MapNodeStore();
        var tree = new PatriciaTree(store, branch[0]);
        for (var node: branch) store.addNode(node);

        // using forBranch guarantees that the proof contains a valid branch
        var finalStep = new BranchStep[1];
        try {
            tree.forBranch(key, step -> {
                if (step.child != null) return;
                finalStep[0] = step;
            });
        } catch (MissingNode e) {
            // The branch is invalid: a node points to a next branch node that is not in the
            // supplied branch!
            return false;
        }

        var nibblesLeft = finalStep[0].nibblesLeft;
        var foundValue = finalStep[0].node.value();

        if (value != null)
            // key must be fully used and the value must match
            return nibblesLeft == 0 && Arrays.equals(value, foundValue);
        else
            // the branch must not prove a value
            return !(nibblesLeft == 0 && foundValue != null);
    }

    // ---------------------------------------------------------------------------------------------
}
