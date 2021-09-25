package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.trees.patricia.NodeStore;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaExtensionNode;
import com.norswap.nanoeth.trees.patricia.PatriciaLeafNode;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;
import com.norswap.nanoeth.utils.Pair;

/**
 * Node store implementation for the in-memory tree.
 * <p>
 * The in-memory tree does not have "a store", since children are kept directly in the nodes, so
 * the data access method are no-op, excepted {@link #getNode(byte[])} which throws an exception.
 */
public final class TreeNodeStore implements NodeStore {

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaExtensionNode extensionNode (
            Nibbles keyFragment, PatriciaBranchNode child) {
        return new MemPatriciaExtensionNode(keyFragment, child);
    }

    // ---------------------------------------------------------------------------------------------

    @SafeVarargs
    @Override public final PatriciaBranchNode branchNode (Pair<Nibbles, PatriciaNode>... pairs) {
        // Almost identical to MapNodeStore#branchNode
        var children = new PatriciaNode[16];
        byte[] value = null;
        for (var pair: pairs) {
            var keySuffix = pair.fst;
            if (keySuffix.length() == 0) {
                // always a leaf node in this case
                value = ((PatriciaLeafNode) pair.snd).value;
                continue;
            }
            var child = pair.snd;
            var pivot  = keySuffix.get(0);
            var suffix = keySuffix.dropFirst(1);
            children[pivot] = prepend(suffix, child);
        }
        return new MemPatriciaBranchNode(value, children);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaBranchNode withValue (
            PatriciaBranchNode branch,
            @Nullable @Retained byte[] value) {

        assert branch instanceof MemPatriciaBranchNode;
        return new MemPatriciaBranchNode(value, ((MemPatriciaBranchNode) branch).children);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaBranchNode withChild (
            PatriciaBranchNode branch, int nibble, PatriciaNode child) {
        assert 0 <= nibble && nibble < 16;
        assert branch instanceof MemPatriciaBranchNode;
        var mbranch = (MemPatriciaBranchNode) branch;
        var newChildren = mbranch.children.clone();
        newChildren[nibble] = child;
        return new MemPatriciaBranchNode(mbranch.value(), newChildren);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode getNode (byte[] cap) {
        throw new UnsupportedOperationException();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public <T extends PatriciaNode> T addNode (T node) {
        return node;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void removeNode (PatriciaNode node) {}

    // ---------------------------------------------------------------------------------------------
}
