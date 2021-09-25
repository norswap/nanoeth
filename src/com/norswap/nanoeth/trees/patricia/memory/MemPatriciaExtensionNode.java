package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.trees.patricia.KVStore;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaExtensionNode;
import com.norswap.nanoeth.trees.patricia.PatriciaLeafNode;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;
import com.norswap.nanoeth.utils.Pair;
import java.util.Map;
import java.util.Objects;

import static com.norswap.nanoeth.trees.patricia.memory.MemPatriciaNodeUtils.prepend;

/**
 * An extension node in the in-memory patricia tree.
 */
public final class MemPatriciaExtensionNode extends PatriciaExtensionNode {

    // ---------------------------------------------------------------------------------------------

    public final Nibbles keyFragment;

    public final PatriciaBranchNode child;

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaExtensionNode (Nibbles keyFragment, PatriciaBranchNode child) {
        assert keyFragment.length() > 0 : "building extension node with empty key fragment";
        this.keyFragment = keyFragment;
        this.child = child;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Step step (Nibbles keySuffix) {
        int len = keyFragment.length();
        int sharedPrefix = keyFragment.sharedPrefix(keySuffix);
        var node = sharedPrefix == len ? child : null;
        return new Step(this, node, sharedPrefix, keySuffix.length() - sharedPrefix);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (Nibbles keySuffix) {
        int len = keyFragment.length();
        return keyFragment.sharedPrefix(keySuffix) == len
            ? child.lookup(keySuffix.dropFirst(len))
            : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode add (KVStore store, Nibbles keySuffix, byte[] value) {

        if (keySuffix.length() == 0) return null;
        int prefixLen = keyFragment.sharedPrefix(keySuffix);

        // the whole key fragment is shared, merge child
        if (prefixLen == keyFragment.length())
            return new MemPatriciaExtensionNode(
                keyFragment, child.add(store, keySuffix.dropFirst(prefixLen), value));

        @SuppressWarnings("unchecked")
        var branch = store.branchNode(
            Pair.of(keyFragment.dropFirst(prefixLen), child),
            Pair.of(keySuffix.dropFirst(prefixLen), new PatriciaLeafNode(Nibbles.EMPTY, value))
        );

        // if the shared prefix isn't empty, wrap the branch node in an extension node
        return prefixLen == 0
            ? branch
            : new MemPatriciaExtensionNode(keySuffix.prefix(prefixLen), branch);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode remove (KVStore store,
            Nibbles keySuffix) {
        int prefixLen = keyFragment.sharedPrefix(keySuffix);
        if (prefixLen < keyFragment.length())
            return this; // not found

        var newChild = child.remove(store, keySuffix.dropFirst(prefixLen));

        if (newChild == child)
            return this; // no change
        if (newChild == null)
            return null; // if the child disappears so does the extension node
        else
            return prepend(keyFragment, newChild);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Nibbles keyFragment() {
        return keyFragment;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaBranchNode child() {
        return child;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] childCap() {
        return child.cap();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (Nibbles prefix, Map<byte[], byte[]> map) {
        child.collectEntries(prefix.concat(keyFragment), map);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof MemPatriciaExtensionNode)) return false;
        var that = (MemPatriciaExtensionNode) o;
        return keyFragment.equals(that.keyFragment) && child.equals(that.child);
    }

    @Override public int hashCode () {
        return Objects.hash(keyFragment, child);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return String.format("MemPatriciaExtensionNode{ %s = %s }", keyFragment, child);
    }

    // ---------------------------------------------------------------------------------------------
}
