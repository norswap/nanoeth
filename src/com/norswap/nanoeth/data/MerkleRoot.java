package com.norswap.nanoeth.data;

import com.norswap.nanoeth.annotations.Wrapper;

/**
 * A {@link Hash} that is marked as being a Merkle root.
 *
 * <p>A Merkle root is the hash associated with the root of a Merkle tree.
 *
 * <p>The hash ("label") associated with a Merkle tree node is the hash of the data for leaf nodes,
 * and the hash of the children's labels for non-leaf nodes.
 */
@Wrapper
public final class MerkleRoot extends Hash {

    // ---------------------------------------------------------------------------------------------

    /**
     * A merkle root composed of only zero bytes, which happens to be the merkle root of empty
     * trees.
     */
    public static final MerkleRoot ZERO = new MerkleRoot(new byte[32]);

    // ---------------------------------------------------------------------------------------------

    public MerkleRoot (byte[] bytes) {
        super(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    public MerkleRoot (Hash hash) {
        super(hash.bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a merkle root from a hex string (e.g. 0x123).
     *
     * <p>If the post-0x part of the hex string is not 64 characters long, the hash will be padded
     * with zeroes at the start so that it is 32 bytes long.
     */
    public MerkleRoot (String hexString) {
        super(hexString);
    }

    // ---------------------------------------------------------------------------------------------
}
