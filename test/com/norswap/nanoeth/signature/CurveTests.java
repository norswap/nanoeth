package com.norswap.nanoeth.signature;

import com.norswap.nanoeth.TestUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.testng.annotations.Test;
import java.math.BigInteger;

import static com.norswap.nanoeth.signature.Curve.SECP256K1;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests the private {@code com.norswap.nanoeth.signature.Curve} class.
 */
public final class CurveTests {

    // ---------------------------------------------------------------------------------------------

    private final EthKeyPair keys       = new EthKeyPair();
    private final BigInteger privateKey = TestUtils.getField(keys, "privateKey");
    private final ECPoint    publicKey  = TestUtils.getField(keys, "publicKey");

    // ---------------------------------------------------------------------------------------------

    @Test public void testSignVerify() {
        byte[] message = new byte[]{0, 1, 2, 3};
        BigInteger[] components = SECP256K1.sign(privateKey, message);
        assertTrue(SECP256K1.verify(publicKey, components[0], components[1], message));
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testPoint() {
        var x = publicKey.getXCoord().toBigInteger();
        var y = publicKey.getYCoord().toBigInteger();
        boolean yOdd = y.mod(BigInteger.TWO).intValueExact() == 1;
        assertEquals(SECP256K1.point(x, yOdd), publicKey);
    }

    // ---------------------------------------------------------------------------------------------
}
