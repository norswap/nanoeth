package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.chain.Blockchain;
import com.norswap.nanoeth.data.Hash;
import java.util.HashMap;

/**
 * A database that enables retrieving blocks by {@link BlockHeader#hash() hash}.
 * <p>To retrieve blocks in the canonical chain by number, use {@link Blockchain} instead.
 * <p>The current implementation is fully in-memory, and does not persist any block to disk.
 */
public final class Blocks {
    private Blocks() {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Singleton blocks DB instance.
     * TODO: enable running multiple nodes in parallel
     */
    public static final Blocks DB = new Blocks();

    // ---------------------------------------------------------------------------------------------

    // current primitive implementation
    private final HashMap<Hash, Block> blocks = new HashMap<>();

    // ---------------------------------------------------------------------------------------------

    /**
     * Registers the given block in the block DB.
     */
    public void register (Block block) {
        blocks.put(block.header.hash(), block);
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the block with the given hash, or null if no such block is known. */
    public Block get (Hash hash) {
        return blocks.get(hash);
    }

    // ---------------------------------------------------------------------------------------------

    /* Returns the block header with the given hash, or null if no such block is known. */
    public BlockHeader getHeader (Hash hash) {
        var block = blocks.get(hash);
        return block != null ? block.header : null;
    }

    // ---------------------------------------------------------------------------------------------

    /** Empties the database. */
    public void clear() {
        blocks.clear();
    }

    // ---------------------------------------------------------------------------------------------
}
