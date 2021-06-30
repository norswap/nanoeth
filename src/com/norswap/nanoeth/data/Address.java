package com.norswap.nanoeth.data;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.annotations.Wrapper;

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

    private final byte[] address;

    // ---------------------------------------------------------------------------------------------

    /**
     * Constructor for the {@link #EMPTY} address.
     */
    private Address() {
        this.address = new byte[0];
    }

    // ---------------------------------------------------------------------------------------------

    public Address (@Retained byte[] address) {
        assert address.length == 20;
        this.address = address;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the big-endian representation of the address. Usually 20-bytes long, zero-length
     * if {@link #EMPTY}.
     */
    public byte[] address() {
        return address;
    }

    // ---------------------------------------------------------------------------------------------
}
