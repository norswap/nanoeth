package com.norswap.nanoeth.history;

import java.time.LocalDate;

/**
 * Enumeration of Ethereum's "versions": the hard forks that were rolled out during the chain's
 * history.
 *
 * <p>Note that the {@link #CONSTANTINOPLE} fork also includes the St-Petersburg hard fork at the
 * same block height, which removes EIP-1283, which was only rolled out on the testnets.
 */
public enum EthereumVersion {

    FRONTIER            (0,             LocalDate.of(2015,  7, 30)),
    ICE_AGE             (200_000,       LocalDate.of(2015,  9,  8)),
    HOMESTEAD           (1_150_000,     LocalDate.of(2016,  3, 15)),
    DAO_FORK            (1_920_000,     LocalDate.of(2016,  7, 20)),
    TANGERINE_WHISTLE   (2_463_000,     LocalDate.of(2016, 10, 18)),
    SPURIOUS_DRAGON     (2_675_000,     LocalDate.of(2016, 11, 23)),
    BYZANTIUM           (4_370_000,     LocalDate.of(2017, 10, 16)),
    CONSTANTINOPLE      (7_280_000,     LocalDate.of(2019,  2, 28)),
    ISTANBUL            (9_069_000,     LocalDate.of(2019, 12,  8)),
    MUIR_GLACIER        (9_200_000,     LocalDate.of(2020,  1,  1)),
    BERLIN              (12_244_000,    LocalDate.of(2021,  4, 15)),
    LONDON              (12_965_000,    LocalDate.of(2021,  8,  4));

    /** Block height where the fork took effect. */
    public final int startBlock;

    /** Date on which the fork took effect. */
    public final LocalDate startDate;

    EthereumVersion (int startBlock, LocalDate startDate) {
        this.startBlock = startBlock;
        this.startDate = startDate;
    }
}
