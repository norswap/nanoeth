package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.utils.Hashing;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.norswap.nanoeth.utils.ByteUtils.hexStringToBytes;
import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;
import static org.testng.Assert.assertEquals;

/** @see TrieTestCase */
public class PatriciaTreeTests {
    // ---------------------------------------------------------------------------------------------

    @DataProvider public static Object[][] trieTestCases () {
        return SharedTrieData.TEST_CASES.stream()
                .map(t -> new Object[] { t })
                .toArray(Object[][]::new);
    }

    // ---------------------------------------------------------------------------------------------

    @Test(dataProvider = "trieTestCases")
    public void testTrie (TrieTestCase testCase) {
        var tree = new PatriciaTree();
        for (var pair : testCase.pairs) {
            var key = hexStringToBytes(pair[0]);
            var value = hexStringToBytes(pair[1]);
            if (testCase.isSecureTrie)
                key = Hashing.keccak(key).bytes;
            tree = value.length == 0
                ? tree.remove(key)
                : tree.add(key, value);
            assertEquals(tree.lookup(key), value.length == 0 ? null : value);
        }
        assertEquals(tree.merkleRoot(), testCase.root);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Prints the key-value pairs in the tree, displaying the keys and values as ascii string
     * if they were originally encoded from such strings - excepted for keys that were hashed
     * prior to insertion.
     *
     * <p>If you're interested in the tree itself, it can be pretty-printed using:
     * {@code System.out.println(DebugUtils.indentTreeString(tree));}
     */
    private void printKeyValues (TrieTestCase testCase, PatriciaTree tree) {
        tree.toMap().forEach((k, v) -> {
            var key = testCase.asciiKeyValues && !testCase.isSecureTrie
                ? new String(k)
                : toFullHexString(k);
            var value = testCase.asciiKeyValues ? new String(v) : toFullHexString(v);
            System.out.println(key + " : " + value);
        });
    }

    // ---------------------------------------------------------------------------------------------
}
