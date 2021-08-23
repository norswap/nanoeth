package com.norswap.nanoeth;

import com.norswap.nanoeth.data.Natural;

/**
 * Configurable values. Hardcoded for now.
 */
public final class Config {
    private Config() {}

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

    /* Difficulty for the initial block. */
    public static Natural GENESIS_DIFFICULTY = new Natural(131072); // 0x20000

    /* Whether the node should validate the difficulty & the proof-of-work. */
    public static boolean VALIDATE_POW = true;
}
