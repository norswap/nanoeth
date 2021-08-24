package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.Config;
import com.norswap.nanoeth.data.Natural;
import java.math.BigInteger;

import static com.norswap.nanoeth.versions.EthereumVersion.*;
import static com.norswap.nanoeth.versions.EthereumVersion.BYZANTIUM;

public final class Difficulty {
    private Difficulty() {}

    /**
     * Compute the canonical difficulty of a block arriving at the given timestamp, with the given
     * parent block header. If the parent is null, returns the genesis difficulty.
     */
    public static Natural computeDifficulty (Natural timestamp, BlockHeader parent) {

        // Initial difficulty, D0 in yellowpaper.
        final var genesisDifficulty = Config.GENESIS.header.difficulty;
        if (parent == null) // genesis
            return genesisDifficulty;

        // Block must come chronologically after its parent, must be checked upstream.
        assert timestamp.compareTo(parent.timestamp) > 0;

        // Number of the block for which the difficulty is being computed (yellowpaper: Hi).
        final Natural number = parent.number.add(1);

        // The difficulty is increased by x * S2 on every block, where:
        // - x the difficulty of the parent / 2048, which is the unit of increment.
        // - S2 is a scaling factor that mostly depends on the time between the block and its parent.
        //   This factor can be positive or negative.

        // The goal is to target a mean/median block time of ~13s.
        // Adjustments were made over various hard forks, for instance, the mean block time didn't
        // use to account for uncles in the calculation.

        final Natural x = parent.difficulty.divide(2048);
        BigInteger S2;

        // Timestamp difference (in seconds) between this block and its parent.
        var timeDiff = new Natural(timestamp.subtract(parent.timestamp));

        if (FRONTIER.contains(number)) {
            // The factor used to be 1 or -1 depending on whether the block time overshot or
            // undershot a 13s target. Some miners started mining only blocks whose timestamp was
            // parentTimestamp + 1. It's not clear to me why they did this. I thought maybe blocks
            // with similar difficulty would tie-break on timestamp, but at least geth never did
            // this (maybe other clients). Might just have been an implmenetation fluke. This had
            // the bad property of pushing the (theoretical, since these blocks were not *actually*
            // within one second) mean block time very high, while preserving the 13s median block
            // time. This was fixed in Homestead.
            S2 = new Natural(timeDiff.greaterSame(13) ? 1 : -1);
        }
        else if (HOMESTEAD.contains(number)) { // EIP-2
            S2 = BigInteger.ONE.subtract(timeDiff.divide(10));
        }
        else { // EIP-100, Byzantium & later
            // This factor reduces the scaling factor by one if the parent blocks has uncles. (y in
            // yellowpaper). The goal is to preserve the ~13s mean block time, but to count uncles
            // in the mean calculation.
            // This formula was explicitly picked over using the number of uncles because it can be
            // checked agains the uncles hash in the header without requiring the whole block to be
            // available.
            var unclesFactor = parent.hasUncles() ? BigInteger.TWO : BigInteger.ONE;
            S2 = new Natural(unclesFactor).subtract(timeDiff.divide(new Natural(9)));

            // Some scenarios:
            // - no uncles, block time < 9s         🡢 difficulty increases by x
            // - no uncles, 9s <= block time < 18s  🡢 no difficulty change
            // - no uncles, 18s <= block time       🡢 difficulty decreases by x
            // - uncles,    block time < 9s         🡢 difficulty increases by 2x
            // - uncles,    9s <= block time < 18s  🡢 difficulty increases by x
        }

        // The factor has a lower cap at -99 (in case the chain experiences extended downtime).
        S2 = S2.max(BigInteger.valueOf(-99));

        // The factor k is used to delay the ice age, realized by variable epsilon below.
        // The factor was introduced/changed in the versions listed in the if statement.
        final int k;
        if (LONDON.contains(number))
            k = 9_700_000; // EIP-3554
        else if (MUIR_GLACIER.contains(number))
            k = 9_000_000; // EIP-2384
        else if (CONSTANTINOPLE.contains(number))
            k = 5_000_000; // EIP-1234
        else if (BYZANTIUM.contains(number))
            k = 3_000_000; // EIP-649
        else
            k = 0;

        // The virtual block number (yellowpaper: Hi') is the number delayed by k.
        var virtualNumber = number.subtract(k).max(BigInteger.ZERO);

        // The difficulty is increased by epsilon (the "exponential difficulty") on every block.
        // Epsilon increases exponentially, with a 100.000 step. Hence, the rate of change
        // of the difficulty accelerates faster and faster.

        // The point of this is to implement an "difficulty bomb" leading to an "ice age" - a point
        // where the difficulty will become so high that the chain will stall, forcing a hard fork
        // before that point to change the difficulty computation. Since a hard fork is necessary
        // anyway, it's the opportunity to add other improvements to Ethereum. Hence, the ice age is
        // a trick that keeps miners/validators from preventing the evolution of the network.

        // The yellowpaper falsely claims that this was introduced in EIP-2, but in reality this was
        // introduced in the Frontier Thawing hard fork.

        BigInteger epsilon = BigInteger.ZERO;

        if (FRONTIER_THAWING.startsBefore(number)) {
            var exponent = virtualNumber.divide(new Natural(100_000)).subtract(BigInteger.TWO);
            epsilon = new Natural(2).pow(exponent.intValue());
        }

        // For each block, the difficulty increases by x * S2 + epsilon.
        var updatedDifficulty = parent.difficulty.add(x.multiply(S2)).add(epsilon);
        return new Natural(genesisDifficulty.max(updatedDifficulty));
    }
}
