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

            if (suffix.length() == 0) {
                children[pivot] = child;
            } else if (child instanceof PatriciaLeafNode) {
                var leaf = (PatriciaLeafNode) child;
                children[pivot] = new PatriciaLeafNode(suffix.concat(leaf.keySuffix), leaf.value);
            } else if (child instanceof PatriciaExtensionNode) {
                var ext = (PatriciaExtensionNode) child;
                children[pivot] = extensionNode(suffix.concat(ext.keyFragment()), ext.child());
            } else {
                var branch = (PatriciaBranchNode) child;
                children[pivot] = extensionNode(suffix, branch);
            }
        }
        return new MemPatriciaBranchNode(children, value);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaExtensionNode extensionNode (
                Nibbles keyFragment, PatriciaBranchNode child) {
        return new MemPatriciaExtensionNode(keyFragment, child);
    }

    // ---------------------------------------------------------------------------------------------
}
