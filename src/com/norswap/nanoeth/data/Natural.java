package com.norswap.nanoeth.data;

import com.norswap.nanoeth.annotations.Wrapper;
import java.math.BigInteger;

/**
 * Represents an arbitrary-length natural (positive) integer.
 */
@Wrapper
public final class Natural
{
    // ---------------------------------------------------------------------------------------------

    private final BigInteger integer;

    // ---------------------------------------------------------------------------------------------

    public Natural (long nat) {
        assert nat >= 0;
        this.integer = BigInteger.valueOf(nat);
    }

    // ---------------------------------------------------------------------------------------------

    public Natural (BigInteger nat) {
        assert nat.signum() >= 0;
        this.integer = nat;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Constructs a natural from a big-endian byte array, which will be interpreted as a positive
     * integer (e.g. {@code [0xFF]} is 255, not -1).
     */
    public Natural (byte[] nat) {
        this.integer = new BigInteger(1, nat);
    }

    // ---------------------------------------------------------------------------------------------
}
