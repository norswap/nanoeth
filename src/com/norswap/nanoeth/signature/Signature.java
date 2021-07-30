package com.norswap.nanoeth.signature;

import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.Hashing;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.util.Objects;

import static com.norswap.nanoeth.versions.EthereumVersion.HOMESTEAD;
import static com.norswap.nanoeth.signature.Curve.SECP256K1;

/**
 * An ECDSA signature using the SECP-256k1 curve.
 */
public final class Signature
{
    // ---------------------------------------------------------------------------------------------

    private static final BigInteger HALF_N =
        SECP256K1.n.shiftRight(1); // n/2

    // ---------------------------------------------------------------------------------------------

    /**
     * The y Parity that specifies which key generated this signature (as there are two possible
     * keys). This is also sometimes called "recovery ID" as it allows recovering the public key
     * from the signature. See the signature package README for more information.
     *
     * <p>Will be 1 if y is odd, 0 if even.
     */
    public final byte yParity;

    public final Natural r;
    public final Natural s;

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a new signature with the given {@code yParity}, {@code r} and {@code s} values.
     * {@code s} must be in its canonical form (see {@link #canonicalizeS(Natural)}).
     */
    public Signature (int yParity, Natural r, Natural s) throws IllegalSignature {
        if (r.signum() < 0)
            throw new IllegalSignature("r must be positive");
        if (s.signum() < 0)
            throw new IllegalSignature("s must be positive");
         if (HOMESTEAD.isPast() && s.compareTo(HALF_N) > 0)
             throw new IllegalSignature("s must be <= n/2 to avoid signature malleability");
        if (yParity != 0 && yParity != 1)
            throw new IllegalSignature("y Parity (used to build v) must be in [0,1]");

        this.yParity = (byte) yParity;
        this.r = r;
        this.s = s;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Canonicalizes the {@code s} signature value if needed.
     *
     * <p>Canonicalization is required because for every signature (r,s) the signature (r, -s (mod
     * n)) is a valid signature of the same message. See signature package README for more
     * information.
     *
     * <p>This change was introduced in EIP-2 (Homestead). We apply it to all transactions, since
     * canonicalized transactions are valid before Homestead as well.
     */
    static Natural canonicalizeS (Natural s) {
        return s.compareTo(HALF_N) > 0
            ? new Natural(SECP256K1.n.subtract(s))
            : s;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true only if this is a valid signature for Keccak hash of the given message.
     * <p>The public key is automatically recovered from the signature and the message.
     */
    public boolean verify (byte[] message) {
        byte[] hash = Hashing.keccak(message).bytes;
        ECPoint publicKey = recoverPublicKeyWithoutHashing(yParity, hash, r, s);
        return publicKey != null && verifyWithoutHashing(publicKey, hash);
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns true only if this is a valid signature for Keccak hash of the given message. */
    public boolean verify (ECPoint publicKey, byte[] message) {
        return verifyWithoutHashing(publicKey, Hashing.keccak(message).bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /** In Ethereum, the message will always be a hash. */
    private boolean verifyWithoutHashing (ECPoint publicKey, byte[] message) {
        return SECP256K1.verify(publicKey, r, s, message);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Recover the public key from the signature of the hash of the given message, as specified in
     * SEC1 §4.1.6.
     * <p>This is static because we need to call this to find the recovery ID, which is needed
     * to create an instance of {@link Signature}.
     */
    public static ECPoint recoverPublicKey (int recoveryId, byte[] message, BigInteger r, BigInteger s) {
        return recoverPublicKeyWithoutHashing(recoveryId, Hashing.keccak(message).bytes, r, s);
    }

    // ---------------------------------------------------------------------------------------------

    /** In Ethereum, the {@code message} will always be a hash. */
    static ECPoint recoverPublicKeyWithoutHashing
            (int recoveryId, byte[] message, BigInteger r, BigInteger s) {

        // Note that we must handle recoveryId in [0, 3] and not just [0, 1]!

        // r could have been generated both by x, or by x + n (mod q).
        // See signature package README.
        BigInteger i = BigInteger.valueOf(recoveryId / 2); // 0 or 1
        BigInteger x = r.add(i.multiply(SECP256K1.n));

        if (x.compareTo(SECP256K1.q) >= 0)
            // Cannot have x coordinate larger than the field prime q.
            return null;

        // Compressed keys require you to know an extra bit of data about the y-coordinate (as there
        // are two possibilities), which is encoded as the parity of y.
        boolean yOdd = (recoveryId & 1) == 1;
        // R is called P in the README and in the intro articles.
        ECPoint R = SECP256K1.point(x, yOdd);

        if (!R.multiply(SECP256K1.n).isInfinity())
            // nR should = 0 (the point at infinity), because R is a point in the cyclic subgroup
            // generated by G, whose order is n.
            return null;

        // ECDSA assumes that the message is a hash, but the scheme is still usable if it isn't.
        BigInteger e = new BigInteger(1, message);

        // Compute a candidate private key Q = mi(r) * (sR - eG),
        // where mi is the modular inverse.
        BigInteger minus_e = BigInteger.ZERO.subtract(e).mod(SECP256K1.n);
        BigInteger r_inverse = r.modInverse(SECP256K1.n);

        return ECAlgorithms.sumOfTwoMultiplies(
            R,           r_inverse.multiply(s)       .mod(SECP256K1.n),
            SECP256K1.G, r_inverse.multiply(minus_e) .mod(SECP256K1.n)).normalize();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof Signature)) return false;
        Signature signature = (Signature) o;
        return yParity == signature.yParity && r.equals(signature.r) && s.equals(signature.s);
    }

    @Override public int hashCode () {
        return Objects.hash(yParity, r, s);
    }

    @Override public String toString() {
        return String.format("Signature{yParity: %d, r: %s, s: %s}",
            yParity, r.toHexString(), s.toHexString());
    }

    // ---------------------------------------------------------------------------------------------
}
