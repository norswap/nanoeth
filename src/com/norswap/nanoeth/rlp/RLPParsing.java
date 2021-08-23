package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.receipts.BloomFilter;
import com.norswap.nanoeth.utils.ByteUtils;

/**
 * Utilities to help parse protocol objects from RLP input.
 */
public final class RLPParsing {
    private RLPParsing() {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Parses the i-th item of the sequence, which should be a byte array of size no greater than
     * 32, into a natural number.
     */
    public static Natural getNatural (RLP seq, int i)
            throws RLPParsingException {

        byte[] bytes = getBytes(seq, i);
        if (bytes.length > 32)
            throw new RLPParsingException(
                    "Natural should not be more than 32 bytes long.");
        return new Natural(bytes);
    }
    
    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into a 32-bit integer. */
    public static int getInt (RLP seq, int i)
            throws RLPParsingException {

        return ByteUtils.toInt(getBytes(seq, i));
    }
    
    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a 8-byte array, into a 64-bit integer. */
    public static long getInt64 (RLP seq, int i)
            throws RLPParsingException {

        var bytes = getBytes(seq, i);
        if (bytes.length != 8)
            throw new RLPParsingException("expected an 8-bytes (64-bit) integer but got "
                + bytes.length + " bytes instead");

        return ByteUtils.toLong(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into an address. */
    public static Address getAddress (RLP seq, int i)
            throws RLPParsingException {

        var bytes = getBytes(seq, i);
        if (bytes.length == 0)  return Address.EMPTY;
        if (bytes.length == 20) return new Address(bytes);
        throw new RLPParsingException("Address should be 20 bytes long");
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into a hash. */
    public static Hash getHash (RLP seq, int i)
            throws RLPParsingException {

        var bytes = getBytes(seq, i);
        if (bytes.length != 32)
            throw new RLPParsingException("Hash should be 32 bytes long");

        return new Hash(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into a hash. */
    public static MerkleRoot getMerkleRoot (RLP seq, int i)
            throws RLPParsingException {

        var bytes = getBytes(seq, i);
        if (bytes.length != 32)
            throw new RLPParsingException("Merkle root should be 32 bytes long");

        return new MerkleRoot(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into a bloom filter. */
    public static BloomFilter getBloomFilter (RLP seq, int i)
            throws RLPParsingException {

        var bytes = getBytes(seq, i);
        if (bytes.length != 256)
            throw new RLPParsingException("Bloom filter should be 256 bytes long");

        return new BloomFilter(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /** Retrieves the i-th item of the sequence, and verifies that it is a byte array with a valid
     * size. */
    public static byte[] getBytes (RLP seq, int i)
            throws RLPParsingException {

        if (i >= seq.items().length) throw new RLPParsingException(
            "decoded RLP is too short");

        var item = seq.itemAt(i);

        if (!item.isBytes()) throw new RLPParsingException(
            "decoded RLP has illegal format: expected byte array at index " + i);

        return item.bytes();
    }

    // ---------------------------------------------------------------------------------------------
}
