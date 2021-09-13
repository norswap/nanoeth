package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPParsingException;
import com.norswap.nanoeth.utils.ByteUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.stream.Stream;

import static com.norswap.nanoeth.Context.CONTEXT;
import static org.testng.Assert.*;

/**
 * Test transaction encoding, decoding & signing.
 */
public final class TransactionTests {

    // ---------------------------------------------------------------------------------------------

    @DataProvider
    public static Object[][] transactions () {
        return Stream.concat(
            SharedTransactionData.TEST_CASES.stream().map(t -> new Object[] { t }),
            OwnTransactionData      .TEST_CASES.stream().map(t -> new Object[] { t })
        ).toArray(Object[][]::new);
    }

    // ---------------------------------------------------------------------------------------------

    @Test(dataProvider = "transactions")
    public void testTransaction (TransactionTestCase testCase) throws RLPParsingException {

        CONTEXT.blockHeight = testCase.blockHeight;
        if (testCase.valid)
            testValidTransaction(testCase);
        else
            Assert.assertThrows(() -> testValidTransaction(testCase));
        CONTEXT.reset();
    }

    // ---------------------------------------------------------------------------------------------

    private void testValidTransaction (TransactionTestCase testCase) throws RLPParsingException {
        String hex = testCase.hexRLP;

        // extra tests for RLP: encode(decode(bytes)) == bytes
        byte[] bytesFromHex = ByteUtils.hexStringToBytes(hex, 0);
        var rlpFromHex = RLP.decode(bytesFromHex);
        byte[] bytesReencodedFromHex = rlpFromHex.encode();
        assertEquals(bytesReencodedFromHex, bytesFromHex);

        // test transaction parsing + serialization: toRlp(txFromRlp(rlp)) == rlp
        var tx = Transaction.from(rlpFromHex);
        var rlpFromTx = tx.rlp();
        assertEquals(rlpFromTx, rlpFromHex);

        // test transaction parsing + serialization: txFromRlp(toRlp(tx)) == tx
        var txReconstructedFromRlp = Transaction.from(rlpFromTx);
        assertEquals(txReconstructedFromRlp, tx); // test transaction parsing

        // test dumping hex string: toHexString(tx) == hexString
        assertEquals(tx.toHexString(), hex);

        // test signature
        assertTrue(tx.verifySignature());
        assertTrue(tx.verifySignature(tx.signingRLP().encode()));

        assertTrue(tx.chainId.same(testCase.chainId));

        // test hash & sender computation
        if (testCase.valid) { // check because official tests don't include these for invalid transactions
            assertEquals(tx.hash().toFullHexString(), testCase.hash);
            assertEquals(tx.recoverSender().toString(), testCase.sender);
        }
    }

    // ---------------------------------------------------------------------------------------------
}
