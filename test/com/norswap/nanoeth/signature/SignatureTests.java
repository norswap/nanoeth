package com.norswap.nanoeth.signature;

import com.norswap.nanoeth.utils.ReflectionUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.testng.annotations.Test;
import java.lang.reflect.Method;
import java.math.BigInteger;

import static com.norswap.nanoeth.utils.ReflectionUtils.invokeCast;
import static org.testng.Assert.assertTrue;

public final class SignatureTests {

    // ---------------------------------------------------------------------------------------------

    private final EthKeyPair keys       = new EthKeyPair();
    private final BigInteger privateKey = ReflectionUtils.getField(keys, "privateKey");
    private final ECPoint    publicKey  = ReflectionUtils.getField(keys, "publicKey");

    private final Method signWithoutHashing =
        ReflectionUtils.getMethod(EthKeyPair.class, "signWithoutHashing", byte[].class);

    private final Method verifyWithoutHashing =
        ReflectionUtils.getMethod(Signature.class, "verifyWithoutHashing", ECPoint.class, byte[].class);

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
