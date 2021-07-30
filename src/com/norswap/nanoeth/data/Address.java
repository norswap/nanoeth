package com.norswap.nanoeth.data;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;

/**
 * Represents a 160-bit (20 bytes) Ethereum address.
 */
@Wrapper
public final class Address {

    // ---------------------------------------------------------------------------------------------

    /**
     * The address the yellow paper denotes as "∅" and is represent by a zero-length byte sequence,
     * unlike all other addresses which are 20-bytes long.
     */
    public static Address EMPTY = new Address();

    // ---------------------------------------------------------------------------------------------

    /**
     * The big-endian representation of the address. Usually 20-bytes long, zero-length if {@link
     * #EMPTY}.
     */
    public final byte[] bytes;

    // ---------------------------------------------------------------------------------------------

    /**
     * Constructor for the {@link #EMPTY} address.
     */
    private Address() {
        this.bytes = new byte[0];
    }

    // ---------------------------------------------------------------------------------------------

    public Address (@Retained byte[] bytes) {
        Assert.that(bytes.length == 20, "byte array for address is not 20 bytes long");
        this.bytes = bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a natural from a hex-string (e.g. "0x95ad61b0a150d79219dcf64e1e6cc01f0b64c4ce").
     *
     * <p>If the post-0x part of the hex string is not 40 characters long, the address will be
     * padded with zeroes at the start so that it is 20 bytes long.
     */
    public Address (String hexString) {
        this(ByteUtils.hexStringToBytes(hexString, 20));
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof Address && Arrays.equals(bytes, ((Address) o).bytes);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(bytes);
    }

    @Override public String toString() {
        return ByteUtils.toCompressedHexString(bytes);
    }

    // ---------------------------------------------------------------------------------------------
}
