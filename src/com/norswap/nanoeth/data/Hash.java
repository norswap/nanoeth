package com.norswap.nanoeth.data;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;

/**
 * Represents a 256-bit (32 bytes) Keccak hash.
 *
 * @see com.norswap.nanoeth.utils.Hashing
 */
@Wrapper
public final class Hash {
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
     * Creates a natural from a hex string (e.g. 0x123).
     *
     * <p>If the post-0x part of the hex string is not 64 characters long, the hash will be padded
     * with zeroes at the start so that it is 32 bytes long.
     */
    public Hash (String hexString) {
        // TODO test more to determine if this padding behaviour is needed
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
