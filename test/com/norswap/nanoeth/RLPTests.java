package com.norswap.nanoeth;

import com.norswap.nanoeth.data.Bytes;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPBytes;
import com.norswap.nanoeth.rlp.RLPSequence;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static com.norswap.nanoeth.data.ByteUtils.array;
import static com.norswap.nanoeth.data.ByteUtils.concat;
import static org.testng.Assert.assertEquals;

public final class RLPTests {

    // ---------------------------------------------------------------------------------------------

    private static byte[] encodeBytesSize (int size) {
        if (size <= 55)
            return array(128 + size);
        if (size <= 255)
            return array(183 + 1, size);
        if (size < 256 * 256)
            return array(183 + 2, size / 256, size % 256);
        else
            throw new Error("not supported in tests");
    }

    // ---------------------------------------------------------------------------------------------

    private static byte[] encodeSequenceSize (int size) {
        if (size <= 55)
            return array(192 + size);
        if (size <= 255)
            return array(247 + 1, size);
        if (size < 256 * 256)
            return array(247 + 2, size / 256, size % 256);
        else
            throw new Error("not supported in tests");
    }

    // ---------------------------------------------------------------------------------------------

    private static byte[] decodedBytes (int size) {
        byte[] bytes = new byte[size];
        IntStream.range(0, size).forEach(i -> bytes[i] = (byte)(i + 1));
        return bytes;
    }

    // ---------------------------------------------------------------------------------------------

    private static byte[] encodedBytes (int size) {
        return concat(encodeBytesSize(size), decodedBytes(size));
    }

    // ---------------------------------------------------------------------------------------------

    private static final byte[] SINGLE_0       = array(0);
    private static final byte[] SINGLE_127     = array(127);
    private static final byte[] SINGLE_128     = array(128);
    private static final byte[] SINGLE_255     = array(255);
    private static final byte[] DIRECT_0       = decodedBytes(0);
    private static final byte[] DIRECT_2       = decodedBytes(2);
    private static final byte[] DIRECT_55      = decodedBytes(55);
    private static final byte[] INDIRECT_56    = decodedBytes(56);
    private static final byte[] INDIRECT_255   = decodedBytes(255);
    private static final byte[] INDIRECT_256   = decodedBytes(256);
    private static final byte[] INDIRECT_500   = decodedBytes(500);

    private static final byte[] E_SINGLE_0     = array(0);
    private static final byte[] E_SINGLE_127   = array(127);
    private static final byte[] E_SINGLE_128   = array(128 + 1, 128);
    private static final byte[] E_SINGLE_255   = array(128 + 1, 255);
    private static final byte[] E_DIRECT_0     = encodedBytes(0);
    private static final byte[] E_DIRECT_2     = encodedBytes(2);
    private static final byte[] E_DIRECT_55    = encodedBytes(55);
    private static final byte[] E_INDIRECT_56  = encodedBytes(56);
    private static final byte[] E_INDIRECT_255 = encodedBytes(255);
    private static final byte[] E_INDIRECT_256 = encodedBytes(256);
    private static final byte[] E_INDIRECT_500 = encodedBytes(500);

    private static final byte[][] BYTES_DECODED = new byte[][]{
        SINGLE_0, SINGLE_127, SINGLE_128, SINGLE_255, DIRECT_0, DIRECT_2, DIRECT_55,
        INDIRECT_56, INDIRECT_255, INDIRECT_256, INDIRECT_500};

    private static final byte[][] BYTES_ENCODED = new byte[][]{
        E_SINGLE_0, E_SINGLE_127, E_SINGLE_128, E_SINGLE_255, E_DIRECT_0, E_DIRECT_2, E_DIRECT_55,
        E_INDIRECT_56, E_INDIRECT_255, E_INDIRECT_256, E_INDIRECT_500};

    // ---------------------------------------------------------------------------------------------

    private void assertDecodeEncodeBytes (byte[] encoded, byte[] decoded) {
        var decodedBytes = RLPBytes.from(decoded);
        var encodedBytes = Bytes.from(encoded);
        assertEquals(RLP.decode(encodedBytes), decodedBytes);
        assertEquals(RLP.encode(decodedBytes), encodedBytes);
    }

    @Test public void testEncodeDecodeBytes() {
        assertDecodeEncodeBytes(SINGLE_0,       SINGLE_0);
        assertDecodeEncodeBytes(SINGLE_127,     SINGLE_127);
        assertDecodeEncodeBytes(E_SINGLE_128,   SINGLE_128);
        assertDecodeEncodeBytes(E_SINGLE_255,   SINGLE_255);
        assertDecodeEncodeBytes(E_DIRECT_0,     DIRECT_0);
        assertDecodeEncodeBytes(E_DIRECT_2,     DIRECT_2);
        assertDecodeEncodeBytes(E_DIRECT_55,    DIRECT_55);
        assertDecodeEncodeBytes(E_INDIRECT_56,  INDIRECT_56);
        assertDecodeEncodeBytes(E_INDIRECT_255, INDIRECT_255);
        assertDecodeEncodeBytes(E_INDIRECT_256, INDIRECT_256);
        assertDecodeEncodeBytes(E_INDIRECT_500, INDIRECT_500);
    }

    // ---------------------------------------------------------------------------------------------

    private byte[] encodeSequence(byte[]... bytes) {
        int size = Arrays.stream(bytes).map(d -> d.length).reduce(0, Integer::sum);
        return concat(encodeSequenceSize(size), concat(bytes));
    }

    private void assertEncodeDecodeSequence(byte[] decoded, byte[] encoded) {
        var seq = RLPSequence.from(RLPBytes.from(decoded));
        assertEncodeDecodeSequence(seq, concat(encodeSequenceSize(encoded.length), encoded));
    }

    private void assertEncodeDecodeSequence(RLPSequence seq, byte[] encoded) {
        var bytes = Bytes.from(encoded);
        assertEquals(seq.encode(), bytes);
        assertEquals(RLP.decode(bytes), seq);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testEncodeDecodeSingleItemSequences() {
        assertEncodeDecodeSequence(SINGLE_0,     E_SINGLE_0);
        assertEncodeDecodeSequence(SINGLE_127,   E_SINGLE_127);
        assertEncodeDecodeSequence(SINGLE_128,   E_SINGLE_128);
        assertEncodeDecodeSequence(SINGLE_255,   E_SINGLE_255);
        assertEncodeDecodeSequence(DIRECT_0,     E_DIRECT_0);
        assertEncodeDecodeSequence(DIRECT_2,     E_DIRECT_2);
        assertEncodeDecodeSequence(DIRECT_55,    E_DIRECT_55);
        assertEncodeDecodeSequence(INDIRECT_56,  E_INDIRECT_56);
        assertEncodeDecodeSequence(INDIRECT_255, E_INDIRECT_255);
        assertEncodeDecodeSequence(INDIRECT_500, E_INDIRECT_500);
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testEncodeDecodeTwoItemSequences() {
        for (int i = 0; i < BYTES_DECODED.length - 1; ++i) {
            var seq = RLPSequence.from(
                RLPBytes.from(BYTES_DECODED[i]),
                RLPBytes.from(BYTES_DECODED[i+1]));
            var encoded = encodeSequence(BYTES_ENCODED[i], BYTES_ENCODED[i + 1]);
            assertEncodeDecodeSequence(seq, encoded);
        }
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testEncodeDecodeNestedSequences() {
        for (int i = 0; i < BYTES_DECODED.length - 3; ++i) {
            var seq = RLPSequence.from(
                RLPSequence.from(
                    RLPBytes.from(BYTES_DECODED[i]),
                    RLPBytes.from(BYTES_DECODED[i + 1])),
                RLPSequence.from(
                    RLPBytes.from(BYTES_DECODED[i + 2]),
                    RLPBytes.from(BYTES_DECODED[i + 3])));
            var encoded = encodeSequence(
                encodeSequence(
                    BYTES_ENCODED[i],
                    BYTES_ENCODED[i + 1]),
                encodeSequence(
                    BYTES_ENCODED[i + 2],
                    BYTES_ENCODED[i + 3]));
            assertEncodeDecodeSequence(seq, encoded);
        }
    }

    // ---------------------------------------------------------------------------------------------
}
