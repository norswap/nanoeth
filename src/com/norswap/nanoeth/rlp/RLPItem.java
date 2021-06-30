package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.data.Bytes;

/**
 * An item that can be RLP-encoded.
 *
 * @see RLP#encode(RLPItem)
 */
public abstract class RLPItem {
    RLPItem() {}

    // ---------------------------------------------------------------------------------------------

    /** Encodes the RLP item to a byte sequence. */
    public abstract Bytes encode();

    // ---------------------------------------------------------------------------------------------
}
