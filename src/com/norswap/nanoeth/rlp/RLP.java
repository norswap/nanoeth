package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.data.Bytes;

/**
 * Enables encoding and decoding from RLP format, as specified in appendix B of the yellowpaper.
 *
 * <p>This implementation only supports byte sequences and item sequences with the maximum length
 * allowed by Java, which is slightly under 2^31. Ethereum allows byte and item sequences of length
 * up to 2^64.
 */
public final class RLP {
    private RLP () {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Encodes the given item to a byte sequence in accordance to the RLP format.
     *
     * @see RLPItem#encode()
     */
    public static Bytes encode (RLPItem item) {
        return item.encode();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Decodes the given byte sequence to an {@link RLPItem}.
     *
     * @throws IllegalArgumentException if the given byte sequence is not well-formed RLP.
     */
    public static RLPItem decode (Bytes bytes) {
        return RLPImplem.decode(bytes);
    }

    // ---------------------------------------------------------------------------------------------
}
