package com.norswap.nanoeth.receipts;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.blocks.BlockHeader;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;

/**
 * Represents a 256-bit (32 bytes) log topic.
 *
 * <p>The purpose of log topics is to be a marker for certain type of events (in fact they map with
 * Solidity events, see https://ethereum.stackexchange.com/a/12951 for more info).
 *
 * <p>Additionally, they are automatically entered into the {@link BlockHeader#logsBloom bloom
 * filter} that is included in each block header. If a client wants to process all logs with a
 * certain topic, this will help him skip the blocks that do not contain the topic, and hence lower
 * network congestion as he will not have to request the logs over the network. (This is not useful
 * if you're validating the network, in which case you have to derive the logs to validate the
 * header anyway.)
 */
@Wrapper
public final class LogTopic {

    // NOTE: This is essentially a copy/paste of the data.Hash implementation.

    // ---------------------------------------------------------------------------------------------

    /** The bytes making up the hash. */
    public final byte[] bytes;

    // ---------------------------------------------------------------------------------------------

    public LogTopic (@Retained byte[] bytes) {
        Assert.that(bytes.length == 32, "log topic is not 32 bytes long");
        this.bytes = bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a log topic from a hex string (e.g. 0x123).
     *
     * <p>If the post-0x part of the hex string is not 64 characters long, the hash will be padded
     * with zeroes at the start so that it is 32 bytes long.
     */
    public LogTopic (String hexString) {
        this(ByteUtils.hexStringToBytes(hexString, 32));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * The hex-string representation of this hash, including leading 0 if any, as per {@link
     * ByteUtils#toFullHexString(byte[])}.
     */
    public String toFullHexString() {
        return ByteUtils.toFullHexString(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof LogTopic && Arrays.equals(bytes, ((LogTopic) o).bytes);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(bytes);
    }

    @Override public String toString() {
        return toFullHexString();
    }

    // ---------------------------------------------------------------------------------------------
}
