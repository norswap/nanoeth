package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.utils.Pair;

/**
 * Interface for key-value stores underlying a modified Merkle patricia tree.
 */
public interface KVStore {

    // ---------------------------------------------------------------------------------------------

    /** Returns a new extension node with the given key fragment and child node. */
    PatriciaExtensionNode extensionNode (Nibbles keyFragment, PatriciaBranchNode child);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new branch node with the give children.
     * <p>
     * If the nibbles part of a pair is empty, the child must be a leaf node, and its value will be
     * used as the value of the branch node. Otherwise the node will be inserted into the new branch
     * node, potentially after being extended as an extension node or longer leaf node to accomodate
     * the remaining nibbles past the first (which is "consumed" by the branch node).
     */
    @SuppressWarnings("unchecked")
    PatriciaBranchNode branchNode (Pair<Nibbles, PatriciaNode>... pairs);

    // ---------------------------------------------------------------------------------------------
}
