package com.norswap.nanoeth.data;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPLayoutable;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.utils.Hashing;
import java.util.Arrays;

/**
 * Represents a 256-bit (32 bytes) Keccak hash.
 *
 * @see com.norswap.nanoeth.utils.Hashing
 */
@Wrapper
public class Hash implements RLPLayoutable {

    // ---------------------------------------------------------------------------------------------

    /** A hash composed of only zero bytes. */
    public static final Hash ZERO = new Hash(new byte[32]);

    // ---------------------------------------------------------------------------------------------

    /**
     * Hash of the empty RLP sequence.
     * <p>Can notably be compared against the uncle hash of a block header to see if the
     * block has any uncles.
     */
    public static final Hash EMPTY_SEQ_HASH = Hashing.keccak(RLP.sequence(new Object[0]).encode());

    // ---------------------------------------------------------------------------------------------

    /** The bytes making up the hash. */
    public final byte[] bytes;

    // ---------------------------------------------------------------------------------------------

    public Hash (@Retained byte[] bytes) {
        Assert.that(bytes.length == 32, "hash is not 32 bytes long");
        this.bytes = bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a hash from a hex string (e.g. 0x123).
     *
     * <p>If the post-0x part of the hex string is not 64 characters long, the hash will be padded
     * with zeroes at the start so that it is 32 bytes long.
     */
    public Hash (String hexString) {
        this(ByteUtils.hexStringToBytes(hexString, 32));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * The hex-string representation of this hash, including leading 0 if any, as per {@link
     * ByteUtils#toFullHexString(byte[])}.
     */
    public String toFullHexString() {
        return ByteUtils.toFullHexString(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public RLP rlpLayout() {
        return RLP.bytes(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof Hash && Arrays.equals(bytes, ((Hash) o).bytes);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(bytes);
    }

    @Override public String toString() {
        return toFullHexString();
    }

    // ---------------------------------------------------------------------------------------------
}
