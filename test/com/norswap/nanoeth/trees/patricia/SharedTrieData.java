package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.data.MerkleRoot;
import norswap.utils.IO;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;

import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;

/**
 * Trie data from the test cases hosted at https://github.com/ethereum/tests.
 * <p>These are loaded from the {@code testdata/TrieTests} directory.
 */
public final class SharedTrieData {
    // ---------------------------------------------------------------------------------------------

    /** List of all parsed trie test cases. */
    public static final ArrayList<TrieTestCase> TEST_CASES;

    // ---------------------------------------------------------------------------------------------

    /** The directory in which trie test cases are stored. */
    private static final String DIRECTORY = "testdata/TrieTests/";

    static {
        TEST_CASES = loadTestCases();
    }

    private static ArrayList<TrieTestCase> loadTestCases() {
        var testCases = new ArrayList<TrieTestCase>();
        var dir = new File(DIRECTORY);
        File[] files = dir.listFiles();
        assert files != null;
        for (File file: files) {
            var fileName = file.getName();

            // This tests the insertion point of keys (by giving for each inserted key the previous
            // and next key). I'm not that iterating on keys is actually required in Ethereum,
            // so I'm not implementing this for now.
            if (fileName.equals("trietestnextprev.json")) continue;

            var string = IO.slurp(file.toString());
            assert string != null;
            var json = new JSONObject(string);
            for (var name: json.keySet()) {
                var test = json.getJSONObject(name);
                var pairs = test.get("in");
                hasHexStrings = false;

                // Somewhat debatably (a flag would have been better), some key-value pairs are
                // encoded as object properties (in trieanyorder*.json) when the insertion order
                // doesn't matter.
                var array = pairs instanceof JSONObject
                    ? collectPairsFromJsonObject((JSONObject) pairs)
                    : collectPairsFromJsonArray((JSONArray) pairs);

                var root = new MerkleRoot(test.getString("root"));
                testCases.add(new TrieTestCase(
                    fileName, name, array, root, fileName.contains("secure"), !hasHexStrings));
            }
        }
        return testCases;
    }

    // ---------------------------------------------------------------------------------------------

    private static String[][] collectPairsFromJsonObject (JSONObject pairs) {
        var keys = pairs.keySet();
        var array = new String[keys.size()][2];
        int i = 0;
        for (String key: keys) {
            var value = pairs.getString(key);
            array[i][0] = stringToHex(key);
            array[i][1] = stringToHex(value);
            ++i;
        }
        return array;
    }

    // ---------------------------------------------------------------------------------------------

    private static String[][] collectPairsFromJsonArray (JSONArray pairs) {
        var array = new String[pairs.length()][2];
        for (int i = 0; i < pairs.length(); i++) {
            var key      = pairs.getJSONArray(i).getString(0);
            var objValue = pairs.getJSONArray(i).get(1);
            var value    = objValue == JSONObject.NULL ? "" : (String) objValue;
            array[i][0] = stringToHex(key);
            array[i][1] = stringToHex(value);
        }
        return array;
    }

    // ---------------------------------------------------------------------------------------------

    private static boolean hasHexStrings = false;

    private static String stringToHex (String str) {
        // Note that we ignore the hexEncoded from hex_encoded_securetrie_test.json, because
        // other tests also have hex strings that need to be converted, without the flag set.
        var has0x = str.startsWith("0x");
        hasHexStrings = hasHexStrings || has0x;
        return has0x ? str : toFullHexString(str.getBytes());
    }

    // ---------------------------------------------------------------------------------------------
}
