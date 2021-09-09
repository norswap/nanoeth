package com.norswap.nanoeth.data;

import com.norswap.nanoeth.Config;
import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.blocks.BlockHeader;
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
     * <p>Not to be confused with {@link #ZERO}.
     * <p>Used as destination for contract creation.
     */
    public static Address EMPTY = new Address();

    // ---------------------------------------------------------------------------------------------

    /**
     * An address composed of 20 zero bytes.
     * <p>Not to be confused with {@link #EMPTY}.
     * <p>Used as coinbase for the {@link Config#GENESIS genesis block}.
     */
    public static Address ZERO = new Address(new byte[20]);

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


    public Address (String hexString) {
        this(ByteUtils.hexStringToBytes(hexString, 20));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a natural from a hex-string (e.g. "0x95ad61b0a150d79219dcf64e1e6cc01f0b64c4ce").
     *
     * <p>If the hex string is empty or simply "0x", then the {@link #EMPTY} address is returned.
     *
     * <p>If the post-0x part of the hex string is not 40 characters long, the address will be
     * padded with zeroes at the start so that it is 20 bytes long.
     */
    public static Address from (String hexString) {
        return hexString.length() == 0 || hexString.equals("0x")
            ? EMPTY
            : new Address(ByteUtils.hexStringToBytes(hexString, 20));
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
