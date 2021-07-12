package com.norswap.nanoeth.utils;

import org.bouncycastle.jcajce.provider.digest.Keccak;

/**
 * Utilities to calculate hash values.
 */
public final class Hashing
{
    private Hashing () {}

    // ---------------------------------------------------------------------------------------------

    /** Return the Keccak (SHA-3 draft) hash of the input. */
    public static byte[] keccak (byte[] input) {
        Keccak.DigestKeccak kecc = new Keccak.Digest256();
        kecc.update(input, 0, input.length);
        return kecc.digest();
    }

    // ---------------------------------------------------------------------------------------------
}