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
    // region BYTE ARRRAYS
    // =============================================================================================

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

    // endregion
    // =============================================================================================
    // region BYTE VALUES
    // =============================================================================================

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
     * This returns 1 if the value is 0.
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
     * {@code [0,4]}.
     *
     * <p>Returns 0 if the byte array is of length 0, making this compatible with RLP-encoding of
     * byte arrays.
     */
    public static int toInt (byte... bytes) {
        if (bytes.length == 0) return 0;
        assert bytes.length <= 4;
        int out = 0;
        for (int i = 0; i < bytes.length; i++)
            out |= uint(bytes[i]) << (8 * (bytes.length - 1 - i));
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the integer encoded by the big-endian {@code bytes} array, whose size should be in
     * {@code [0,8]}.
     *
     * <p>Returns 0 if the byte array is of length 0, making this compatible with RLP-encoding of
     * byte arrays.
     */
    public static long toLong (byte... bytes) {
        if (bytes.length == 0) return 0;
        assert bytes.length <= 8;
        long out = 0;
        for (int i = 0; i < bytes.length; i++)
            out |= (long) uint(bytes[i]) << (8 * (bytes.length - 1 - i));
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Pads the given byte array to the given {@code length} by adding zeroes
     * at the start.
     *
     * <p>{@code length} should be bigger or equal to the array size.
     */
    public static byte[] bytesPadded (byte[] bytes, int length) {
        Assert.that(length >= bytes.length,
            "Byte array of size %d is larger than the target padding size %d", bytes.length, length);
        byte[] result = new byte[length];
        int dstOffset = length - bytes.length;
        System.arraycopy(bytes, 0, result, dstOffset, bytes.length);
        return result;
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
        Assert.that(unpadded.length <= length, "Length smaller than unpadded encoding");
        int srcOffset = unpadded[0] == 0 ? 1 : 0; // exlude byte included only for sign bit
        int unpaddedLenth = unpadded.length - srcOffset;
        int dstOffset = length - unpaddedLenth;

        Assert.that(length >= unpaddedLenth,
            "Input is too large to put in byte array of size %s", length);

        System.arraycopy(unpadded, srcOffset, result, dstOffset, unpaddedLenth);
        return result;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the big-endian encoding of {@code value} in bytes, excluding any byte that would
     * be included solely to signify the sign of the value.
     *
     * <p><b>Attention:</b> This means that the 0 big integer encodes as 0-length byte array. This
     * is intended behaviour, as this is how Ethereum encodes such values.
     */
    public static byte[] bytesWithoutSign (BigInteger value) {
        byte[] unpadded = value.toByteArray();
        return unpadded[0] != 0
            ? unpadded
            : Arrays.copyOfRange(unpadded, 1, unpadded.length);
    }

    // ---------------------------------------------------------------------------------------------

    /** Sets {@code dst[index, index + src.length]} to {@code src}. */
    public static byte[] setRangeAt (byte[] dst, int index, byte[] src) {
        assert 0 <= index && index + src.length <= dst.length;
        System.arraycopy(src, 0, dst, index, src.length);
        return dst;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Just like {@link Arrays#copyOfRange(byte[], int, int)}, but allows passing a size instead
     * of an end index.
     */
    public static byte[] copyOfSizedRange (byte[] array, int index, int size) {
        return Arrays.copyOfRange(array, index, index + size);
    }

    // endregion
    // =============================================================================================
    // region HEX STRINGS
    // =============================================================================================

    /**
     * Converts a hex digit (in 0-9, a-f or A-F) to its numeric value.
     */
    public static byte hexDigitToByte (char hex) {
        if ('0' <= hex && hex <= '9')
            return (byte) (hex - '0');
        if ('a' <= hex && hex <= 'f')
            return (byte) (10 + hex - 'a');
        if ('A' <= hex && hex <= 'F')
            return (byte) (10 + hex - 'A');
        throw new IllegalArgumentException("Not an hex digit: '" + hex + "'");
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Converts an integer in [0, 15] into an hex digit (0..9 a..f).
     */
    public static char intToHexDigit (int b) {
        if (0 <= b && b <= 9)
            return (char) ('0' + b);
        if (10 <= b && b <= 15)
            return (char) ('a' + b - 10);
        throw new IllegalArgumentException("Byte not in hex digit range: " + b);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Converts a hex-string (e.g. "0x123") to a byte array, with the first digits occupying the
     * first array slots. If there is odd number of digit, the first byte matches the first digit
     * (its higher-order nibble (4 bits) will be 0).
     *
     * <p>This accepts the empty hex string ("0x"), for which an empty array is returned.
     */
    public static byte[] hexStringToBytes (String hexString) {
        return hexStringToBytes(hexString, 0);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Converts a hex-string (e.g. "0x123") in the same way as {@link #hexStringToBytes(String)},
     * but ensure the returned array will have at least the given minimum length.
     *
     * <p>If the minimum size is larger than the natural size, the low-index bytes are left zeroed.
     *
     * <p>This accepts the empty hex string ("0x"), for which an empty array is returned.
     */
    public static byte[] hexStringToBytes (String hexString, int minLen) {
        Assert.arg(hexString.startsWith("0x"), "Hex string does not start with 0x: %s", hexString);

        int strLen = hexString.length() - 2; // for "0x"
        if (strLen == 0) return new byte[0];
        int natLen = (strLen - 1) / 2 + 1; // strLen/2, rounded up
        int len = Math.max(natLen, minLen);
        byte[] array = new byte[len];
        int odd = 0;
        int strIndex = 2;
        int arrIndex = Math.max(0, minLen - natLen);

        try {
            // align pairs of hex digits with full bytes
            if (strLen % 2 == 1) {
                array[arrIndex++] = hexDigitToByte(hexString.charAt(strIndex++));
                odd = 1;
            }

            // iterate on every character
            while (strIndex < hexString.length()) {
                byte b = hexDigitToByte(hexString.charAt(strIndex));
                // alternate filling the high and low order nibble (4 bits)
                if ((strIndex + odd) % 2 == 0)
                    array[arrIndex] |= b << 4;
                else
                    array[arrIndex++] |= b;
                ++strIndex;
            }
            return array;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("not a valid hex string: " + hexString, e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Converts a byte array into a hex string (e.g. "0x123"). This hex string does display
     * all bytes in the byte array, even if it starts with 0s.
     *
     * <p>This returns "0x" for empty byte arrays.
     *
     * @see #toCompressedHexString(byte[])
     */
    public static String toFullHexString (byte[] bytes) {
        var builder = new StringBuilder(bytes.length * 2);
        builder.append("0x");
        for (byte b: bytes) {
            builder.append(intToHexDigit((b & 0xF0) >>> 4));
            builder.append(intToHexDigit(b & 0xF));
        }
        return builder.toString();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Converts a byte array into a hex string (e.g. "0x123"). This hex string does not preserve
     * the length of the byte array if it starts with 0s: the returned string never has leading 0s
     * after the "0x" prefix.
     *
     * <p>This returns "0x" for empty byte arrays, or arrays comprising only 0s.
     *
     * @see #toFullHexString(byte[])
     */
    public static String toCompressedHexString (byte[] bytes) {
        var builder = new StringBuilder(bytes.length * 2);
        builder.append("0x");
        var leading0 = true;
        for (byte b: bytes) {
            if (leading0) {
                if (b == 0) continue;
                leading0 = false;
                if (b >>> 4 != 0) builder.append(intToHexDigit((b & 0xF0) >>> 4));
                builder.append(intToHexDigit(b & 0xF));
                continue;
            }
            builder.append(intToHexDigit((b & 0xF0) >>> 4));
            builder.append(intToHexDigit(b & 0xF));
        }
        return builder.toString();
    }

    // endregion
    // =============================================================================================
}
