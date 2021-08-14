package com.norswap.nanoeth.receipts;

import com.norswap.nanoeth.blocks.BlockHeader;
import com.norswap.nanoeth.data.Address;

/**
 * A log entry generated during contract execution.
 */
public final class LogEntry {

    // ---------------------------------------------------------------------------------------------

    /** Address that generated this log. */
    public final Address logger;

    // ---------------------------------------------------------------------------------------------

    /** Topics for this entry. */
    public final LogTopic[] topics;

    // ---------------------------------------------------------------------------------------------

    /** Arbitrary data attached to the entry. */
    public final byte[] data;

    // ---------------------------------------------------------------------------------------------

    public LogEntry (Address logger, LogTopic[] topics, byte[] data) {
        this.logger = logger;
        this.topics = topics;
        this.data = data;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Adds the address and the topics for this entry into the given log filter.
     *
     * <p>This implements the M(O) function from the yellowpaper excepted that the output of the
     * function is immediately combined with the given bloom filter. This target bloom filter is
     * most likely a combination of many individual log entry bloom filters, to be used as {@link
     * BlockHeader#logsBloom}.
     */
    public void addToBloomFilter (BloomFilter bloomFilter) {
        byte[] bits = new byte[256];
        bloomFilter.add(logger.bytes);
        for (var topic: topics)
            bloomFilter.add(topic.bytes);
    }

    // ---------------------------------------------------------------------------------------------
}
