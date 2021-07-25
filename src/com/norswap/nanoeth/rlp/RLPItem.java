package com.norswap.nanoeth.rlp;

/**
 * An item that can be RLP-encoded.
 *
 * @see RLP#encode(RLPItem)
 */
public abstract class RLPItem {
    RLPItem() {}

    // ---------------------------------------------------------------------------------------------

    /** Encodes the RLP item to a byte sequence. */
    public abstract byte[] encode();

    // ---------------------------------------------------------------------------------------------
}
