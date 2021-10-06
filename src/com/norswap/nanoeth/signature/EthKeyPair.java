package com.norswap.nanoeth.signature;

import com.norswap.nanoeth.data.Address;
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

import static com.norswap.nanoeth.crypto.Curve.SECP256K1;
import static com.norswap.nanoeth.signature.Signature.recoverPublicKeyWithoutHashing;

/**
 * A SECP-256k1 key pair that can be used to sign transactions.
 */
public final class EthKeyPair {

    // ---------------------------------------------------------------------------------------------

    private final BigInteger privateKey;
    public final ECPoint publicKey;

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

    /** Create a key pair from the hex string representation (e.g. 0x123) of the private key. */
    public EthKeyPair (String privateKey) {
        this.privateKey = new Natural(privateKey);
        this.publicKey = publicKeyFromPrivateKey(this.privateKey);
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
     * Sign the Keccak hash of the given message (an arbitrary byte sequence) with ECDSA using the
     * private key.
     */
    public Signature sign (byte[] message) {
        byte[] hash = Hashing.keccak(message).bytes;
        return signWithoutHashing(hash);
    }

    // ---------------------------------------------------------------------------------------------

    /** In Ethereum, the {@code message} will always be a hash. */
    public Signature signWithoutHashing (byte[] message) {
        BigInteger[] components = SECP256K1.sign(privateKey, message);
        var r = new Natural(components[0]);
        var s = SignatureUtils.canonicalizeS(new Natural(components[1]));

        // See signature package README to understand what the recovery ID is and its relationship
        // to "y parity".
        int recoveryId = findYParity(message, r, s);

        try {
            return recoveryId >= 0
                ? new Signature(recoveryId, r, s)
                : signWithoutHashingWithRandomK(message);
        } catch (IllegalSignature e) {
            throw new Error("implementation bug", e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Given that {@code (x,y)} is the public key (elliptic curve point) associated with the
     * signature, this method returns the found y parity (0 or 1 for even or odd, respectively) for
     * the message with the given {@code r} and {@code s} value; or -1 in the very rare case where
     * {@code n <= x < q}. See signature package README for more information.
     */
    private int findYParity (byte[] message, Natural r, Natural s) {
        for (int recoveryId = 0; recoveryId < 4; ++recoveryId)
            if (publicKey.equals(recoverPublicKeyWithoutHashing(recoveryId, message, r, s)))
                return recoveryId < 2 ? recoveryId : -1;
        throw new Error("implementation error: invalid (r,s) signature for message");
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Same as {@link #signWithoutHashing} but uses a random k in the signature scheme, to handle
     * the 1/10^36 probability case where (r, s) is rejected (see signature package README).
     */
    private Signature signWithoutHashingWithRandomK (byte[] message) throws IllegalSignature {

        System.err.println("This event should only happen with 1/10^36 probability. " +
                "In all likelihood, something went wrong in the implementation.");

        while (true) {
            BigInteger[] components = SECP256K1.signWithRandomK(privateKey, message);
            var r = new Natural(components[0]);
            var s = new Natural(components[1]);
            int recoveryId = findYParity(message, r, s);
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
        return SignatureUtils.address(publicKey);
    }

    // ---------------------------------------------------------------------------------------------
}
