package com.norswap.nanoeth.blocks;

public final class ProofOfWork {
    private ProofOfWork() {}

    /**
     * Verifies the proof of work computation for the given header, i.e. that the header content,
     * hash and nonce are consistent. This assumes that the nonce and the difficulty have been
     * validated upstream.
     */
    public static boolean verifyPoW (BlockHeader header) {
        // TODO
        return true;
    }
}
