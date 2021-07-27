package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.transactions.TransactionTestCase.Result;
import com.norswap.nanoeth.utils.ByteUtils;
import norswap.utils.IO;
import norswap.utils.exceptions.Exceptions;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;

/**
 * A collection of transactions to be used as test cases.
 *
 * <p>Stored directly in a Java class, as it avoids the ceremony of dealing with the file system.
 *
 * <p>These were obtained by looking for transaction of interest on Etherscan.
 */
public final class TransactionData
{
    // ---------------------------------------------------------------------------------------------

    public static final String[] TX_HEX_STRINGS = new String[] {
        "0xf8a90b850649534e0082b5d79495ad61b0a150d79219dcf64e1e6cc01f0b64c4ce80b844095ea7b3000000000000000000000000b4a81261b16b92af0b9f7c4a83f1e885132d81e4ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff26a02f4fb64773167b707e3fbfddc8f548d0037f7933bf866f8c94a3de6304192947a03b56e5760b222b8bed8f2b2180bea58f51bc8fe2fb23b2f0980841eb608b54f1",
        // out-of-gas transaction
        "0xf8a917850430e234008271ab94cc8fa225d80b9c7d42f96e9570156c65d6caaa2580b844a9059cbb000000000000000000000000a2b713632c11ab8e99a426aacaf247f37b173554000000000000000000000000000000000000000000000000000000000000091426a07672fa108c29517f441b344827caf00f5a5f6081ab70b11633964b11c68676dba0269a75d63217085cd24f4d8d5059454dddf787feff6679ad4db9d88435ad8b2e",
        // an "interesting" transaction: https://etherscan.io/tx/0x9c81f44c29ff0226f835cd0a8a2f2a7eca6db52a711f8211b566fd15d3e0e8d4
        // notice the interesting r value
        // has an empty access list, but not captured here
        // to a contract, but ethereal sets the output to null :'(
        // TODO
    };

    // ---------------------------------------------------------------------------------------------

    // unused
    public static final Transaction[] TX = Arrays.stream(TX_HEX_STRINGS)
        .map(TransactionData::parseTransaction)
        .toArray(Transaction[]::new);

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseTransaction (String s) {

        return Exceptions.suppress(() -> {
           byte[] bytes = ByteUtils.hexStringToBytes(s, 0);
           // TODO handle more transaction types
           return Transaction.from(0, RLP.decode(bytes));
        });
    }

    // ---------------------------------------------------------------------------------------------

    /** The prefix (excluding the base name) of the path of the directories in which transaction
     * test cases are stored. */
    private static final String DIRECTORY_PREFIX = "testdata/TransactionTests/";

    /** The basename of the directories in which transaction tests cases are stored.*/
    private static final String[] DIRECTORIES = new String[] {
        "ttAddress", "ttData", "ttEIP2028", "ttGasLimit", "ttGasPrice", "ttNonce", "ttRSValue",
        "ttSignature", "ttValue", "ttVValue", "ttWrongRLP"

    };

    /** List of all parsed transaction test cases. */
    public static final ArrayList<TransactionTestCase> ETHEREUM_TESTS_TRANSACTIONS
        = new ArrayList<>();

    /** Indicate the test case should be skipped, given problem parsing the result. */
    private static final Result skipThisTestCase = new Result(null, null);

    /** List of test cases to skip. */
    private static final HashSet<String> SKIPPED = new HashSet<>(Arrays.asList(
            "dataTx_bcValidBlockTestFrontier.json", // we don't validate gas yet (this has 50k gas, which is insufficient except on frontier)
            "DataTestInsufficientGas2028.json"      // we don't validate gas yet
        ));

    static {
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
                if (result != skipThisTestCase)
                    ETHEREUM_TESTS_TRANSACTIONS.add(
                        new TransactionTestCase(fileName, 0, rlp, result));
            }
        }
    }

    private static Result parseResultIstanbul (String file, JSONObject data) {
        var istanbul = data.getJSONObject("Istanbul");
        return !istanbul.has("sender")
            ? null
            : new Result(
                istanbul.getString("hash"),
                istanbul.getString("sender"));
    }

    // ---------------------------------------------------------------------------------------------

    /** The various builds for which the test results are defined in the transaction test cases. */
    private static final String[] BUILDS = new String[] {
            "Frontier", "Homestead", "EIP150", "EIP158", "Byzantium", "Constantinople",
            "ConstantinopleFix", "Istanbul" };

    private static Result parseResult (String name, JSONObject data) {
        String sender = null;
        String hash = null;

        int i = 0;
        for (String build: BUILDS) {
            var buildData = data.getJSONObject(build);
            var hasSender = buildData.has("sender");

            var sender2 = hasSender ? buildData.getString("sender") : null;
            var hash2   = hasSender ? buildData.getString("hash")   : null;

            if (i++ == 0 && hasSender) {
                sender = sender2;
                hash = hash2;
            } else if (!Objects.equals(sender, sender2) || !Objects.equals(hash, hash2)) {
                // different result expected for different builds, log & skip the test case.
                System.out.println("divergent build for test: " + name);
                return skipThisTestCase;
            }
        }

        return sender == null ? null : new Result(hash, sender);
    }

    // ---------------------------------------------------------------------------------------------
}
