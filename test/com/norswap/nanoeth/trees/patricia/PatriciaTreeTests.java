package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.utils.Hashing;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static com.norswap.nanoeth.utils.ByteUtils.hexStringToBytes;
import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;
import static org.testng.Assert.*;

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
        var keys = new ArrayList<String>(testCase.pairs.length);

        for (var pair : testCase.pairs) {
            var key   = hexStringToBytes(pair[0]);
            var value = hexStringToBytes(pair[1]);

            if (testCase.isSecureTrie)
                key = Hashing.keccak(key).bytes;
            if (value.length == 0) {
                tree = tree.remove(key);
                keys.remove(toFullHexString(key));
                assertNull(tree.lookup(key));
                var proof = tree.prove(key);
                assertTrue(proof.verify(tree.merkleRoot()));
            } else {
                tree = tree.add(key, value);
                keys.add(toFullHexString(key));
                assertEquals(tree.lookup(key), value);
                var proof = tree.prove(key);
                assertTrue(proof.verify(tree.merkleRoot()));
            }
        }

        assertEquals(tree.merkleRoot(), testCase.root);

        for (var key: keys) {
            var proof = tree.prove(hexStringToBytes(key));
            assertNotNull(proof.value);
            assertTrue(proof.verify(tree.merkleRoot()));
        }
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
