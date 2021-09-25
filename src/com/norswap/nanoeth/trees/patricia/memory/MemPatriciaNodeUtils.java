package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaLeafNode;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;

/** Utility methods used in in-memory tree implementation. */
final class MemPatriciaNodeUtils {
    private MemPatriciaNodeUtils() {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new node that corresponds to the node that the given nibbles prepended.
     * This results in a single node of the same type for leaf and extension nodes, and for
     * an extension node holding the original node for branch nodes.
     * <p>
     * This is used by branch and extension nodes to implement entry deletion.
     */
    static PatriciaNode prepend (Nibbles nibbles, PatriciaNode node) {
        if (node instanceof MemPatriciaExtensionNode) {
            var extNode = (MemPatriciaExtensionNode) node;
            return new MemPatriciaExtensionNode(
                nibbles.concat(extNode.keyFragment), extNode.child);
        }
        if (node instanceof PatriciaLeafNode) {
            var leafNode = (PatriciaLeafNode) node;
            return new PatriciaLeafNode(
                nibbles.concat(leafNode.keySuffix), leafNode.value);
        }
        if  (node instanceof MemPatriciaBranchNode) {
            var branchNode = (MemPatriciaBranchNode) node;
            return new MemPatriciaExtensionNode(nibbles, branchNode);
        }
        throw new Error(); // unreachable
    }

    // ---------------------------------------------------------------------------------------------
}
