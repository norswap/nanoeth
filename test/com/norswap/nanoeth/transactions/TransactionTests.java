package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.rlp.RLPItem;
import com.norswap.nanoeth.utils.ByteUtils;
import org.testng.annotations.Test;

import static com.norswap.nanoeth.transactions.TransactionData.TX_HEX_STRINGS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test transaction encoding, decoding & signing.
 */
public final class TransactionTests {

    // ---------------------------------------------------------------------------------------------

    @Test public void testTransaction() throws IllegalTransactionFormatException {
        for (String hex: TX_HEX_STRINGS) {

            // extra tests for RLP: encode(decode(bytes)) == bytes
            byte[] bytesFromHex = ByteUtils.hexStringToBytes(hex, 0);
            var rlpFromHex = RLPItem.decode(bytesFromHex);
            byte[] bytesReencodedFromHex = rlpFromHex.encode();
            assertEquals(bytesReencodedFromHex, bytesFromHex);

            // TODO handle other transactions types (first param of Transaction.from)

            // test transaction parsing + serialization: toRlp(txFromRlp(rlp)) == rlp
            var tx = Transaction.from(0, rlpFromHex);
            var rlpFromTx = tx.rlp();
            assertEquals(rlpFromTx, rlpFromHex);

            // test transaction parsing + serialization: txFromRlp(toRlp(tx)) == tx
            var txReconstructedFromRlp = Transaction.from(0, rlpFromTx);
            assertEquals(txReconstructedFromRlp, tx); // test transaction parsing

            // test dumping hex string: toHexString(tx) == hexString
            assertEquals(tx.toHexString().toLowerCase(), hex.toLowerCase());

            // test signature
            assertTrue(tx.verifySignature());
            assertTrue(tx.verifySignature(tx.signingRLP().encode()));
        }
    }

    // ---------------------------------------------------------------------------------------------
}
