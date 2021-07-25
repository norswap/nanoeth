package com.norswap.nanoeth.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

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
}
