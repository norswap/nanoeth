package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.AbridgedNode;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static com.norswap.nanoeth.trees.patricia.PatriciaNode.Type.LEAF;

/**
 * A leaf in the in-memory patrica tree, which store the suffix of the key and its associated value.
 */
public final class MemPatriciaLeafNode extends MemPatriciaNode {

    // ---------------------------------------------------------------------------------------------

    /**
     * The suffix of the key leading to this node from the root of the Merkle tree, i.e. the part of
     * the key that wasn't used to reach this leaf node when traversing the Merkle tree from the
     * root.
     */
    public final Nibbles keySuffix;

    // ---------------------------------------------------------------------------------------------

    /** The value held in the leaf. */
    public final byte[] value;

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaLeafNode (Nibbles keySuffix, @Retained byte[] value) {
        this.keySuffix = keySuffix;
        this.value = value;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Type type () {
        return LEAF;
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

    @Override public MemPatriciaNode add (Nibbles keySuffix, byte[] value) {
        var ownSuffix = this.keySuffix;
        int prefixLen = ownSuffix.sharedPrefix(keySuffix);

        if (prefixLen == ownSuffix.length() && prefixLen == keySuffix.length())
            // same suffix, overwrite value
            return new MemPatriciaLeafNode(keySuffix, value);

        var branch = new MemPatriciaBranchNode(new MemPatriciaNode[16], null, true);
        branch = branch.insert(ownSuffix, this.value, prefixLen);
        branch = branch.insert(keySuffix, value,      prefixLen);

        // if the shared prefix isn't empty, wrap the branch node in an extension node
        return prefixLen == 0
            ? branch
            : new MemPatriciaExtensionNode(keySuffix.prefix(prefixLen), branch);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public MemPatriciaNode remove (Nibbles keySuffix) {
        return this.keySuffix.equals(keySuffix)
            ? null // delete this leaf
            : this; // not found
    }

    // ---------------------------------------------------------------------------------------------

    @Override public RLP compose() {
        return RLP.sequence(keySuffix.hexPrefix(true), value);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public AbridgedNode abridged() {
        return new AbridgedNode(LEAF, keySuffix, value, null, cap());
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (Nibbles prefix, Map<byte[], byte[]> map) {
        map.put(prefix.concat(keySuffix).bytes(), value);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof MemPatriciaLeafNode)) return false;
        var that = (MemPatriciaLeafNode) o;
        return keySuffix.equals(that.keySuffix) && Arrays.equals(value, that.value);
    }

    @Override public int hashCode () {
        return 31 * Objects.hash(keySuffix) + Arrays.hashCode(value);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return String.format("MemPatriciaLeafNode{ %s = %s }",
            keySuffix, ByteUtils.toFullHexString(value));
    }

    // ---------------------------------------------------------------------------------------------
}
