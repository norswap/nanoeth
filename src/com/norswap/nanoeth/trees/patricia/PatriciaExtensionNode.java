package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaExtensionNode;
import com.norswap.nanoeth.trees.patricia.store.StorePatriciaExtensionNode;
import com.norswap.nanoeth.utils.Pair;

import java.util.Map;
import java.util.Objects;

import static com.norswap.nanoeth.trees.patricia.AbridgedNode.Type.EXTENSION;

/**
 * A an extension node in the patricia tree represents a shared sequence of nibbles between
 * multiples keys (a "key fragment"). This is a (yellowpaper-mandated) compression method that
 * avoids having a chain of single-child branch nodes. It can also happen that the key fragment is
 * only a single nibble-long, since we use extension nodes when there is only one child (as it is
 * more space-efficient than using a branch node — again, is is yellowpaper-mandated).
 * <p>
 * The node holds the key fragment and a child node, which is normally always a branch node.
 */
public abstract class PatriciaExtensionNode extends PatriciaNode {
    // ---------------------------------------------------------------------------------------------

    /** Returns the key fragment associated with this node. */
    public abstract Nibbles keyFragment();

    // ---------------------------------------------------------------------------------------------

    /** Returns the child node of this extension node. This should always be a branch node. */
    public abstract PatriciaBranchNode child (NodeStore store);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the cap value of {@link #child(NodeStore)}.
     * <p>
     * This is a separate method because in the abridged representation, the child's cap value
     * is stored directly, but not the child itself.
     */
    public abstract byte[] childCap();

    // ---------------------------------------------------------------------------------------------

    @Override public Step step (NodeStore store, Nibbles keySuffix) {
        int len = keyFragment().length();
        int sharedPrefix = keyFragment().sharedPrefix(keySuffix);
        var node = sharedPrefix == len ? child(store) : null;
        return new Step(this, node, sharedPrefix, keySuffix.length() - sharedPrefix);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (NodeStore store, Nibbles keySuffix) {
        int len = keyFragment().length();
        return keyFragment().sharedPrefix(keySuffix) == len
            ? child(store).lookup(store, keySuffix.dropFirst(len))
            : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode add (NodeStore store, Nibbles keySuffix, byte[] value) {

        int prefixLen = keyFragment().sharedPrefix(keySuffix);

        // the whole key fragment is shared, merge child
        if (prefixLen == keyFragment().length())
            return store.extensionNode(
                keyFragment(),
                child(store).add(store, keySuffix.dropFirst(prefixLen), value));

        @SuppressWarnings("unchecked")
        var branch = store.branchNode(
            Pair.of(keyFragment().dropFirst(prefixLen), child(store)),
            Pair.of(keySuffix.dropFirst(prefixLen), store.leafNode(Nibbles.EMPTY, value)));

        // if the shared prefix isn't empty, wrap the branch node in an extension node
        return store.prepend(keySuffix.prefix(prefixLen), branch);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode remove (NodeStore store, Nibbles keySuffix) {

        int prefixLen = keyFragment().sharedPrefix(keySuffix);
        if (prefixLen < keyFragment().length())
            return this; // not found

        var child = child(store);
        var newChild = child.remove(store, keySuffix.dropFirst(prefixLen));

        if (newChild == child)
            return this; // no change

        store.removeNode(this);
        return newChild == null
            ? null
            : store.prepend(keyFragment(), newChild);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public AbridgedNode abridged() {
        return new AbridgedNode(EXTENSION, keyFragment(), null, new byte[][]{ childCap() });
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (NodeStore store, Nibbles prefix, Map<byte[], byte[]> map) {
        child(store).collectEntries(store, prefix.concat(keyFragment()), map);
    }

    // ---------------------------------------------------------------------------------------------
}
