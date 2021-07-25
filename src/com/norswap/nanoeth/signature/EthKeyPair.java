package com.norswap.nanoeth.signature;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Bytes;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.Crypto;
import com.norswap.nanoeth.utils.Hashing;
import com.norswap.nanoeth.utils.Randomness;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import static com.norswap.nanoeth.signature.Curve.SECP256K1;
import static com.norswap.nanoeth.signature.Signature.recoverPublicKey;

/**
 * A SECP-256k1 key pair that can be used to sign transactions.
 */
public final class EthKeyPair {

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the address of the Ethereum account associated with the key pair, which is formed
     * by the 20 rightmost byte of the public key's encoding.
     */
    public static Address address (ECPoint publicKey) {
        byte[] bytes = publicKey.getEncoded(false);
        // +1 because of the first byte which signifies that the representation is uncompressed
        return new Address(Arrays.copyOfRange(bytes, 13, 33));
    }

    // ---------------------------------------------------------------------------------------------

    private final BigInteger privateKey;
    private final ECPoint publicKey;

    // ---------------------------------------------------------------------------------------------

    /** Generate a new key pair. */
    public EthKeyPair() {
        KeyPair pair;
        try {
            var gen = KeyPairGenerator.getInstance("ECDSA", Crypto.BOUNCY_CASTLE_KEY_PROVIDER);
            gen.initialize(new ECGenParameterSpec("secp256k1"), Randomness.SECURE);
            pair = gen.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new Error(e);
        }
        this.publicKey  = ((BCECPublicKey)  pair.getPublic ()).getQ().normalize();
        this.privateKey = ((BCECPrivateKey) pair.getPrivate()).getD();
    }

    // ---------------------------------------------------------------------------------------------

    /** Create a key pair from an existing public and private key. */
    public EthKeyPair (ECPoint publicKey, BigInteger privateKey) {
        this.publicKey  = publicKey.normalize();
        this.privateKey = privateKey;
    }

    // ---------------------------------------------------------------------------------------------

    /** Create a key pair from an existing private key (regenerating the public key). */
    public EthKeyPair (BigInteger privateKey) {
        this.publicKey = publicKeyFromPrivateKey(privateKey);
        this.privateKey = privateKey;
    }

    // ---------------------------------------------------------------------------------------------

    private static ECPoint publicKeyFromPrivateKey (BigInteger privateKey) {
        // FixedPointCombMultiplier currently doesn't support scalars longer than the group order.
        // Because the subgroup of G has order n, this doesn't change the result.
        if (privateKey.bitLength() > SECP256K1.n.bitLength())
            privateKey = privateKey.mod(SECP256K1.n);
        return new FixedPointCombMultiplier().multiply(SECP256K1.G, privateKey);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Sign the Keccak hash of the given message (an arbitrary byte sequence) using the private key.
     */
    public Signature sign (Bytes message) {
        byte[] hash = Hashing.keccak(message).bytes;
        return signWithoutHashing(hash);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Sign the Keccak hash of the given message (an arbitrary byte sequence) using the private key.
     */
    public Signature sign (byte[] message) {
        byte[] hash = Hashing.keccak(message).bytes;
        return signWithoutHashing(hash);
    }

    // ---------------------------------------------------------------------------------------------

    /** Sign a byte sequence with ECDSA without hasing the given message. */
    private Signature signWithoutHashing (byte[] message) {
        BigInteger[] components = SECP256K1.sign(privateKey, message);
        var r = new Natural(components[0]);
        var s = new Natural(components[1]);
        int recoveryId = findRecoveryId(message, r, s);
        return recoveryId >= 0
            ? new Signature(recoveryId, r, s)
            : signWithoutHashingWithRandomK(message);
    }

    // ---------------------------------------------------------------------------------------------

    /** See signature package README to understand what the recovery ID is. */
    private int findRecoveryId (byte[] message, Natural r, Natural s) {
        // Ethereum reject super rare cases where n <= x < q.
        // See signature package README.
        for (int recoveryId = 0; recoveryId < 4; ++recoveryId)
            if (publicKey.equals(recoverPublicKey(recoveryId, message, r, s)))
                return recoveryId < 2 ? recoveryId : -1;
        throw new Error("implementation error: invalid (r,s) signature for message");
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Same as {@link #signWithoutHashing} but uses a random k in the signature scheme, to handle
     * the 1/10^36 probability case where (r, s) is rejected (see signature package README).
     */
    private Signature signWithoutHashingWithRandomK (byte[] message) {
        while (true) {
            BigInteger[] components = SECP256K1.signWithRandomK(privateKey, message);
            var r = new Natural(components[0]);
            var s = new Natural(components[1]);
            int recoveryId = findRecoveryId(message, r, s);
            if (recoveryId >= 0)
                return new Signature(recoveryId, r, s);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the address of the Ethereum account associated with the key pair, which is formed
     * by the 20 rightmost byte of the public key's encoding.
     */
    public Address address() {
        return address(publicKey);
    }

    // ---------------------------------------------------------------------------------------------
}
