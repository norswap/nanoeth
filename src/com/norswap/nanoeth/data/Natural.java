package com.norswap.nanoeth.data;

import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPLayoutable;
import com.norswap.nanoeth.utils.ByteUtils;
import java.math.BigInteger;

/**
 * Represents an arbitrary-length natural (positive) integer.
 */
public final class Natural extends BigInteger implements RLPLayoutable
{
    // ---------------------------------------------------------------------------------------------

    /** Zero, as a natural, to avoid holding duplicated copies. */
    public static final Natural ZERO = new Natural(0);

    // ---------------------------------------------------------------------------------------------

    public Natural (long nat) {
        this(BigInteger.valueOf(nat));
    }

    // ---------------------------------------------------------------------------------------------

    /** Creates a natural from a hex-string (e.g. "0x123"). */
    public Natural (String hexString) {
        this(new BigInteger(hexString.substring(2), 16));
    }

    // ---------------------------------------------------------------------------------------------

    public Natural (BigInteger nat) {
        super(1, nat.toByteArray());
        assert nat.signum() >= 0;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Constructs a natural from a big-endian byte array, which will be interpreted as a positive
     * integer (e.g. {@code [0xFF]} is 255, not -1).
     */
    public Natural (byte[] nat) {
        super(1, nat);
    }

    // ---------------------------------------------------------------------------------------------

    public boolean same (long value) {
        return compareTo(BigInteger.valueOf(value)) == 0;
    }

    public boolean greater (long value) {
        return compareTo(BigInteger.valueOf(value)) > 0;
    }

    public boolean lower (long value) {
        return compareTo(BigInteger.valueOf(value)) < 0;
    }

    public boolean greaterSame (long value) {
        return compareTo(BigInteger.valueOf(value)) >= 0;
    }

    public boolean lowerSame (long value) {
        return compareTo(BigInteger.valueOf(value)) <= 0;
    }

    // ---------------------------------------------------------------------------------------------

    public Natural add (long value) {
        return new Natural(add(BigInteger.valueOf(value)));
    }

    public Natural subtract (long value) {
        return new Natural(subtract(BigInteger.valueOf(value)));
    }

    public Natural multiply (long value) {
        return new Natural(multiply(BigInteger.valueOf(value)));
    }

    public Natural divide (long value) {
        return new Natural(divide(BigInteger.valueOf(value)));
    }

    public Natural mod (long value) {
        return new Natural(mod(BigInteger.valueOf(value)));
    }

    public Natural pow (int value) {
        return new Natural(super.pow(value));
    }

    // ---------------------------------------------------------------------------------------------

    public String toHexString() {
        return ByteUtils.toCompressedHexString(ByteUtils.bytesWithoutSign(this));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Natural are encoded in RLP using only as many bytes as necessary, with no leading 0.
     * <p>Zero itself is represented by an empty byte array.
     */
    @Override public RLP rlpLayout() {
        return RLP.bytes(ByteUtils.bytesWithoutSign(this));
    }

    // ---------------------------------------------------------------------------------------------
}
