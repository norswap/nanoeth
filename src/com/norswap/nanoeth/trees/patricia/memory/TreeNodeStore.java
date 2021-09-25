package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.trees.patricia.KVStore;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaExtensionNode;
import com.norswap.nanoeth.trees.patricia.PatriciaLeafNode;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;
import com.norswap.nanoeth.utils.Pair;

/**
 * Key-value store implementation for the in-memory tree.
 * <p>
 * The in-memory tree does not have "a store", since children are kept directly in the nodes.
 * However, the {@link KVStore} interface also defines some node factory methods that we need to
 * implement.
 */
public final class TreeKVStore implements KVStore {

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaExtensionNode extensionNode (
            Nibbles keyFragment, PatriciaBranchNode child) {
        return new MemPatriciaExtensionNode(keyFragment, child);
    }

    // ---------------------------------------------------------------------------------------------

    @SafeVarargs
    @Override public final PatriciaBranchNode branchNode (Pair<Nibbles, PatriciaNode>... pairs) {
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

    @Override public PatriciaBranchNode withValue (PatriciaBranchNode branch, byte[] value) {
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
}
