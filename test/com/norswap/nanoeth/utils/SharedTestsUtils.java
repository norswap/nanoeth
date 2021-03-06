package com.norswap.nanoeth.utils;

import static com.norswap.nanoeth.versions.EthereumVersion.*;

/**
 * Utilities for dealing with shared tests from the ethereum/tests repository.
 */
public final class SharedTestsUtils {
    private SharedTestsUtils () {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Path to a local clone of the github.com/ethereum/tests repository, passed in as a system
     * property by the gradle build file, which itselfs reads it from a gradle property (e.g.
     * specified in "gradle.properties").
     */
    public static final String PATH = System.getProperty("ethereumTests");

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the starting block height for the given version string used in the shared tests.
     */
    public static int blockHeight (String version) {
        return switch (version) {
            case "Frontier"             -> FRONTIER.startBlock;
            case "Homestead"            -> HOMESTEAD.startBlock;
            case "EIP150"               -> TANGERINE_WHISTLE.startBlock;
            case "EIP158"               -> SPURIOUS_DRAGON.startBlock; // (*)
            case "Byzantium"            -> BYZANTIUM.startBlock;
            case "Constantinople"       -> CONSTANTINOPLE.startBlock;
            case "ConstantinopleFix"    -> CONSTANTINOPLE.startBlock;
            case "Istanbul"             -> ISTANBUL.startBlock;
            case "Berlin"               -> BERLIN.startBlock;
            case "London"               -> LONDON.startBlock;
            default -> throw new AssertionError("unreachable: " + version);

            // (*) if interpreted as EIP-161 that supersedes EIP-158
        };
    }

    // ---------------------------------------------------------------------------------------------
}
