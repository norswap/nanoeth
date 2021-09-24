package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.data.MerkleRoot;

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
    public final AbridgedNode[] branch;

    // ---------------------------------------------------------------------------------------------

    public MerkleProof (byte[] key, byte[] value, AbridgedNode[] branch) {
        this.key = key;
        this.value = value;
        this.branch = branch;
    }

    // ---------------------------------------------------------------------------------------------

    public boolean verify (MerkleRoot root) {
        if (branch.length == 0)
            // absence of value is trivially correct in an empty tree
            return value == null && root.equals(EMPTY_TREE_ROOT);

        if (!branch[0].merkleRoot().equals(root))
            return false; // mismatched root

        // verify that every node is consistent with its parent
        var keySuffix = new Nibbles(key);
        var prevNode = branch[0];
        for (int i = 1; i < branch.length; i++) {
            var prefix = prevNode.consumablePrefix(keySuffix);
            if (!Arrays.equals(prevNode.capForSuffix(prefix, keySuffix), branch[i].cap()))
                return false;
            keySuffix = keySuffix.dropFirst(prefix);
            prevNode = branch[i];
        }

        var prefix = prevNode.consumablePrefix(keySuffix);
        var childCap = prevNode.capForSuffix(prefix, keySuffix);
        keySuffix = keySuffix.dropFirst(prefix);

        if (childCap != null)
            // the branch is not complete
            return false;
        else if (value != null)
            // key must be fully used and the value must match
            return keySuffix.length() == 0 && Arrays.equals(prevNode.value, value);
        else
            // the branch must not prove a value
            return !(keySuffix.length() == 0 && prevNode.value != null);
    }

    // ---------------------------------------------------------------------------------------------
}
