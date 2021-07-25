package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.utils.ByteUtils;
import java.util.ArrayList;
import java.util.Arrays;

import static java.lang.String.format;

/**
 * Enables encoding and decoding from RLP format, as specified in appendix B of the yellowpaper.
 *
 * <p>This implementation only supports byte sequences and item sequences with the maximum length
 * allowed by Java, which is slightly under 2^31. Ethereum allows byte and item sequences of length
 * up to 2^64.
 */
public final class RLP {
    private RLP () {}

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
     * Encodes the given item to a byte sequence in accordance to the RLP format.
     *
     * @see RLPItem#encode()
     */
    public static byte[] encode (RLPItem item) {
        // dispatches between #encode(byte[]) and #encode(RLPSequence)
        return item.encode();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Encodes a byte sequence in RLP format.
     */
    static byte[] encode (byte[] bytes) {

        if (bytes.length == 1 && ByteUtils.uint(bytes[0]) < SINGLE_BYTE_ENCODING_LIMIT)
            return bytes;

        int size = bytes.length;
        byte[] encodedSize = encodeByteSequenceSize(size);
        int sizeSize = encodedSize.length;
        byte[] out = new byte[sizeSize + size];
        ByteUtils.setRangeAt(out, 0, encodedSize);
        ByteUtils.setRangeAt(out, encodedSize.length, bytes);
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Encodes a sequence of items (byte arrays or nested sequences) to RLP format.
     */
    static byte[] encode (RLPSequence sequence) {
        byte[][] encodedItems = sequence.stream()
            .map(RLPItem::encode)
            .toArray(byte[][]::new);
        int serializedSize = Arrays.stream(encodedItems)
            .map(it -> it.length)
            .reduce(0, Integer::sum);
        byte[] encodedSize = encodeItemSequenceSize(serializedSize);
        int sizeSize = encodedSize.length;
        byte[] out = new byte[sizeSize + serializedSize];
        ByteUtils.setRangeAt(out, 0, encodedSize);
        int pos = sizeSize;
        for (byte[] item: encodedItems) {
            ByteUtils.setRangeAt(out, pos, item);
            pos += item.length;
        }
        return out;
    }

    // endregion
    // ---------------------------------------------------------------------------------------------
    // region DECODING
    // ---------------------------------------------------------------------------------------------

    private static final class Offset {
        int x;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Decodes the given byte sequence to an {@link RLPItem}.
     *
     * @throws IllegalArgumentException if the given byte sequence is not well-formed RLP.
     */
    public static RLPItem decode (byte[] bytes) {
        var offset = new Offset();
        var out = decode(bytes, offset);
        int left = bytes.length - offset.x;
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
    private static void checkRemaining (byte[] bytes, int offset, int amount) {
        if (bytes.length < offset)
            throw new IllegalArgumentException(format(
                "Trying to fetch %d bytes at offset %d but size is only %d.",
                amount, offset, bytes.length));
        if (bytes.length < offset + amount)
            throw new IllegalArgumentException(format(
                "Trying to fetch %d bytes at offset %d but only %d bytes are available.",
                amount, offset, bytes.length - offset));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Converts the big-endian {@code bytes} sequence to an integer, verifying it satisfies the
     * constraint the Java (and the model) put on array sizes.
     */
    private static int toInt (byte[] bytes, int offset, int size) {
        checkRemaining(bytes, offset, size);
        if (size <= 4) {
            int out = ByteUtils.toInt(ByteUtils.copyOfSizedRange(bytes, offset, size));
            if (out <= MAX_ARRAY_SIZE)
                return out;
        }
        throw new IllegalArgumentException(format(
            "The model only supports arrays of size up to %d elements.", MAX_ARRAY_SIZE));
    }

    // ---------------------------------------------------------------------------------------------

    private static int decodeByteSequenceSize (int marker, byte[] bytes, Offset offset) {
        if (marker < DIRECT_BYTES_SIZE_ENCODING_LIMIT)
            return marker - BYTES_SIZE_SUMMAND;

        assert marker < INDIRECT_BYTES_SIZE_ENCODING_LIMIT;
        int sizeSize = marker - BYTES_SIZE_SIZE_SUMMAND;
        int size = toInt(bytes, offset.x, sizeSize);
        offset.x += sizeSize;
        return size;
    }

    // ---------------------------------------------------------------------------------------------

    private static int decodeItemSequenceSize (int marker, byte[] bytes, Offset offset) {
        if (marker < DIRECT_ITEMS_SIZE_ENCODING_LIMIT)
            return marker - ITEMS_SIZE_SUMMAND;

        // indirect items encoding
        int sizeSize = marker - ITEMS_SIZE_SIZE_SUMMAND;
        int size = toInt(bytes, offset.x, sizeSize);
        offset.x += sizeSize;
        return size;
    }

    // ---------------------------------------------------------------------------------------------

    private static RLPItem decode (byte[] bytes, Offset offset) {
        int marker = ByteUtils.uint(bytes[offset.x++]);
        if (isByteSequence(marker)) {
            if (marker < BYTES_SIZE_SUMMAND)
                return new RLPBytes((byte) marker);
            int size = decodeByteSequenceSize(marker, bytes, offset);
            checkRemaining(bytes, offset.x, size);
            var out = new RLPBytes(ByteUtils.copyOfSizedRange(bytes, offset.x, size));
            offset.x += size;
            return out;
        } else {
            int size = decodeItemSequenceSize(marker, bytes, offset);
            var list = new ArrayList<RLPItem>();
            int end = offset.x + size;
            checkRemaining(bytes, offset.x, end - offset.x);
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
