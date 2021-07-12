package com.norswap.nanoeth.utils;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Utilities dealing with {@code byte} values, the byte-array encoding of larger numbers, as well
 * as byte arrays in general.
 */
public final class ByteUtils {
    private ByteUtils () {}

    // =============================================================================================
    // BYTE ARRRAYS

    /**
     * Returns the concatenation of the byte arrays.
     */
    public static byte[] concat (byte[]... bytes) {
        int size = Arrays.stream(bytes).map(d -> d.length).reduce(0, Integer::sum);
        byte[] out = new byte[size];
        int pos = 0;
        for (byte[] d: bytes) {
            System.arraycopy(d, 0, out, pos, d.length);
            pos += d.length;
        }
        return out;
    }

    // =============================================================================================
    // BYTE VALUES

    /**
     * True if the value is in the signed byte range [-128, 127] or if it is in [128, 255]
     * (in which case casting it to a byte yield a negative value which can be used to represent
     * an unsigned byte value).
     */
    public static boolean isByte (int value) {
        return -127 <= value && value <= 255;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Short way to create an array of bytes that also allows 0-255 values.
     */
    public static byte[] array (int... bytes) {
        byte[] out = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            assert isByte(bytes[i]);
            out[i] = (byte) bytes[i];
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Converts the byte to an {@code int} by reinterpreting its bit pattern as that of an unsigned
     * number.
     */
    public static int uint (byte b) {
        return ((int) b) & 0xff;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Adds two bytes together with wrap-around behaviour (e.g. {@code 127 + 1 == -128}).
     *
     * <p>This works whether you want to interpret the byte values as signed or unsigned (where this
     * is equivalent to åddition modulo 255).
     *
     * <p>In particular, this behaviour is helpful to implement unsigned byte addition: e.g. {@code
     * 127+127} will overflow to {@code -129 == 256 - 127} which is the signed representation of
     * {@code 127}.
     *
     * <p>The arguments are supplied as integer, to avoid tiresome casting. Only integer values
     * that can be legaly truncated to a byte value
     */
    public static byte addMod (int a, int b) {
        assert isByte(a) && isByte(b);
        // The addition promotes to int, the cast truncates to byte.
        // This produces the same behaviour as wraparound byte addition
        return (byte) (a + b);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Samed as {@link #addMod} but indicates that the numbers are intepreted as unsigned bytes, and
     * that the addition should not overflow under that interpretation.
     */
    public static byte uadd (int a, int b) {
        assert -256 <= a && a <= 255 && -256 <= b && b <= 255;
        byte result = addMod(a, b);
        assert uint(result) == uint((byte) a) + uint((byte) b);
        return result;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the minimum number of bytes needed to store the integer.
     */
    public static int byteSize (int value) {
        if (value == (value & 0xFF))        return 1;
        if (value == (value & 0xFFFF))      return 2;
        if (value == (value & 0xFFFFFF))    return 3;
        else                                return 4;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a byte array of size {@code byteSize(value)} encoding {@code value} in big-endian.
     */
    public static byte[] bytes (int value) {
        int size = byteSize(value);
        byte[] out = new byte[size];
        for (int i = 0; i < size; ++i) {
            out[i] = (byte) (value >>> ((size - 1 - i) * 8));
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the integer encoded by the big-endian {@code bytes} array, whose size should be in
     * {@code ]0,4]}.
     */
    public static int toInt (byte[] bytes) {
        assert 0 < bytes.length && bytes.length <= 4;
        int out = 0;
        for (int i = 0; i < bytes.length; i++)
            out |= uint(bytes[i]) << (8 * (bytes.length - 1 - i));
        return out;
    }


    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the big-endian encoding of {@code value} in bytes padded (at the start) with zeroes
     * so that the length of the output is at least {@code length}.
     *
     * <p>{@code length} should be bigger or equal than the length unpadded encoding of {@code
     * value}, excluding the sign bit.
     *
     * <p>This method does not require a sign bit to be included in the result, and should only
     * be called with positive values.
     */
    public static byte[] bytesPadded (BigInteger value, int length) {
        Assert.that(value.compareTo(BigInteger.ZERO) >= 0, "Negative value");

        byte[] result = new byte[length];
        byte[] unpadded = value.toByteArray();
        int srcOffset = unpadded[0] == 0 ? 1 : 0; // exlude byte included only for sign bit
        int unpaddedLenth = unpadded.length - srcOffset;
        int dstOffset = length - unpaddedLenth;

        Assert.that(length >= unpaddedLenth,
            "Input is too large to put in byte array of size %s", length);

        System.arraycopy(unpadded, srcOffset, result, dstOffset, unpaddedLenth);
        return result;
    }

    // ---------------------------------------------------------------------------------------------
}
