package com.norswap.nanoeth.data;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;

/**
 * Represents a (fixed-size) sequence of byte.
 *
 * <p>This is mutable for ease of construction, but it is generally understood it shouldn't
 * be modified after construction. Use {@link #freeze()} to signal this.
 */
public final class Bytes {

    // ---------------------------------------------------------------------------------------------

    public final byte[] storage;

    // ---------------------------------------------------------------------------------------------

    Bytes (byte[] storage) {
        this.storage = storage;
    }

    // ---------------------------------------------------------------------------------------------

    /** cf. {@link Bytes} */
    boolean frozen = false;

    // ---------------------------------------------------------------------------------------------

    /**
     * Create a byte sequence using the given byte array. The sequence is immediately frozen.
     */
    public static Bytes from (@Retained byte... bytes) {
        Bytes out = new Bytes(bytes);
        out.freeze();
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Create a byte sequence of the given size.
     */
    public static Bytes ofSize (int size) {
        return new Bytes(new byte[size]);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Indicate that the byte array has been fully initialized and should not be further modified.
     * <p>This can only be called once.
     * @return {@code this}
     */
    public Bytes freeze() {
        assert !frozen;
        frozen = true;
        return this;
    }

    // ---------------------------------------------------------------------------------------------

    /** Whether the byte sequence has already been frozen. Used for assertions. */
    public boolean frozen() {
        return frozen;
    }

    // ---------------------------------------------------------------------------------------------

    public int size() {
        return storage.length;
    }

    // ---------------------------------------------------------------------------------------------

    /** Return the byte at the given index. */
    public byte get (int i) {
        return storage[i];
    }

    // ---------------------------------------------------------------------------------------------

    /** Sets the byte at the given index. */
    public void set (int i, byte value) {
        assert !frozen;
        storage[i] = value;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Sets the [index, other.length] range of this byte sequence to the content of the {@code
     * other} byte array.
     */
    public void setRange (int index, byte[] other) {
        assert 0 <= index && index + other.length <= storage.length;
        System.arraycopy(other, 0, storage, index, other.length);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Sets the [index, index + other.size()] range of this byte sequence to the content of
     * {@code other}.
     */
    public void setRange (int index, Bytes other) {
        assert 0 <= index && index + other.size() <= storage.length;
        System.arraycopy(other.storage, 0, storage, index, other.size());
    }

    // ---------------------------------------------------------------------------------------------

    /** Sets the byte at the given index. */
    public void set (int i, int value) {
        assert -256 <= value && value <= 255;
        storage[i] = (byte) value;
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns a {@link Bytes} object for the range [index, index+length[. */
    public Bytes slice (int index, int length) {
        return Bytes.from(arraySlice(index, length));
    }

    // --------------------------------------------------------------------------------------------

    /** Returns a byte array object for the range [index, index+length[. */
    public byte[] arraySlice (int index, int length) {
        return Arrays.copyOfRange(storage, index, index + length);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bytes bytes = (Bytes) o;
        return Arrays.equals(storage, bytes.storage);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(storage);
    }

    /** Displays the bytes as unsigned numbers. */
    @Override public String toString() {
        if (storage.length == 0)
            return "[]";
        var b = new StringBuilder("[");
        for (int i = 0; ; i++) {
            b.append(ByteUtils.uint(storage[i]));
            if (i == storage.length - 1)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    // ---------------------------------------------------------------------------------------------
}
