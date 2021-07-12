package com.norswap.nanoeth.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.security.Security;
import java.util.Arrays;

/**
 * Cryptography-related utilities & constants.
 */
public final class Crypto {
    private Crypto() {}

    // ---------------------------------------------------------------------------------------------

    public static final String BOUNCY_CASTLE_KEY_PROVIDER;
    static {
        // Initializing the field in its declaration doesn't ensure the provider is added,
        // even if it comes after the static block!
        Security.addProvider(new BouncyCastleProvider());
        BOUNCY_CASTLE_KEY_PROVIDER = "BC";
    }

    // ---------------------------------------------------------------------------------------------

    // TODO unused - useful? (inspired from web3j which stores the public key as a BigInteger)

    /**
     * Converts the elliptic curve point into a big integer by getting the uncompressed byte array
     * encoding of the point (as per SEC1 §2.3.3) and creating a positive number from that
     * representation.
     */
    public static BigInteger asInteger (ECPoint point) {
        byte[] bytes = point.getEncoded(false);
        // we strip the first byte which signifies that the representation is uncompressed
        return new BigInteger(1, Arrays.copyOfRange(bytes, 1, bytes.length));
    }

    // ---------------------------------------------------------------------------------------------
}
