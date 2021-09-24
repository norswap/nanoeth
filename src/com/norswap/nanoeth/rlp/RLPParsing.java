package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.utils.ByteUtils;

/**
 * Utilities to help parse protocol objects from a RLP layout.
 */
public final class RLPParsing {
    private RLPParsing() {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Same as {@link RLP#bytes()} but converts an (unchecked) {@link IllegalRLPAccess} to a
     * (checked) {@link RLPParsingException}.
     */
    public static byte[] getBytes(RLP rlp) throws RLPParsingException {
        try {
            return rlp.bytes();
        } catch (IllegalRLPAccess e) {
            throw new RLPParsingException(
                    "trying to access bytes, but RLP object is a sequence or an encoding", e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Same as {@link RLP#items()} but converts an (unchecked) {@link IllegalRLPAccess} to a
     * (checked) {@link RLPParsingException}.
     */
    public static RLP[] getItems(RLP rlp) throws RLPParsingException {
        try {
            return rlp.items();
        } catch (IllegalRLPAccess e) {
            throw new RLPParsingException(
                "trying to access sequence items, but RLP object represent bytes or an encoding", e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Parses the i-th item of the sequence, which should be a byte array of size 1, into a
     * unsigned byte.
     */
    public static int getByte (RLP seq, int i) throws RLPParsingException {
        var bytes = getBytes(seq, i);
        if (bytes.length == 1) return ByteUtils.uint(bytes[0]);
        throw new RLPParsingException("Expected a single byte but got "
            + bytes.length + " bytes instead.");
    }
    
    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a 8-byte array, into a 64-bit integer. */
    public static long getInt64 (RLP seq, int i) throws RLPParsingException {
        var bytes = getBytes(seq, i);
        if (bytes.length == 8) return ByteUtils.toLong(bytes);
        throw new RLPParsingException("Expected an 8-bytes (64-bit) integer but got "
            + bytes.length + " bytes instead.");
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Retrieves the i-th item of the sequence, and verifies that it is a byte array with a valid
     * size.
     */
    public static byte[] getBytes (RLP seq, int i) throws RLPParsingException {
        if (i >= seq.items().length) throw new RLPParsingException("Decoded RLP is too short.");
        var item = seq.itemAt(i);
        if (item.isBytes()) return item.bytes();
        throw new RLPParsingException("Expected byte array at index " + i + ".");
    }

    // ---------------------------------------------------------------------------------------------
}
