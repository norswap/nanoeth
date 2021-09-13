package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * A leaf in the in-memory patrica tree, which store the suffix of the key and its associated data.
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

    /** The data held in the leaf. */
    public final byte[] data;

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaLeafNode (Nibbles keySuffix, @Retained byte[] data) {
        this.keySuffix = keySuffix;
        this.data = data;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (Nibbles keySuffix) {
        return keySuffix.equals(this.keySuffix)
            ? data
            : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public MemPatriciaNode add (Nibbles keySuffix, byte[] data) {
        var ownSuffix = this.keySuffix;
        int prefixLen = ownSuffix.sharedPrefix(keySuffix);

        if (prefixLen == ownSuffix.length() && prefixLen == keySuffix.length())
            // same suffix, overwrite value
            return new MemPatriciaLeafNode(keySuffix, data);

        var branch = new MemPatriciaBranchNode(new MemPatriciaNode[16], null, true);
        branch = branch.insert(ownSuffix, this.data, prefixLen);
        branch = branch.insert(keySuffix, data,      prefixLen);

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

    @Override public RLP encode (SizeContext ctx) {
        // lower bound on RLP length (hex_prefix, data)
        // (note that single bytes can be encoded without adding a size byte)
        int dataLength   = data.length + (data.length > 1 ? 1 : 0);
        int suffixLength = keySuffix.length() / 2 + 1;  // +1 for hex-prefix flags
        if (suffixLength > 1) ++ suffixLength;          // +1 for suffix size
        int rlpLength = dataLength + suffixLength + 1;  // +1 for pair size

        var rlp = RLP.sequence(keySuffix.hexPrefix(true), data);
        return cap(rlp, rlpLength, ctx);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (Nibbles prefix, Map<byte[], byte[]> map) {
        map.put(prefix.concat(keySuffix).bytes(), data);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof MemPatriciaLeafNode)) return false;
        var that = (MemPatriciaLeafNode) o;
        return keySuffix.equals(that.keySuffix) && Arrays.equals(data, that.data);
    }

    @Override public int hashCode () {
        return 31 * Objects.hash(keySuffix) + Arrays.hashCode(data);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return String.format("MemPatriciaLeafNode{ %s = %s }",
            keySuffix, ByteUtils.toFullHexString(data));
    }

    // ---------------------------------------------------------------------------------------------
}
