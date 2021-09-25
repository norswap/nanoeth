package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.utils.Pair;

/**
 * Interface for key-value stores underlying a modified Merkle patricia tree.
 * TODO type assumptions
 */
public interface KVStore {

    // =============================================================================================
    // region Factory Methods
    // =============================================================================================

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

    /**
     * Returns a copy of {@code branch} with its value set to {@code value}.
     */
    PatriciaBranchNode withValue (PatriciaBranchNode branch, byte[] value);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a copy of {@code branch} with its child for the given {@code nibble} set to {@code
     * child}. The child is added to the store.
     */
    PatriciaBranchNode withChild (PatriciaBranchNode branch, int nibble, PatriciaNode child);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new node that corresponds to the node that the given nibbles prepended.
     * This results in a single node of the same type for leaf and extension nodes, and for
     * an extension node holding the original node for branch nodes.
     */
    default PatriciaNode prepend (Nibbles nibbles, PatriciaNode node) {
        if (nibbles.length() == 0)
            return node;
        if (node instanceof PatriciaExtensionNode) {
            var ext = (PatriciaExtensionNode) node;
            return extensionNode(nibbles.concat(ext.keyFragment()), ext.child());
        }
        if (node instanceof PatriciaLeafNode) {
            var leaf = (PatriciaLeafNode) node;
            return new PatriciaLeafNode(nibbles.concat(leaf.keySuffix), leaf.value);
        }
        if  (node instanceof PatriciaBranchNode) {
            var branch = (PatriciaBranchNode) node;
            return extensionNode(nibbles, branch);
        }
        throw new Error(); // unreachable
    }

    // endregion
    // =============================================================================================
    // region Data Access
    // =============================================================================================

    /** Return a node from its cap value, or null if no such node exists. */
    @Nullable PatriciaNode getNode (byte[] cap);

    // ---------------------------------------------------------------------------------------------

    /** Adds a node to the store the returns it. */
    <T extends PatriciaNode> T addNode (T node);

    // endregion
    // =============================================================================================
}
