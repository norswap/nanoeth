package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.utils.Hashing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * An object that can be {@link #encode() encoded} in RLP format, or {@link RLPEncoding#decode(byte[])
 * decoded} from a byte array in RLP format.
 * <p>
 * This represents either a sequence of sub-items, a byte array, or a binary-encoded RLP item
 * (either a sequence or byte array).
 * <p>
 * We allow representing already-encoded items in order to enable incremental RLP encoding (see the
 * README of this package for more information).
 */
public final class RLP implements RLPLayoutable {

    // ---------------------------------------------------------------------------------------------

    /** RLP-encoding of an empty RLP byte array, which is sometimes used to represent absent values. */
    public static final byte[] RLP_EMPTY_BYTE_ARRAY = RLP.bytes(new byte[0]).encode();

    // ---------------------------------------------------------------------------------------------

    /** Does the given RLP encoding encode a byte array? */
    public static boolean encodesBytes (byte[] encoding) {
        return RLPEncoding.isByteSequence(ByteUtils.uint(encoding[0]));
    }

    // ---------------------------------------------------------------------------------------------

    /** Does the given RLP encoding encode an item sequence? */
    public static boolean encodesSequence (byte[] encoding) {
        return !encodesBytes(encoding);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * If the given encoding encodes a byte array, returns that byte array, otherwise return
     * the encoding.
     */
    public static byte[] unwrap (byte[] encoding) {
        return encodesBytes(encoding)
            ? RLP.decode(encoding).bytes
            : encoding;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Marker stored in {@link #items} to signify that {@link #bytes} represents an already-encoded
     * RLP item and not a RLP byte array.
     * <p>
     * Using this marker avoids the space overhead of adding a third field to this class.
     */
    private static final RLP[] ENCODED_MARKER = new RLP[0];

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
     * Creates a new RLP item holding an already-encoded RLP item (either a sequence or a byte
     * array). Use {@link RLP#encode()} to access the encoding, and {@link #inflate()} to retrieve
     * the original layout.
     */
    public static RLP encoded (byte[] encoding) {
        // The condition is bogus, the point of this assertion is to cause an early exception if
        // the RLP encoding is not valid.
        assert RLP.decode(encoding) != null;
        return new RLP(ENCODED_MARKER, encoding);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a sequence from the given items, automatically translating them according to their
     * types. Supported types are:
     * <ul>
     * <li>{@link RLPLayoutable} (including {@link RLP} itself)</li>
     * <li>{@link Byte} (can pass a {@code byte})</li>
     * <li>{@link Integer} (encoded on 4 bytes, can pass an {@code int}</li>
     * <li>{@link Long} (encoded on 8 bytes, can pass a {@code long}</li>
     * <li>{@code byte[]}</li>
     * <li>{@code Object[]}</li>
     * </ul>
     */
    public static RLP sequence (Object... items) {
        var converted = new ArrayList<RLP>();
        for (var item: items) {
            if (item instanceof RLPLayoutable)
                converted.add(((RLPLayoutable) item).rlpLayout());
            else if (item instanceof Byte)
                converted.add(RLP.bytes(ByteUtils.bytesWithoutSign(new Natural((byte) item))));
            else if (item instanceof Integer)
                converted.add(RLP.bytes(ByteUtils.bytesPadded(new Natural((int) item), 4)));
            else if (item instanceof Long)
                converted.add(RLP.bytes(ByteUtils.bytesPadded(new Natural((long) item), 8)));
            else if (item instanceof byte[])
                converted.add(RLP.bytes((byte[]) item));
            else if (item instanceof Object[])
                converted.add(RLP.sequence((Object[]) item)); // cast is crucial here!
            else throw new IllegalArgumentException(
                    "unhandled conversion from type: " + item.getClass());
        }
        return sequence(converted.toArray(new RLP[items.length]));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Decodes the given byte sequence to an {@link RLP} layout.
     *
     * @throws IllegalArgumentException if the given byte sequence is not well-formed RLP.
     */
    public static RLP decode (byte[] bytes) {
        return RLPEncoding.decode(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Decodes the given hex string (e.g. 0x123) to an {@link RLP} layout.
     *
     * @throws IllegalArgumentException if the given hex string is not well-formed RLP.
     */
    public static RLP decode (String hexString) {
        return decode(ByteUtils.hexStringToBytes(hexString));
    }

    // ---------------------------------------------------------------------------------------------

    /** True iff this object represents a byte array. */
    public boolean isBytes() {
        return bytes != null && items == null;
    }

    // ---------------------------------------------------------------------------------------------

    /** True iff this object represents a sequence of sub-items. */
    public boolean isSequence() {
        return items != null && items != ENCODED_MARKER;
    }

    // ---------------------------------------------------------------------------------------------

    /** True iff this object represents an already-encoded RLP item. */
    public boolean isEncoded () {
        return items == ENCODED_MARKER;
    }

    // ---------------------------------------------------------------------------------------------

    private void checkBytes() {
        if (!isBytes()) throw new IllegalRLPAccess("RLP object does not represent bytes");
    }

    // ---------------------------------------------------------------------------------------------

    private void checkSequence() {
        if (!isSequence()) throw new IllegalRLPAccess("RLP object does not represent a sequence");
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
        return items == ENCODED_MARKER
            ? bytes
            : bytes == null
                ? RLPEncoding.encode(items)
                : RLPEncoding.encode(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * If this object is an encoded RLP item, returns a non-encoded version, otherwise returns
     * this object.
     */
    public RLP inflate() {
        return items == ENCODED_MARKER
            ? RLPEncoding.decode(bytes)
            : this;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the {@link Hashing#keccak Keccak} hash of the the {@link #encode() encoding} of this
     * object.
     */
    public Hash hash() {
        return Hashing.keccak(encode());
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

    /**
     * The hex-string representation of the binary encoding of this RLP item, including leading 0 if
     * any, as per {@link ByteUtils#toFullHexString(byte[])}.
     */
    public String toHexString() {
        return ByteUtils.toFullHexString(encode());
    }

    // ---------------------------------------------------------------------------------------------

    @Override public RLP rlpLayout() {
        return this;
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
        return items == ENCODED_MARKER
            ? "RLP (encoded)" + ByteUtils.toCompressedHexString(bytes)
            : bytes == null
                ? "RLP (sequence) " + Arrays.toString(items)
                : "RLP (bytes) " + ByteUtils.toCompressedHexString(bytes);
    }

    // ---------------------------------------------------------------------------------------------
}
