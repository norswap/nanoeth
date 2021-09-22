package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.MerkleProof;
import com.norswap.nanoeth.trees.patricia.MerkleProofBuilder;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;
import java.util.Map;

/**
 * This is a special kind of node that never occurs in a normal patricia trie, but is used to
 * facilitate the verification of {@link MerkleProof Merkle proofs}.
 * <p>
 * This node has no children, but always has a memoized result for its {@link #cap()} method (which
 * we here call "digest"). All other implemented methods throw exceptions.
 */
public final class MemPatriciaDigestNode extends MemPatriciaNode {

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new node with the given digest (memoized result for the {@link #cap()} method.
     */
    public MemPatriciaDigestNode (byte[] digest) {
        this.digest = digest;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (Nibbles keySuffix) {
        throw new UnsupportedOperationException();
    }

    @Override public void collectEntries (Nibbles prefix, Map<byte[], byte[]> map) {
        throw new UnsupportedOperationException();
    }

    @Override public MemPatriciaNode add (Nibbles keySuffix, byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override public MemPatriciaNode remove (Nibbles keySuffix) {
        throw new UnsupportedOperationException();
    }

    @Override public void buildProof (Nibbles keySuffix, MerkleProofBuilder builder) {
        throw new UnsupportedOperationException();
    }

    @Override public RLP compose() {
        throw new UnsupportedOperationException();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof MemPatriciaDigestNode
            && Arrays.equals(this.digest, ((MemPatriciaDigestNode) o).digest);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(digest);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return String.format("MemPatriciaDigestNode { %s }", ByteUtils.toFullHexString(digest));
    }

    // ---------------------------------------------------------------------------------------------
}
