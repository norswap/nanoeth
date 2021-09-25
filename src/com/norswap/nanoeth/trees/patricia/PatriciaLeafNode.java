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

    @Override public Step step (Nibbles keySuffix) {
        int sharedPrefix = this.keySuffix.sharedPrefix(keySuffix);
        return new Step(this, null, sharedPrefix, keySuffix.length() - sharedPrefix);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (Nibbles keySuffix) {
        return keySuffix.equals(this.keySuffix)
                ? value
                : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode add (KVStore store, Nibbles keySuffix, byte[] value) {

        var ownSuffix = this.keySuffix;
        int prefixLen = ownSuffix.sharedPrefix(keySuffix);

        if (prefixLen == ownSuffix.length() && prefixLen == keySuffix.length())
            // same suffix, overwrite value
            return new PatriciaLeafNode(keySuffix, value);

        // split into a branch node with two children (or a child and a value)
        @SuppressWarnings("unchecked")
        var branch = store.branchNode(
            Pair.of(ownSuffix.dropFirst(prefixLen), new PatriciaLeafNode(Nibbles.EMPTY, this.value)),
            Pair.of(keySuffix.dropFirst(prefixLen), new PatriciaLeafNode(Nibbles.EMPTY, value)));

        // if the shared prefix isn't empty, wrap the branch node in an extension node
        return prefixLen == 0
                ? branch
                : store.extensionNode(keySuffix.prefix(prefixLen), branch);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode remove (KVStore store,
            Nibbles keySuffix) {
        return this.keySuffix.equals(keySuffix)
                ? null // delete this leaf
                : this; // not found
    }

    // ---------------------------------------------------------------------------------------------

    @Override public AbridgedNode abridged() {
        return new AbridgedNode(LEAF, keySuffix, value, null);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (Nibbles prefix, Map<byte[], byte[]> map) {
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
