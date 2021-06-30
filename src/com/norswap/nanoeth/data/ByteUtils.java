package com.norswap.nanoeth.data;

import java.util.Arrays;

import static java.lang.String.format;

/**
 * Utility dealing with byte values.
 */
public final class ByteUtils {
    private ByteUtils () {}

    // ---------------------------------------------------------------------------------------------

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
     * Returns a byte sequence of size {@code byteSize(value)} encoding {@code value} in big-endian.
     */
    public static Bytes byteSequence (int value) {
        return Bytes.from(bytes(value));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the integer encoded by the big-endian {@code bytes} array, whose size should be in
     * {@code ]0,4]}.
     */
    public static int toInt (Bytes bytes) {
        assert 0 < bytes.size() && bytes.size() <= 4;
        int out = 0;
        for (int i = 0; i < bytes.size(); i++)
            out |= uint(bytes.get(i)) << (8 * (bytes.size() - 1 - i));
        return out;
    }

    // ---------------------------------------------------------------------------------------------
}
