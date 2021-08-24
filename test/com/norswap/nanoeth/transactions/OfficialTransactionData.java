package com.norswap.nanoeth.transactions;

import norswap.utils.IO;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static com.norswap.nanoeth.versions.EthereumVersion.*;

/**
 * Transaction data from the test cases hosted at https://github.com/ethereum/tests.
 * <p>These are loaded from the {@code testdata/TransactionTests} directory.
 */
public final class OfficialTransactionData {

    // ---------------------------------------------------------------------------------------------

    /** List of all parsed transaction test cases. */
    public static final ArrayList<TransactionTestCase> TEST_CASES;

    // ---------------------------------------------------------------------------------------------

    /** List of test cases to skip. */
    private static final HashSet<String> SKIPPED = new HashSet<>(Arrays.asList(
            "dataTx_bcValidBlockTestFrontier.json",  // we don't validate gas yet (this has 50k gas, which is insufficient except on frontier)
            "DataTestInsufficientGas2028.json",      // we don't validate gas yet
            "DataTestSufficientGas2028.json",        // we don't validate gas yet
            "DataTestNotEnoughGAS.json",             // we don't validate gas yet
            "EmptyTransaction.json"                  // we don't validate gas yet (TODO: also has an invalid > n/2 signature)
    ));

    // ---------------------------------------------------------------------------------------------

    /** The prefix (excluding the base name) of the path of the directories in which transaction
     * test cases are stored. */
    private static final String DIRECTORY_PREFIX = "testdata/TransactionTests/";

    /** The basename of the directories in which transaction tests cases are stored.*/
    private static final String[] DIRECTORIES = new String[] {
            "ttAddress", "ttData", "ttEIP2028", "ttGasPrice", "ttNonce", "ttRSValue",
            "ttSignature", "ttValue", "ttVValue", "ttWrongRLP"
            // , "ttGasLimit" // we don't valid the gas limit yet
    };

    /** All the versions (hard forks) for which results are supplied in the test cases. */
    private static final String[] VERSIONS = new String[]{
            "Frontier", "Homestead", "EIP150", "EIP158", "Byzantium", "Constantinople",
            "ConstantinopleFix", "Istanbul"
    };

    /** Returns the starting block height for the given version. */
    private static int blockHeight (String version) {
        return switch (version) {
            case "Frontier"             -> FRONTIER.startBlock;
            case "Homestead"            -> HOMESTEAD.startBlock;
            case "EIP150"               -> TANGERINE_WHISTLE.startBlock;
            case "EIP158"               -> SPURIOUS_DRAGON.startBlock; // (*)
            case "Byzantium"            -> BYZANTIUM.startBlock;
            case "Constantinople"       -> CONSTANTINOPLE.startBlock;
            case "ConstantinopleFix"    -> CONSTANTINOPLE.startBlock;
            case "Istanbul"             -> ISTANBUL.startBlock;
            default -> throw new AssertionError("unreachable");

            // (*) if interpreted as EIP-161 that supersedes EIP-158
        };
    }

    static {
        TEST_CASES = loadTestCases();
    }

    private static ArrayList<TransactionTestCase> loadTestCases() {
        var testCases = new ArrayList<TransactionTestCase>();
        for (String path: DIRECTORIES) {
            var dir = new File(DIRECTORY_PREFIX + path);
            File[] files = dir.listFiles();
            assert files != null;
            for (File file: files) {
                var fileName = file.getName();
                if (SKIPPED.contains(fileName)) continue;
                var string = IO.slurp(file.toString());
                assert string != null;
                var json = new JSONObject(string);
                var name = json.keys().next();
                var data = json.getJSONObject(name);
                var rlp = data.getString("rlp");

                for (String version: VERSIONS) {
                    var versionResult = data.getJSONObject(version);
                    var valid = versionResult.has("sender");
                    testCases.add(new TransactionTestCase(
                        String.format("%s (%s))", fileName, version),
                        blockHeight(version), /* chainId */ 1, rlp, valid,
                        valid ? "0x" + versionResult.getString("hash")   : null,
                        valid ? "0x" + versionResult.getString("sender") : null));
                }
            }
        }
        return testCases;
    }

    // ---------------------------------------------------------------------------------------------
    // Some notes from playing with the official tests
    //
    // - ttSignature/Vitalik_1.json - the r & s signatures are identical
    //
    // ---------------------------------------------------------------------------------------------
}