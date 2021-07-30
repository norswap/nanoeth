package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.data.StorageKey;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * An object that can be {@link #encode() encoded} in RLP format, or {@link RLPEncoding#decode(byte[])
 * decoded} from a byte array in RLP format.
 *
 * <p>This represents either a sequence of sub-items, or a byte array.
 */
public final class RLP {

    // ---------------------------------------------------------------------------------------------

    private final RLP[] items;
    private final byte[] bytes;

    // ---------------------------------------------------------------------------------------------

    private RLP (@Retained RLP[] items, @Retained byte[] bytes) {
        this.items = items;
        this.bytes = bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /** Creates a new RLP object representing the given byte array.*/
    public static RLP bytes (byte... bytes) {
        return new RLP(null, bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /** Creates a new RLP object representing a sequence of the given sub-items. */
    public static RLP sequence (RLP... items) {
        return new RLP(items, null);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a sequence from the given items, automatically translating them according to their
     * types. Supported types are:
     * <ul>
     * <li>{@code byte[]}</li>
     * <li>{@link RLP}</li>
     * <li>{@link Integer} (can pass an {@code int}</li>
     * <li>{@link Natural}</li>
     * <li>{@link Address}</li>
     * <li>{@link Hash}</li>
     * <li>{@link StorageKey}</li>
     * </ul>
     */
    public static RLP sequence (Object... items) {
        var converted = new ArrayList<RLP>();
        for (var item: items) {
            if (item instanceof RLP)
                converted.add((RLP) item);
            else if (item instanceof Integer)
                converted.add(RLP.bytes(ByteUtils.bytesWithoutSign(new Natural((int) item))));
            else if (item instanceof Long)
                converted.add(RLP.bytes(ByteUtils.bytesWithoutSign(new Natural((long) item))));
            else if (item instanceof Natural)
                converted.add(RLP.bytes(ByteUtils.bytesWithoutSign((Natural) item)));
            else if (item instanceof byte[])
                converted.add(RLP.bytes((byte[]) item));
            else if (item instanceof Address)
                converted.add(RLP.bytes(((Address) item).bytes));
            else if (item instanceof Hash)
                converted.add(RLP.bytes(((Hash) item).bytes));
            else if (item instanceof StorageKey)
                converted.add(RLP.bytes(((StorageKey) item).bytes));
            else throw new IllegalArgumentException(
                        "unhandled conversion from type: " + item.getClass());
        }
        return sequence(converted.toArray(new RLP[items.length]));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Decodes the given byte sequence to an {@link RLP} object.
     *
     * @throws IllegalArgumentException if the given byte sequence is not well-formed RLP.
     */
    public static RLP decode (byte[] bytes) {
        return RLPEncoding.decode(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Decodes the given hex string (e.g. 0x123) to an {@link RLP} object.
     *
     * @throws IllegalArgumentException if the given hex string is not well-formed RLP.
     */
    public static RLP decode (String hexString) {
        return decode(ByteUtils.hexStringToBytes(hexString));
    }

    // ---------------------------------------------------------------------------------------------

    /** True iff this object represents a byte array. */
    public boolean isBytes() {
        return bytes != null;
    }

    // ---------------------------------------------------------------------------------------------

    /** True iff this object represents a sequence of sub-items. */
    public boolean isSequence() {
        return items != null;
    }

    // ---------------------------------------------------------------------------------------------

    private void checkBytes() {
        if (bytes == null) throw new IllegalRLPAccess("RLP object does not represent bytes");
    }

    // ---------------------------------------------------------------------------------------------

    private void checkSequence() {
        if (items == null) throw new IllegalRLPAccess("RLP object does not represent a sequence");
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the byte array that this object represents.
     *
     * @throws IllegalRLPAccess if this object does not represent a byte array.
     */
    public byte[] bytes() {
        checkBytes();
        return bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns an array containing the sub-items that this object represents.
     *
     * @throws IllegalRLPAccess if this object does not represent a sequence of sub-items.
     */
    public RLP[] items() {
        checkSequence();
        return items;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the byte at the given index.
     *
     * @throws IllegalRLPAccess if this object does not represent a byte array, or if the index is
     * out of bounds.
     */
    public byte byteAt (int i) {
        checkBytes();
        if (i < 0 || bytes.length <= i)
            throw new IllegalRLPAccess("byte index out of bounds: " + i);
        return bytes[i];
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the sub-item at the given index.
     *
     * @throws IllegalRLPAccess if this object does not represent a sequence of sub-items, or the
     * index is out of bounds.
     */
    public RLP itemAt (int i) {
        checkSequence();
        if (i < 0 || items.length <= i)
            throw new IllegalRLPAccess("sequence index out of bounds: " + i);
        return items[i];
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the binary RLP encoding of this object. */
    public byte[] encode() {
        return bytes == null
            ? RLPEncoding.encode(items)
            : RLPEncoding.encode(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a stream over the sequence of sub-items.
     *
     * @throws IllegalRLPAccess if this object does not represent a sequence of sub-items.
     */
    public Stream<RLP> stream() {
        checkSequence();
        return Arrays.stream(items);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof RLP)) return false;
        var rlp = (RLP) o;
        return Arrays.equals(items, rlp.items) && Arrays.equals(bytes, rlp.bytes);
    }

    @Override public int hashCode () {
        return 31 * Arrays.hashCode(items) + Arrays.hashCode(bytes);
    }

    @Override public String toString () {
        return bytes == null
            ? "RLP (sequence) " + Arrays.toString(items)
            : "RLP (bytes) " + ByteUtils.bytesToHexString(bytes);
    }

    // ---------------------------------------------------------------------------------------------
}
