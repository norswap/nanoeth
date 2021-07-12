package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.data.Bytes;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * Implements the functionality exposed in {@link RLP}.
 * <hr>
 * <h2>Understanding the value of the first byte of an RLP-encoded item
 * <ul>
 * <li>Single byte encoding ([0, 127]): 128 items which are encoded as themselves (single byte)</li>
 * <li>Direct bytes size encoding ([128, 183]): 56 items representing the size of a byte sequence in
 * the [0, 55] range</li>
 * <li>Indirect bytes size encoding ([184, 191]): 8 items representing the size of a byte sequence
 * in the [1, 8] range, encoding other byte sequences whose size is in the [55, 2^64[ range</li>
 * <li>Direct items size encoding ([192, 247]): 56 items representing the serialized size of an item
 * sequence in the [0, 55] range</li>
 * <li>Indirect items size encoding ([248, 255]): 8 items representing the size of a byte sequence
 * in the [1, 8] range, encoding item sequences whose serialized size is in the [55, 2^64]
 * range</li>
 * </ul>
 *
 * <p>Note that the "serialized size" of an item sequence matches s(x) in the yellowpaper. It is the
 * sum of the RLP-encoded size of every item in the sequence, and hence exclude the space needed to
 * encode the size of the item sequence itself.
 *
 * <p>The values 128, 184, 192 and 248 mark boundaries on the value of the first byte, indicating a
 * change in encoding. We repreent them in the implementation as constants whose name ends with
 * {@code LIMIT}.</p>
 *
 * <p>The values 128, 183, 192 and 247 (note the occasional off-by-one) are similarly special, as
 * they represent a base that is added to a size to encode it. We represent them in the
 * implementation as constants whose name ends with {@code SUMMAND}.
 */
class RLPImplem
{
    // ---------------------------------------------------------------------------------------------

    private RLPImplem () {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Seems to be a safe value for maximum Java array size.
     * (cf. https://stackoverflow.com/questions/3038392)
     *
     * <p>Also acts as a marker for where the spec would handle array up to its maximum allowed
     * size of 2^64.
     */
    private final static int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The maximum (serializable) size of byte and item sequences whose size is encoded in a single
     * byte.
     */
    private final static int MAX_SHORT_SEQUENCE_SIZE = 55;

    /**
     * The maximum size of byte sequences used to encode the size of another byte or item sequence.
     */
    private final static int MAX_SIZE_ARRAY_SIZE = 8;

    // For the following values, check the top comment.

    private final static int BYTES_SIZE_SUMMAND = 128;
    private final static int BYTES_SIZE_SIZE_SUMMAND = 183;
    private final static int ITEMS_SIZE_SUMMAND = 192;
    private final static int ITEMS_SIZE_SIZE_SUMMAND = 247;

    private final static int SINGLE_BYTE_ENCODING_LIMIT = 128;
    private final static int DIRECT_BYTES_SIZE_ENCODING_LIMIT = 184;
    private final static int INDIRECT_BYTES_SIZE_ENCODING_LIMIT = 192;
    private final static int DIRECT_ITEMS_SIZE_ENCODING_LIMIT = 248;

    // ---------------------------------------------------------------------------------------------
    // region ENCODING
    // ---------------------------------------------------------------------------------------------

    private static byte[] encodeByteSequenceSize (int size) {
        if (size <= MAX_SHORT_SEQUENCE_SIZE)
            return ByteUtils.array(BYTES_SIZE_SUMMAND + size);
        else
            return ByteUtils.concat(
                ByteUtils.array(BYTES_SIZE_SIZE_SUMMAND + ByteUtils.byteSize(size)),
                ByteUtils.bytes(size));
    }

    // ---------------------------------------------------------------------------------------------

    private static byte[] encodeItemSequenceSize (int size) {
        if (size <= MAX_SHORT_SEQUENCE_SIZE)
            return ByteUtils.array(ITEMS_SIZE_SUMMAND + size);
        else
            return ByteUtils.concat(
                ByteUtils.array(ITEMS_SIZE_SIZE_SUMMAND + ByteUtils.byteSize(size)),
                ByteUtils.bytes(size));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Encodes a byte sequence in RLP format.
     */
    static Bytes encode (Bytes bytes) {
        assert bytes.frozen();

        if (bytes.size() == 1 && ByteUtils.uint(bytes.get(0)) < SINGLE_BYTE_ENCODING_LIMIT)
            return bytes;

        int size = bytes.size();
        byte[] encodedSize = encodeByteSequenceSize(size);
        int sizeSize = encodedSize.length;
        Bytes out = Bytes.ofSize(sizeSize + size);
        out.setRange(0, encodedSize);
        out.setRange(encodedSize.length, bytes);
        return out.freeze();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Encodes a sequence of items (byte arrays or nested sequences) to RLP format.
     */
    static Bytes encode (RLPSequence sequence) {
        Bytes[] encodedItems = sequence.stream()
            .map(RLPItem::encode)
            .toArray(Bytes[]::new);
        int serializedSize = Arrays.stream(encodedItems)
            .map(Bytes::size)
            .reduce(0, Integer::sum);
        byte[] encodedSize = encodeItemSequenceSize(serializedSize);
        int sizeSize = encodedSize.length;
        Bytes out = Bytes.ofSize(sizeSize + serializedSize);
        out.setRange(0, encodedSize);
        int pos = sizeSize;
        for (Bytes item: encodedItems) {
            out.setRange(pos, item);
            pos += item.size();
        }
        return out.freeze();
    }

    // endregion
    // ---------------------------------------------------------------------------------------------
    // region DECODING
    // ---------------------------------------------------------------------------------------------

    private static final class Offset {
        int x;
    }

    // ---------------------------------------------------------------------------------------------

    /** Implements {@link RLP#decode(Bytes)}. */
    static RLPItem decode (Bytes bytes) {
        var offset = new Offset();
        var out = decode(bytes, offset);
        int left = bytes.size() - offset.x;
        assert left >= 0;
        if (left > 0)
            throw new IllegalArgumentException(left + " bytes left at the end of decoded array.");
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    private static boolean isByteSequence (int marker) {
        return marker < INDIRECT_BYTES_SIZE_ENCODING_LIMIT;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * @throws IllegalArgumentException if {@code bytes.length < offset + amount}
     */
    private static void checkRemaining (Bytes bytes, int offset, int amount) {
        if (bytes.size() < offset)
            throw new IllegalArgumentException(format(
                "Trying to fetch %d bytes at offset %d but size is only %d.",
                amount, offset, bytes.size()));
        if (bytes.size() < offset + amount)
            throw new IllegalArgumentException(format(
                "Trying to fetch %d bytes at offset %d but only %d bytes are available.",
                amount, offset, bytes.size() - offset));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Converts the big-endian {@code bytes} sequence to an integer, verifying it satisfies the
     * constraint the Java (and the model) put on array sizes.
     */
    private static int toInt (Bytes bytes, int offset, int size) {
        checkRemaining(bytes, offset, size);
        if (size <= 4) {
            int out = ByteUtils.toInt(bytes.arraySlice(offset, size));
            if (out <= MAX_ARRAY_SIZE)
                return out;
        }
        throw new IllegalArgumentException(format(
            "The model only supports arrays of size up to %d elements.", MAX_ARRAY_SIZE));
    }

    // ---------------------------------------------------------------------------------------------

    private static int decodeByteSequenceSize (int marker, Bytes bytes, Offset offset) {
        if (marker < DIRECT_BYTES_SIZE_ENCODING_LIMIT)
            return marker - BYTES_SIZE_SUMMAND;

        assert marker < INDIRECT_BYTES_SIZE_ENCODING_LIMIT;
        int sizeSize = marker - BYTES_SIZE_SIZE_SUMMAND;
        int size = toInt(bytes, offset.x, sizeSize);
        offset.x += sizeSize;
        return size;
    }

    // ---------------------------------------------------------------------------------------------

    private static int decodeItemSequenceSize (int marker, Bytes bytes, Offset offset) {
        if (marker < DIRECT_ITEMS_SIZE_ENCODING_LIMIT)
            return marker - ITEMS_SIZE_SUMMAND;

        // indirect items encoding
        int sizeSize = marker - ITEMS_SIZE_SIZE_SUMMAND;
        int size = toInt(bytes, offset.x, sizeSize);
        offset.x += sizeSize;
        return size;
    }

    // ---------------------------------------------------------------------------------------------

    private static RLPItem decode (Bytes bytes, Offset offset) {
        int marker = ByteUtils.uint(bytes.get(offset.x++));
        if (isByteSequence(marker)) {
            if (marker < BYTES_SIZE_SUMMAND)
                return RLPBytes.from((byte) marker);
            int size = decodeByteSequenceSize(marker, bytes, offset);
            checkRemaining(bytes, offset.x, size);
            var out = RLPBytes.from(bytes.slice(offset.x, size));
            offset.x += size;
            return out;
        } else {
            int size = decodeItemSequenceSize(marker, bytes, offset);
            var list = new ArrayList<RLPItem>();
            int end = offset.x + size;
            while (offset.x < end)
                list.add(decode(bytes, offset));
            if (offset.x != end)
                throw new IllegalArgumentException("End offset of last item did not match sequence size.");
            return RLPSequence.from(list.toArray(RLPItem[]::new));
        }
    }

    // endregion
    // ---------------------------------------------------------------------------------------------
}
