package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.rlp.RLP;
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
            OfficialTransactionData .TEST_CASES.stream().map(t -> new Object[] { t }),
            OwnTransactionData      .TEST_CASES.stream().map(t -> new Object[] { t })
        ).toArray(Object[][]::new);
    }

    // ---------------------------------------------------------------------------------------------

    @Test(dataProvider = "transactions")
    public void testTransaction (TransactionTestCase testCase)
            throws IllegalTransactionFormatException {

        CONTEXT.blockHeight = testCase.blockHeight;
        if (testCase.valid)
            testValidTransaction(testCase);
        else
            Assert.assertThrows(() -> testValidTransaction(testCase));
        CONTEXT.reset();
    }

    // ---------------------------------------------------------------------------------------------

    private void testValidTransaction (TransactionTestCase testCase) throws IllegalTransactionFormatException {
        String hex = testCase.hexRLP;

        // extra tests for RLP: encode(decode(bytes)) == bytes
        byte[] bytesFromHex = ByteUtils.hexStringToBytes(hex, 0);
        var rlpFromHex = RLP.decode(bytesFromHex);
        byte[] bytesReencodedFromHex = rlpFromHex.encode();
        assertEquals(bytesReencodedFromHex, bytesFromHex);

        // test transaction parsing + serialization: toRlp(txFromRlp(rlp)) == rlp
        var tx = Transaction.from(testCase.envelopeType, rlpFromHex);
        var rlpFromTx = tx.rlp();
        assertEquals(rlpFromTx, rlpFromHex);

        // test transaction parsing + serialization: txFromRlp(toRlp(tx)) == tx
        var txReconstructedFromRlp = Transaction.from(testCase.envelopeType, rlpFromTx);
        assertEquals(txReconstructedFromRlp, tx); // test transaction parsing

        // test dumping hex string: toHexString(tx) == hexString
        assertEquals(tx.toHexString(), hex);

        // test signature
        assertTrue(tx.verifySignature());
        assertTrue(tx.verifySignature(tx.signingRLP().encode()));

        if (testCase instanceof OfficialTransactionTestCase) {
            // official tests are supposed to fail when chain id != 1
            assertTrue(tx.chainId.same(1));

            var oCase = (OfficialTransactionTestCase) testCase;
            assertEquals(tx.hash().toFullHexString(), "0x" + oCase.result.hash);
            assertEquals(tx.recoverSender().toString(), "0x" + oCase.result.sender);
        }
    }

    // ---------------------------------------------------------------------------------------------
}
