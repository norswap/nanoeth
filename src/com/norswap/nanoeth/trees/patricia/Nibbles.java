package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.trees.patricia.memory.MemPatriciaLeafNode;
import com.norswap.nanoeth.utils.ByteUtils;

/**
 * Represent a sequence of nibbles (half-bytes, 4-bit values), which are used as key fragments
 * in Patricia tree lookups.
 * <p>
 * The {@link #hexPrefix(boolean)} method implements the hex-prefix encoding for sequences of
 * nibbles, as specified in appendix C of the yellowpaper.
 * <p>
 * Note that when represented as an array of bytes, the spec specifies big-endian ordering
 * (i.e. the first nibble in a byte is the high-order nibble).
 */
public final class Nibbles {
    // ---------------------------------------------------------------------------------------------

    /** The byte array (typically a whole key, hence the name) backing the nibble sequence. */
    private final byte[] key;

    /** The start nibble index (not a byte index!) in {@link #key}. */
    private final int start;

    /** The exclusive end nibble index (not a byte index!) in {@link #key}. */
    private final int end;

    // ---------------------------------------------------------------------------------------------

    public Nibbles (@Retained byte[] key) {
        this(key, 0, key.length * 2);
    }

    // ---------------------------------------------------------------------------------------------

    /** Construct a nibble sequence containing a single nibble. */
    public Nibbles (byte nibble) {
        this(new byte[]{ nibble }, 1, 2);
        assert nibble < 16 : "value is not a nibble";
    }

    // ---------------------------------------------------------------------------------------------

    public Nibbles (@Retained byte[] key, int start, int end) {
        assert start <= end : "start > end";
        this.key = key;
        this.start = start;
        this.end = end;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a {@link Nibbles} object that represent the hex-prefix encoded nibble sequence, as
     * per appendix C of the yellowpaper. See {@link #hexPrefix(boolean)} for detail on the
     * encoding.
     * <p>
     * The returned object will be backed by the passed array.
     */
    public static Nibbles fromHexPrefix (@Retained byte[] hexPrefixEncoded) {
        var oddSize = (hexPrefixEncoded[0] & 0x10) != 0;
        return new Nibbles(hexPrefixEncoded, oddSize ? 1 : 2, hexPrefixEncoded.length);
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the nibble at the given index. */
    public byte get (int i) {
        assert 0 <= i && i < length() : "index out of range";
        int index = start + i;
        return index % 2 == 0
            ? (byte) ((key[index / 2] & 0xF0) >>> 4)
            : (byte)  (key[index / 2] & 0x0F);
    }

    // ---------------------------------------------------------------------------------------------

    /** Return the number of nibbles represented by this object. */
    public int length() {
        return end - start;
    }

    // ---------------------------------------------------------------------------------------------

    /** Return the amount of nibbles that this object and {@code o} share as prefix. */
    public int sharedPrefix (Nibbles o) {
        final int max = Math.min(length(), o.length());
        int i = 0;
        for (; i < max; ++i)
            if (get(i) != o.get(i)) break;
        return i;
    }

    // ---------------------------------------------------------------------------------------------

    /** Return a prefix of this nibble sequence, keeping the first {@code n} nibbles. */
    public Nibbles prefix (int n) {
        assert n <= length() : "trying to get a prefix bigger than the sequence";
        return new Nibbles(key, start, start + n);
    }

    // ---------------------------------------------------------------------------------------------

    /** Return a prefix of this nibble sequence, dropping the last {@code n} nibbles. */
    public Nibbles dropLast (int n) {
        assert n <= length() : "trying to drop more nibbles than available";
        return new Nibbles(key, start, end - n);
    }

    // ---------------------------------------------------------------------------------------------

    /** Return a suffix of this nibble sequence, keeping the last {@code n} nibbles. */
    public Nibbles suffix (int n) {
        assert n <= length() : "trying to get a suffix bigger than the sequence";
        return new Nibbles(key, end - n, end);
    }

    // ---------------------------------------------------------------------------------------------

    /** Return a suffix of this nibble sequence, shaving off the first {@code n} nibbles. */
    public Nibbles dropFirst (int n) {
        assert n <= length() : "trying to drop more nibbles than available";
        return new Nibbles(key, start + n, end);
    }

    // ---------------------------------------------------------------------------------------------

    public Nibbles concat (Nibbles other) {
        // optimize a common case that consists of concatenating two contiguous spans of the same key
        if (key == other.key && end == other.start)
            return new Nibbles(key, start, other.end);

        int nibbleLength = length() + other.length();
        var newKey = new byte[nibbleLength / 2 + nibbleLength % 2];
        copyNibbles(key, start, newKey, 0, length());
        copyNibbles(other.key, other.start, newKey, length(), other.length());
        return new Nibbles(newKey, 0, nibbleLength);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the hex-prefix encoding of the nibble sequence, as per appendix C of the yellowpaper.
     * <p>
     * This encoding sets two flags in the first (highest-order) nibble: whether the number of
     * nibble is odd (lowest-order bit), and whether the nibble sequence is the key suffix
     * associated with a {@link MemPatriciaLeafNode} (indicated by the {@code leaf} parameter, stored in
     * the second lowest-order bit).
     * <p>
     * If the number of nibbles is odd, the second highest-order nibble is the first nibble,
     * otherwise it is zero and the third highest-order nibble is the first nibble.
     */
    public byte[] hexPrefix (boolean leaf) {

        byte[] out = new byte[length() / 2 + 1];
        int i; // indexes the nibbles in `out`

        if (length() % 2 == 0) {
            // set second low-order bit of first nibble
            if (leaf) out[0] = 0x20;
            i = 2;
        } else {
            // set second & first low-order bit of first nibble
            out[0] = (byte) (leaf ? 0x30 : 0x10);
            i = 1;
        }

        copyNibbles(key, start, out, i, length());
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Copies {@code length} nibbles from {@code src} starting at nibble index {@code srcPos}
     * into <b>the zeroed</b> array {@code dst} at nibble index {@code dstPos}.
     */
    private void copyNibbles (byte[] src, int srcPos, byte[] dst, int dstPos, int length) {
        for (int i = 0; i < length; ++i) {
            int idst = dstPos + i;
            int isrc = srcPos + i;
            var srcByte = src[isrc / 2];
            int srcNibble = isrc % 2 == 0
                ? (srcByte & 0xF0) >>> 4
                :  srcByte & 0x0F;
            dst[idst / 2] |= (idst % 2 == 0)
                ? srcNibble << 4
                : srcNibble;
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a copy of this nibble sequence as an array of bytes. If the number of nibbles in the
     * sequence is odd, there is an extra nibble in the byte array, which is set to 0.
     */
    public byte[] bytes() {
        var out = new byte[length() / 2 + length() % 2];
        copyNibbles(key, start, out, 0, length());
        if (length() % 2 == 1) out[length() / 2] &= 0xF0;
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a string representation of this nibble sequence, as an hex string (e.g. 0x123)
     * containing as many characters after "0x" as there are nibbles in the sequence.
     */
    @Override public String toString() {
        return "0x" + ByteUtils.toFullHexString(key).substring(2 + start, 2 + end);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return o instanceof Nibbles
            && length() == ((Nibbles) o).length()
            && sharedPrefix((Nibbles) o) == length();
    }

    @Override public int hashCode () {
        int hash = 0;
        for (int i = start; i < end; i++)
            hash = 31 * hash + get(i);
        return hash;
    }

    // ---------------------------------------------------------------------------------------------
}
