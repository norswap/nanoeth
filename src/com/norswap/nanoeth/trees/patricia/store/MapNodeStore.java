package com.norswap.nanoeth.trees.patricia.store;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.trees.patricia.NodeStore;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaExtensionNode;
import com.norswap.nanoeth.trees.patricia.PatriciaLeafNode;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;
import com.norswap.nanoeth.utils.Pair;
import java.util.HashMap;

/**
 * Key-value store implementation that stores key-values in an in-memory map (dictionary).
 */
public final class MapNodeStore implements NodeStore {

    // ---------------------------------------------------------------------------------------------

    private final HashMap<byte[], PatriciaNode> store = new HashMap<>();

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaExtensionNode extensionNode
            (Nibbles keyFragment, PatriciaBranchNode child) {
        return addNode(new StorePatriciaExtensionNode(keyFragment, child.cap()));
    }

    // ---------------------------------------------------------------------------------------------

    @SafeVarargs
    @Override public final PatriciaBranchNode branchNode (Pair<Nibbles, PatriciaNode>... pairs) {
        // Almost identical to TreeNodeStore#branchNode
        var childrenCaps = new byte[16][];
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
            childrenCaps[pivot] = prepend(suffix, child).cap();
        }
        return addNode(new StorePatriciaBranchNode(value, childrenCaps));
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaBranchNode withValue (
            PatriciaBranchNode branch,
            @Nullable @Retained byte[] value) {
        assert branch instanceof StorePatriciaBranchNode;
        var sbranch = (StorePatriciaBranchNode) branch;
        removeNode(branch);
        return addNode(new StorePatriciaBranchNode(value, sbranch.childrenCaps));
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaBranchNode withChild (
            PatriciaBranchNode branch, int nibble, PatriciaNode child) {
        assert 0 <= nibble && nibble < 16;
        assert branch instanceof StorePatriciaBranchNode;
        var sbranch = (StorePatriciaBranchNode) branch;
        var newChildrenCaps = sbranch.childrenCaps.clone();
        newChildrenCaps[nibble] = child == null
            ? null
            : child.cap();
        removeNode(branch);
        return addNode(new StorePatriciaBranchNode(sbranch.value(), newChildrenCaps));
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable PatriciaNode getNode (byte[] cap) {
        return store.get(cap);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public <T extends PatriciaNode> T addNode (T node) {
        store.put(node.cap(), node);
        return node;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void removeNode (PatriciaNode node) {
        assert store.containsKey(node.cap());
        store.remove(node.cap());
    }

    // ---------------------------------------------------------------------------------------------
}
