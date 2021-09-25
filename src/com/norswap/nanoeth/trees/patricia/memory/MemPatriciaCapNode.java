package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.AbridgedNode;
import com.norswap.nanoeth.trees.patricia.MerkleProof;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;
import java.util.Map;

/**
 * This is a special kind of node that never occurs in a normal patricia trie, but is used to
 * facilitate the construction of partial tree and the verification of {@link MerkleProof Merkle
 * proofs}.
 * <p>
 * This node has no children, but always has a memoized result for its {@link #cap() cap value} All
 * other implemented methods throw exceptions.
 */
public final class MemPatriciaCapNode extends MemPatriciaNode {

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new node with the given cap value (memoized result for the {@link #cap()} method).
     */
    public MemPatriciaCapNode (byte[] cap) {
        this.cap = cap;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Type type() {
        throw new UnsupportedOperationException();
    }

    @Override public Step step (Nibbles keySuffix) {
        throw new UnsupportedOperationException();
    }

    @Override public byte[] lookup (Nibbles keySuffix) {
        throw new UnsupportedOperationException();
    }

    @Override public void collectEntries (Nibbles prefix, Map<byte[], byte[]> map) {
        throw new UnsupportedOperationException();
    }

    @Override public MemPatriciaNode add (Nibbles keySuffix, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override public MemPatriciaNode remove (Nibbles keySuffix) {
        throw new UnsupportedOperationException();
    }

    @Override public AbridgedNode abridged () {
        throw new UnsupportedOperationException();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof MemPatriciaCapNode
            && Arrays.equals(this.cap, ((MemPatriciaCapNode) o).cap);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(cap);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return String.format("MemPatriciaCapNode { %s }", ByteUtils.toFullHexString(cap));
    }

    // ---------------------------------------------------------------------------------------------
}
