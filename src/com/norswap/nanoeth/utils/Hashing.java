package com.norswap.nanoeth.utils;

import com.norswap.nanoeth.data.Bytes;
import com.norswap.nanoeth.data.Hash;
import org.bouncycastle.jcajce.provider.digest.Keccak;

/**
 * Utilities to calculate hash values.
 */
public final class Hashing
{
    private Hashing () {}

    // ---------------------------------------------------------------------------------------------

    /** Return the Keccak (SHA-3 draft) hash of the input. */
    public static Hash keccak (byte[] input) {
        Keccak.DigestKeccak kecc = new Keccak.Digest256();
        kecc.update(input, 0, input.length);
        return new Hash(kecc.digest());
    }

    // ---------------------------------------------------------------------------------------------

    /** Return the Keccak (SHA-3 draft) hash of the input. */
    public static Hash keccak (Bytes input) {
        Keccak.DigestKeccak kecc = new Keccak.Digest256();
        kecc.update(input.storage, 0, input.size());
        return new Hash(kecc.digest());
    }

    // ---------------------------------------------------------------------------------------------
}