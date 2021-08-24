package com.norswap.nanoeth;

import com.norswap.nanoeth.blocks.Block;
import com.norswap.nanoeth.blocks.BlockHeader;
import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.receipts.BloomFilter;
import com.norswap.nanoeth.transactions.Transaction;
import com.norswap.nanoeth.utils.Hashing;

import static com.norswap.nanoeth.data.Hash.EMPTY_SEQ_HASH;

/**
 * Configurable values. Hardcoded for now.
 */
public final class Config {
    private Config() {}

    // =============================================================================================
    // region Hard Forks
    // =============================================================================================

    public static final int FRONTIER_START            =          0;
    public static final int FRONTIER_THAWING_START    =    200_000;
    public static final int HOMESTEAD_START           =  1_150_000;
    public static final int DAO_FORK_START            =  1_920_000;
    public static final int TANGERINE_WHISTLE_START   =  2_463_000;
    public static final int SPURIOUS_DRAGON_START     =  2_675_000;
    public static final int BYZANTIUM_START           =  4_370_000;
    public static final int CONSTANTINOPLE_START      =  7_280_000;
    public static final int PETERSBURG_START          =  7_280_000;
    public static final int ISTANBUL_START            =  9_069_000;
    public static final int MUIR_GLACIER_START        =  9_200_000;
    public static final int BERLIN_START              = 12_244_000;
    public static final int LONDON_START              = 12_965_000;

    // endregion
    // =============================================================================================
    // region Genesis
    // =============================================================================================

    private static final BlockHeader MAINNET_GENESIS_HEADER = new BlockHeader(
        Hash.ZERO,                      // parent hash
        EMPTY_SEQ_HASH,                 // uncle hash
        Address.ZERO,                   // coinbase
        MerkleRoot.ZERO,                // TODO state root for premine
        MerkleRoot.ZERO,                // transactions root (empty)
        MerkleRoot.ZERO,                // receipts root (empty)
        new BloomFilter(new byte[256]), // bloom filter
        new Natural(131072),            // difficult (== 0x20000 == 2^17)
        Natural.ZERO,                   // number
        new Natural(3141592),           // gas limit
        Natural.ZERO,                   // gas used
        Natural.ZERO,                   // timestamp (*)
        new byte[0],                    // extra data
        Hash.ZERO,                      // mix hash (ignored)
        new Natural(Hashing.keccak(new byte[]{42}).bytes).longValue()); // nonce

    // (*) The yellowpaper has some text to the effect of "the initial timestamp, check
    // documentation", but the chain was started with a timestamp of 0.

    // ---------------------------------------------------------------------------------------------

    /** The genesis block. */
    public static Block GENESIS = new Block(
        MAINNET_GENESIS_HEADER,
        new Transaction[0],
        new BlockHeader[0]);

    // endregion
    // =============================================================================================

    /* Whether the node should validate the difficulty & the proof-of-work. */
    public static boolean VALIDATE_POW = true;

    // =============================================================================================
}
