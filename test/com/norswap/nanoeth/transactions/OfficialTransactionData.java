package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.transactions.OfficialTransactionTestCase.Result;
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
    public static final ArrayList<OfficialTransactionTestCase> TEST_CASES;

    // ---------------------------------------------------------------------------------------------

    /** List of test cases to skip. */
    private static final HashSet<String> SKIPPED = new HashSet<>(Arrays.asList(
            "dataTx_bcValidBlockTestFrontier.json",  // we don't validate gas yet (this has 50k gas, which is insufficient except on frontier)
            "DataTestInsufficientGas2028.json",      // we don't validate gas yet

            // TODO must validate nonce size! & why does it fail currently? (malleability)
            // "TransactionWithNonceOverflow.json",

            // TODO must valid gas price! & why does it fail currently? (malleability)
            // "TransactionWithGasPriceOverflow.json",

            // NOTE: This actually passes because it is rejected for being malleable.
            //   It should be rejected for lack of gas instead.
            //   Might be an issue in the test suite.
            //   Leave it here as a reminder.
            "DataTestNotEnoughGAS.json"
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

    // TODO test at more than just the istanbul block height!

    static {
        TEST_CASES = loadTestCases();
    }

    private static ArrayList<OfficialTransactionTestCase> loadTestCases() {
        var testCases = new ArrayList<OfficialTransactionTestCase>();
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
                var result = parseResultIstanbul(fileName, data);
                testCases.add(new OfficialTransactionTestCase(
                    fileName, blockHeight("Istanbul"), rlp, result));
            }
        }
        return testCases;
    }

    private static Result parseResultIstanbul (String file, JSONObject data) {
        var istanbul = data.getJSONObject("Istanbul");
        return !istanbul.has("sender")
            ? null
            : new Result(
                "0x" + istanbul.getString("hash"),
                "0x" + istanbul.getString("sender"));
    }

    // ---------------------------------------------------------------------------------------------
    // Some notes from playing with the official tests
    //
    // - ttSignature/Vitalik_1.json - the r & s signatures are identical
    //
    // ---------------------------------------------------------------------------------------------
}