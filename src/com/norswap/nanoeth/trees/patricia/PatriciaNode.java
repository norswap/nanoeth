package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.MerkleRoot;
import java.util.Map;

/**
 * Interface for Modified Merkle Patricia Tree node, to be compatible with {@link PatriciaTree}.
 * <p>
 * nanoeth includes a in-memory patricia tree implementation (in the {@code memory} sub-package),
 * but such an implementation is not realistic in practice as the mainnet account trie is itself >
 * 20GB. The inclusion of this interface enables more efficient implementation to be plugged into
 * nanoeth, by subclassing {@link PatriciaTree}.
 */
public interface PatriciaNode {
    // ---------------------------------------------------------------------------------------------

    /**
     * Lookup the entry with the given key suffix, the suffix of a sequence of nibbles, where the
     * missing prefix was used to reach the present node.
     * <p>
     * This must handle empty nibble sequences.
     */
    byte[] lookup (Nibbles keySuffix);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed node, after associating the given data with the given key suffix.
     * <p>
     * This must handle empty nibble sequences.
     */
    PatriciaNode add (Nibbles keySuffix, byte[] data);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the transformed node, after removing the entry for the given key suffix (if any), or
     * returns {@code null} if the removal of the key means that the node itself must disappear.
     * <p>
     * This must handle empty nibble sequences.
     */
    PatriciaNode remove (Nibbles keySuffix);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the Merkle root of the Merkle tree rooted at this node. This implements the TRIE
     * function in the yellowpaper (equation 195).
     */
    MerkleRoot merkleRoot();

    // ---------------------------------------------------------------------------------------------

    /**
     * Adds all the entries store under this node to {@code map}, given that the prefix to reach
     * this node is given by {@code prefix}.
     */
    void collectEntries (Nibbles prefix, Map<byte[], byte[]> map);

    // ---------------------------------------------------------------------------------------------
}
