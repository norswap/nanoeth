package com.norswap.nanoeth.signature;

import com.norswap.nanoeth.TestUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.testng.annotations.Test;
import java.lang.reflect.Method;
import java.math.BigInteger;

import static com.norswap.nanoeth.TestUtils.invokeCast;
import static org.testng.Assert.assertTrue;

public final class SignatureTests {

    // ---------------------------------------------------------------------------------------------

    private final EthKeyPair keys       = new EthKeyPair();
    private final BigInteger privateKey = TestUtils.getField(keys, "privateKey");
    private final ECPoint    publicKey  = TestUtils.getField(keys, "publicKey");

    private final Method signWithoutHashing =
        TestUtils.getMethod(EthKeyPair.class, "signWithoutHashing", byte[].class);

    private final Method verifyWithoutHashing =
        TestUtils.getMethod(Signature.class, "verifyWithoutHashing", ECPoint.class, byte[].class);

    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
    @Test public void testSignVerify() {
        byte[] message = new byte[]{0, 1, 2, 3};
        Signature sig = invokeCast(signWithoutHashing, keys, message);
        assertTrue(invokeCast(verifyWithoutHashing, sig, publicKey, message));
        sig = keys.sign(message);
        assertTrue(sig.verify(publicKey, message));
        assertTrue(sig.verify(message));
    }

    // ---------------------------------------------------------------------------------------------
}
