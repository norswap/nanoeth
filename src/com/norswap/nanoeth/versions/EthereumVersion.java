package com.norswap.nanoeth.versions;

import com.norswap.nanoeth.Config;
import com.norswap.nanoeth.Context;
import com.norswap.nanoeth.data.Natural;

import static com.norswap.nanoeth.Context.CONTEXT;

/**
 * Enumeration of Ethereum's "versions": the hard forks that were rolled out during the chain's
 * history.
 *
 * <p>Note that the {@link #CONSTANTINOPLE} fork also includes the St-Petersburg hard fork at the
 * same block height, which removes EIP-1283, which was only rolled out on the testnets.
 */
public enum EthereumVersion {

    FRONTIER            (Config.FRONTIER_START          ),
    FRONTIER_THAWING    (Config.FRONTIER_THAWING_START  ),
    HOMESTEAD           (Config.HOMESTEAD_START         ),
    DAO_FORK            (Config.DAO_FORK_START          ),
    TANGERINE_WHISTLE   (Config.TANGERINE_WHISTLE_START ),
    SPURIOUS_DRAGON     (Config.SPURIOUS_DRAGON_START   ),
    BYZANTIUM           (Config.BYZANTIUM_START         ),
    CONSTANTINOPLE      (Config.CONSTANTINOPLE_START    ),
    PETERSBURG          (Config.PETERSBURG_START        ),
    ISTANBUL            (Config.ISTANBUL_START          ),
    MUIR_GLACIER        (Config.MUIR_GLACIER_START      ),
    BERLIN              (Config.BERLIN_START            ),
    LONDON              (Config.LONDON_START            );

    // ---------------------------------------------------------------------------------------------

    private static final EthereumVersion[] VERSIONS = values();

    // ---------------------------------------------------------------------------------------------

    /** Block height where the fork took effect. */
    public final int startBlock;

    // ---------------------------------------------------------------------------------------------

    EthereumVersion (int startBlock) {
        this.startBlock = startBlock;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true iff the current block height ({@link Context#blockHeight}) is lower than
     * the initial block height ({@link #startBlock}) of this version.
     */
    public boolean isFuture () {
        return CONTEXT.blockHeight < startBlock;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true iff the current block height ({@link Context#blockHeight}) is higher or equal
     * than the initial block height ({@link #startBlock}) of this version.
     */
    public boolean isPast() {
        return CONTEXT.blockHeight >= startBlock;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true iff the start block of this version is before the given block height.
     */
    public boolean startsBefore (int blockHeight) {
        return startBlock < blockHeight;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true iff the start block of this version is before the given block height.
     */
    public boolean startsBefore (Natural blockHeight) {
        return startsBefore(blockHeight.intValue());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true iff the given block height is "within" this version: after or on its start block,
     * but before the start block of the next version.
     */
    public boolean contains (int blockHeight) {
        return ordinal() < VERSIONS.length - 1
            ? startBlock <= blockHeight && blockHeight < VERSIONS[ordinal() + 1].startBlock
            : startBlock <= blockHeight;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true if the given block height is "within" this version: after or on its start block,
     * but before the start block of the next version.
     */
    public boolean contains (Natural blockHeight) {
        return contains(blockHeight.intValue());
    }

    // ---------------------------------------------------------------------------------------------
}
