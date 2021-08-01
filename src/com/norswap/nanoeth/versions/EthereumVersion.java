package com.norswap.nanoeth.versions;

import com.norswap.nanoeth.Config;
import com.norswap.nanoeth.Context;

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
    ICE_AGE             (Config.ICE_AGE_START           ),
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
}
