package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.utils.Hashing;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static com.norswap.nanoeth.utils.ByteUtils.hexStringToBytes;
import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;
import static org.bouncycastle.pqc.math.linearalgebra.ByteUtils.toHexString;
import static org.testng.Assert.*;

public class MerkleProofTests {
    // ---------------------------------------------------------------------------------------------

    @DataProvider
    public static Object[][] trieTestCases () {
        return SharedTrieData.TEST_CASES.stream()
                .map(t -> new Object[] { t })
                .toArray(Object[][]::new);
    }

    // ---------------------------------------------------------------------------------------------

    @Test(dataProvider = "trieTestCases")
    public void testMerkleProofs (TrieTestCase testCase) {
        var tree = new PatriciaTree();
        var keys = new ArrayList<String>(testCase.pairs.length);
        for (var pair: testCase.pairs) {
            var key = hexStringToBytes(pair[0]);
            var value = hexStringToBytes(pair[1]);
            if (testCase.isSecureTrie)
                key = Hashing.keccak(key).bytes;
            tree = value.length == 0
                    ? tree.remove(key)
                    : tree.add(key, value);
            if (value.length == 0) {
                keys.remove(toFullHexString(key));
            } else {
                keys.add(toFullHexString(key));
                var proof = tree.prove(key);
                assertTrue(proof.check(tree.merkleRoot()));
            }
        }
        for (var key: keys) {
            var proof = tree.prove(hexStringToBytes(key));
            assertNotNull(proof);
            assertTrue(proof.check(tree.merkleRoot()));
        }
    }

    // ---------------------------------------------------------------------------------------------
}
