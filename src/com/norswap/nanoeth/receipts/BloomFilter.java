package com.norswap.nanoeth.receipts;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.blocks.BlockHeader;
import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPLayoutable;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.utils.Hashing;
import java.util.Arrays;

/**
 * A bloom filter associated with one or multiple {@link LogEntry}.
 *
 * <p>In practice, such a bloom filter is constructed from all the log entries in a block,
 * and included in the block header as {@link BlockHeader#logsBloom}.
 *
 * <p>See {@link LogTopic} for an explanation of the usefulness of bloom filters.
 */
@Wrapper
public final class BloomFilter implements RLPLayoutable {

    // ---------------------------------------------------------------------------------------------

    /** The bytes making up the hash. */
    public final byte[] bits;

    // ---------------------------------------------------------------------------------------------

    /** Creates a new empty bloom filter. */
    public BloomFilter() {
        this.bits = new byte[256];
    }

    // ---------------------------------------------------------------------------------------------

    public BloomFilter (@Retained byte[] bits) {
        Assert.that(bits.length == 256, "bloom filter is not 256 bytes (2048 bits) long");
        this.bits = bits;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a bloom filter from a hex string (e.g. 0x123).
     *
     * <p>If the post-0x part of the hex string is not 512 characters long, the hash will be padded
     * with zeroes at the start so that it is 256 bytes long.
     */
    public BloomFilter (String hexString) {
        this(ByteUtils.hexStringToBytes(hexString, 256));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Indicates if some of the log entries associated to this bloom filter <b>may</b> contain the
     * given topic.
     *
     * <p>This can return true if the no log entries contains the topic, but may not return
     * false if the some log entry contains the topic.
     */
    public boolean mayContain (LogTopic topic) {
        return mayContain(topic.bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Indicates if the log entries associated to this bloom filter <b>may</b> have been been logged
     * by the given address.
     *
     * <p>This can return true if no log entry was logged by the address, but may not return
     * false if some log entry was logged by the address.
     */
    public boolean mayContain (Address address) {
        return mayContain(address.bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /** Implements lookup based on the M3:2048 function from the yellowpaper. */
    private boolean mayContain (byte[] value) {
        byte[] hash = Hashing.keccak(value).bytes;
        int v0 = ByteUtils.toInt(hash[0], hash[1]) % 2048; // 11 first bits of fst pair of bytes
        int v2 = ByteUtils.toInt(hash[2], hash[3]) % 2048; // 11 first bits of snd pair of bytes
        int v4 = ByteUtils.toInt(hash[4], hash[5]) % 2048; // 11 first bits of trd pair of bytes
        return (bits[v0 / 8] & (1 << (v0 % 8))) != 0 &&
               (bits[v2 / 8] & (1 << (v2 % 8))) != 0 &&
               (bits[v4 / 8] & (1 << (v4 % 8))) != 0 ;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Implements the M3:2048 function from the yellowpaper, but adds the result directly
     * into {@code bits}.
     */
    void add (byte[] value) {
        byte[] hash = Hashing.keccak(value).bytes;
        int v0 = ByteUtils.toInt(hash[0], hash[1]) % 2048; // 11 first bits of fst pair of bytes
        int v2 = ByteUtils.toInt(hash[2], hash[3]) % 2048; // 11 first bits of snd pair of bytes
        int v4 = ByteUtils.toInt(hash[4], hash[5]) % 2048; // 11 first bits of trd pair of bytes
        bits[v0 / 8] |= 1 << (v0 % 8);
        bits[v2 / 8] |= 1 << (v2 % 8);
        bits[v4 / 8] |= 1 << (v4 % 8);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * The hex-string representation of this hash, including leading 0 if any, as per {@link
     * ByteUtils#toFullHexString(byte[])}.
     */
    public String toFullHexString() {
        return ByteUtils.toFullHexString(bits);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public RLP rlpLayout() {
        return RLP.bytes(bits);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof BloomFilter && Arrays.equals(bits, ((BloomFilter) o).bits);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(bits);
    }

    @Override public String toString() {
        return toFullHexString();
    }

    // ---------------------------------------------------------------------------------------------
}
