package com.norswap.nanoethdev;

import com.norswap.nanoeth.Context;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.signature.EthKeyPair;
import com.norswap.nanoeth.transactions.IllegalTransactionFormatException;
import com.norswap.nanoeth.transactions.Transaction;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.ReflectionUtils;
import com.norswap.nanoeth.versions.EthereumVersion;
import norswap.utils.IO;
import org.json.JSONObject;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;

import static com.norswap.nanoeth.utils.ByteUtils.toCompressedHexString;

/**
 * The goal of this script is to update some of the transaction tests from the
 * github.com/ethereum/tests repository.
 *
 * The issue is that some transactions in those tests have a "malleable signature", essentially, a
 * signature `s` that is higher than the ellipctic curve parameter `N` divided by 2.
 * This should be rejected after EIP-2 (to avoid replay attacks) and so is only valid on Frontier.
 *
 * The affected test cases are negative tests - test that pass if the client rejects the
 * transaction. The problem is that currently, an implementation may reject them (except for
 * Frontier) because the signature is invalid, and not because it validated the thing that was
 * supposed to be tested (e.g. for `TransactionWithGasPriceOverflow`: that the gas price does not
 * overflow, i.e. is encoded on more than 32 bytes), making these tests much less useful than they
 * should be.
 *
 * Relevant issue: https://github.com/ethereum/tests/issues/912
 */
public class FixBadSignature {

    static String TEST_REPO = "/Users/norswap/ethereum/ethereum-tests/";

    static String[] badSignatureTestsPaths = new String[] {
        // MUST disable natural size check in RLPParsing.java for this to not crash
        "ttNonce/TransactionWithNonceOverflow",
        "ttGasPrice/TransactionWithGasPriceOverflow",
        "ttData/DataTestNotEnoughGAS",
    };

    public static void main (String[] args) throws IOException, IllegalTransactionFormatException {
        for (String testPathStr: badSignatureTestsPaths) {
            var testPath
                = TEST_REPO + "TransactionTests/" + testPathStr + ".json";
            var testFillerPath
                = TEST_REPO + "src/TransactionTestsFiller/" + testPathStr + "Filler.json";

            sanityCheckFiler(testFillerPath);

            // NOTE: this is necessary, otherwise the check for s > n/2 will kick in, which is
            // precisely why we are regenerating these tests.
            Context.CONTEXT.blockHeight = EthereumVersion.FRONTIER.startBlock;

            var testString = IO.slurp(testPath);
            var testJson = new JSONObject(testString);
            var name = testJson.keys().next();
            testJson = testJson.getJSONObject(name);
            var hexRLP = testJson.getString("rlp");
            var rlp = RLP.decode(hexRLP);
            var tx = Transaction.from(0, rlp);
            // to inspect
            // DebugUtils.dump(tx);

            var keyPair = new EthKeyPair();
            var fixedTx = tx.sign(keyPair);
            // to inspect
            // DebugUtils.dump(fixedTx);

            Assert.that(fixedTx.recoverSender().equals(keyPair.address()),
                "Recovered sender is not the address derived from the public key!");

            BigInteger privateKey = ReflectionUtils.getField(keyPair, "privateKey");
            var privateKeyHex = toCompressedHexString(privateKey.toByteArray());

            System.out.println("FOR TEST CASE " + testPath + " , " + testFillerPath);
            System.out.println("update filler with:");
            System.out.println("private key: " + privateKeyHex);
            System.out.println("r: " + toCompressedHexString(fixedTx.signature.r.toByteArray()));
            System.out.println("s: " + toCompressedHexString(fixedTx.signature.s.toByteArray()));
            // OK, the buggy cases are all legacy transactions
            System.out.println("v: " + (27 + fixedTx.signature.yParity));

            System.out.println();
            System.out.println("update test with:");
            System.out.println("filledwith: nanoeth+commit.TODO");
            System.out.println("lllcversion: none");
            System.out.println("sourceHash: TODO");
            System.out.println("rlp: " + fixedTx.rlp().toHexString());

            System.out.println();
            System.out.println("----------------------");
            System.out.println();
        }
    }

    private static void sanityCheckFiler (String testFillerPath) {
        var string = IO.slurp(Path.of(testFillerPath).toString());
        var json = new JSONObject(string);
        var name = json.keys().next();
        json = json.getJSONObject(name);
        var expectArray = json.getJSONArray("expect");
        var expect = expectArray.getJSONObject(0);
        var txJson = json.getJSONObject("transaction");

        Assert.that(
            expectArray.length() == 1,
            "More than one \"expect\" entry");
        Assert.that(
            expect.getJSONArray("network").getString(0).equals(">=Frontier"),
            "We expected a test for >=Frontier");
        Assert.that(
            expect.getString("result").equals("invalid"),
            "We expected an \"invalid\" result");
    }
}
