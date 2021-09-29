package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.trees.patricia.memory.TreeNodeStore;
import com.norswap.nanoeth.trees.patricia.store.MapNodeStore;
import com.norswap.nanoeth.utils.Pair;

/**
 * Interface for a node store underlying a modified Merkle patricia tree.
 * <p>
 * This interface defines two types of method. First, factory methods to create implementations
 * of {@link PatriciaNode} satisfying certain constraints, and which are able to work with the
 * implemented store.
 * <p>
 * Second, data access methods to add, remove and retrieve nodes from the store.
 * <p>
 * It is expected a node store will only interact with nodes it is compatible with. Whenever a
 * method in this interface receives a node parameter, it is free to assume it is from an
 * implementation it supports.
 * <p>
 * Currently, this is implemented by {@link MapNodeStore}, which uses a in-memory hash map as store,
 * and {@link TreeNodeStore} which represents a tree without store (i.e. each node has direct
 * references to its children), so the data access method are no-ops or throw exceptions (the reason
 * why there is a store at all is that we wish to share logic between tree and store
 * implementations, and implementing a store for store-less trees makes this much easier.
 */
public interface NodeStore {

    // =============================================================================================
    // region Factory Methods
    // =============================================================================================

    /**
     * Creates a new leaf node with the given key suffix and value.
     * <p>
     * The returned value must be added to the store.
     */
    default PatriciaLeafNode leafNode (Nibbles keySuffix, byte[] value) {
        return addNode(new PatriciaLeafNode(keySuffix, value));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new extension node with the given key fragment and child node.
     * <p>
     * The returned value must be added to the store, nothing should be done with the child.
     */
    PatriciaExtensionNode extensionNode (Nibbles keyFragment, PatriciaBranchNode child);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new branch node with the give children.
     * <p>
     * If the nibbles part of a pair is empty, the child must be a leaf node, and its value will be
     * used as the value of the branch node. Otherwise the node will be inserted into the new branch
     * node, potentially after being extended as an extension node or longer leaf node to accomodate
     * the remaining nibbles past the first (which is "consumed" by the branch node).
     * <p>
     * The returned value must be added to the store, nothing should be done with the children.
     */
    @SuppressWarnings("unchecked")
    PatriciaBranchNode branchNode (Pair<Nibbles, PatriciaNode>... pairs);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a copy of {@code branch} with its value set to {@code value} (possibly null).
     * <p>
     * The returned value must be added to the store, while {@code branch} must be removed from
     * the store if not null.
     */
    PatriciaBranchNode withValue (PatriciaBranchNode branch, @Nullable @Retained byte[] value);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a copy of {@code branch} with its child for the given {@code nibble} set to {@code
     * child}.
     * <p>
     * {@code child} is allowed to be null, but it is the responsability of the caller to ensure
     * that the returned node is valid (i.e. has at a least a child and a value or two children).
     * <p>
     * The returned value must be added to the store, while {@code branch} must be removed from the
     * store. Nothing should be done with the child.
     */
    PatriciaBranchNode withChild
            (PatriciaBranchNode branch, int nibble, @Nullable PatriciaNode child);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new node that corresponds to the node that the given nibbles prepended.
     * This results in a single node of the same type for leaf and extension nodes, and for
     * an extension node holding the original node for branch nodes.
     * <p>
     * The returned value must be added to the store, while {@code node} must be removed from the
     * store if it does not become a child of the result.
     */
    default PatriciaNode prepend (Nibbles nibbles, PatriciaNode node) {
        PatriciaNode out;
        if (nibbles.length() == 0)
            return node;
        if (node instanceof PatriciaExtensionNode) {
            removeNode(node);
            var ext = (PatriciaExtensionNode) node;
            return extensionNode(nibbles.concat(ext.keyFragment()), ext.child(this));
        }
        else if (node instanceof PatriciaLeafNode) {
            removeNode(node);
            var leaf = (PatriciaLeafNode) node;
            return leafNode(nibbles.concat(leaf.keySuffix), leaf.value);
        }
        else /* if  (node instanceof PatriciaBranchNode) */ {
            var branch = (PatriciaBranchNode) node;
            return extensionNode(nibbles, branch);
        }
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

    // ---------------------------------------------------------------------------------------------

    /**
     * Removes the node from the store.
     * <p>
     * The behaviour is implementation-defined, and in particular, this does <b>not</b> guarantee
     * that after calling this method {@code getNode(node.cap())} will return null.
     * <p>
     * The major example is that for the chain state, reorgs are possible, so we want to keep old
     * nodes around. However, we might want to record which block removed which nodes, so that
     * we may eventually prune the tree.
     */
    void removeNode (PatriciaNode node);

    // endregion
    // =============================================================================================
}
