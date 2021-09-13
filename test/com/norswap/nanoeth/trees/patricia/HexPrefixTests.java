package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.trees.patricia.Nibbles;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.norswap.nanoeth.utils.ByteUtils.hexStringToBytes;
import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;

public class HexPrefixTests {
    // ---------------------------------------------------------------------------------------------

    // layout:
    // - number of leading nibbles to ignore
    // - number of trailing nibble to ignore
    // - input bytes
    // - hex-prefix result
    private static final Object[][] examples = new Object[][] {
        { 1, 0, "0x07",     "0x17"},         // 1 (odd)  nibbles, aligned
        { 0, 0, "0x77",     "0x0077" },      // 2 (even) nibbles, aligned
        { 1, 0, "0x0777",   "0x1777"},       // 3 (odd)  nibbles, aligned
        { 0, 0, "0x7777",   "0x007777" },    // 4 (even) nibbles, aligned

        { 0, 1, "0x70",     "0x17"},         // 1 (odd)  nibbles, unaligned
        { 1, 1, "0x0770",   "0x0077" },      // 2 (even) nibbles, unaligned
        { 0, 1, "0x7770",   "0x1777"},       // 3 (odd)  nibbles, unaligned
        { 1, 1, "0x077770", "0x007777" },    // 4 (even) nibbles, unaligned

        // same as tests 1-4, adding shifts
        { 2, 1, "0x0070",     "0x17"},
        { 1, 1, "0x0770",     "0x0077" },
        { 2, 1, "0x007770",   "0x1777"},
        { 1, 1, "0x077770",   "0x007777" },

        // same as tests 5-8, adding shifts
        { 1, 2, "0x0700",     "0x17"},
        { 2, 2, "0x007700",   "0x0077" },
        { 1, 2, "0x077700",   "0x1777"},
        { 2, 2, "0x00777700", "0x007777" }
    };

    @DataProvider public static Object[][] examples () {
        return examples;
    }

    // ---------------------------------------------------------------------------------------------

    @Test(dataProvider = "examples")
    public void testNonLeaf (int ignoreStart, int ignoreEnd, String nibblesString, String expected) {
        var bytes = hexStringToBytes(nibblesString);
        var nibbles = new Nibbles(bytes, ignoreStart, bytes.length * 2 - ignoreEnd);
        var actual = toFullHexString(nibbles.hexPrefix(false));
        Assert.assertEquals(actual, expected);
    }

    // ---------------------------------------------------------------------------------------------

    @Test(dataProvider = "examples")
    public void testLeaf (int ignoreStart, int ignoreEnd, String nibblesString, String expected) {
        var bytes = hexStringToBytes(nibblesString);
        var nibbles = new Nibbles(bytes, ignoreStart, bytes.length * 2 - ignoreEnd);
        var actual = toFullHexString(nibbles.hexPrefix(true)); // this is true now
        var expectedBytes = hexStringToBytes(expected);
        expectedBytes[0] |= 0x20; // set leaf flag
        expected = toFullHexString(expectedBytes);
        Assert.assertEquals(actual, expected);
    }

    // ---------------------------------------------------------------------------------------------
}
