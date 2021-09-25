package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.utils.Pair;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static com.norswap.nanoeth.trees.patricia.AbridgedNode.Type.LEAF;

/**
 * A leaf in the patrica tree, which store the suffix of the key and its associated value.
 * <p>
 * Because a leaf node has no children, this implementation can be shared between multiple
 * implementations.
 */
public final class PatriciaLeafNode extends PatriciaNode {

    // ---------------------------------------------------------------------------------------------

    public final Nibbles keySuffix;

    public final byte[] value;

    public PatriciaLeafNode (Nibbles keySuffix, @Retained byte[] value) {
        this.keySuffix = keySuffix;
        this.value = value;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Step step (NodeStore store, Nibbles keySuffix) {
        int sharedPrefix = this.keySuffix.sharedPrefix(keySuffix);
        return new Step(this, null, sharedPrefix, keySuffix.length() - sharedPrefix);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (NodeStore store, Nibbles keySuffix) {
        return keySuffix.equals(this.keySuffix)
            ? value
            : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode add (NodeStore store, Nibbles keySuffix, byte[] value) {

        var ownSuffix = this.keySuffix;
        int prefixLen = ownSuffix.sharedPrefix(keySuffix);

        if (prefixLen == ownSuffix.length() && prefixLen == keySuffix.length())
            if (Arrays.equals(value, this.value))
                return this;
            else {
                store.removeNode(this);
                return store.leafNode(keySuffix, value);
            }

        store.removeNode(this);

        // split into a branch node with two children (or a child and a value)
        // NOTE: no need to add the leave nodes to the store, they'll be destructured in branchNode
        @SuppressWarnings("unchecked")
        var branch = store.branchNode(
            Pair.of(ownSuffix.dropFirst(prefixLen), store.leafNode(Nibbles.EMPTY, this.value)),
            Pair.of(keySuffix.dropFirst(prefixLen), store.leafNode(Nibbles.EMPTY, value)));

        // if the shared prefix isn't empty, wrap the branch node in an extension node
        return store.prepend(keySuffix.prefix(prefixLen), branch);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode remove (NodeStore store, Nibbles keySuffix) {
        if (this.keySuffix.equals(keySuffix)) {
            store.removeNode(this);
            return null;
        }
        return this; // not found
    }

    // ---------------------------------------------------------------------------------------------

    @Override public AbridgedNode abridged() {
        return new AbridgedNode(LEAF, keySuffix, value, null);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (NodeStore store,
            Nibbles prefix, Map<byte[], byte[]> map) {
        map.put(prefix.concat(keySuffix).bytes(), value);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof PatriciaLeafNode)) return false;
        var that = (PatriciaLeafNode) o;
        return keySuffix.equals(that.keySuffix) && Arrays.equals(value, that.value);
    }

    @Override public int hashCode () {
        return 31 * Objects.hash(keySuffix) + Arrays.hashCode(value);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return String.format("PatriciaLeafNode{ %s = %s }",
                keySuffix, ByteUtils.toFullHexString(value));
    }

    // ---------------------------------------------------------------------------------------------
}
