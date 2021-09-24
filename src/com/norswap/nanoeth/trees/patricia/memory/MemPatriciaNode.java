package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;

/**
 * The common interface for all in-memory patricia tree node implementations.
 */
public abstract class MemPatriciaNode extends PatriciaNode {

    // ---------------------------------------------------------------------------------------------

    // narrow the return type
    @Override public abstract MemPatriciaNode add (Nibbles keySuffix, byte[] data);

    // narrow the return type
    @Override public abstract MemPatriciaNode remove (Nibbles keySuffix);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new node that corresponds to the node that the given nibbles prepended.
     * This results in a single node of the same type for leaf and extension nodes, and for
     * an extension node holding the original node for branch nodes.
     * <p>
     * This is used by branch and extension nodes to implement entry deletion.
     */
    MemPatriciaNode prepend (Nibbles nibbles, MemPatriciaNode node) {
        if (node instanceof MemPatriciaExtensionNode) {
            var extNode = (MemPatriciaExtensionNode) node;
            return new MemPatriciaExtensionNode(
                nibbles.concat(extNode.keyFragment), (MemPatriciaBranchNode) extNode.child);
        }
        if (node instanceof MemPatriciaLeafNode) {
            var leafNode = (MemPatriciaLeafNode) node;
            return new MemPatriciaLeafNode(
                nibbles.concat(leafNode.keySuffix), leafNode.data);
        }
        if  (node instanceof MemPatriciaBranchNode) {
            var branchNode = (MemPatriciaBranchNode) node;
            return new MemPatriciaExtensionNode(nibbles, branchNode);
        }
        throw new Error(); // unreachable
    }

    // ---------------------------------------------------------------------------------------------
}
