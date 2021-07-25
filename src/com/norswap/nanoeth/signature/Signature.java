package com.norswap.nanoeth.signature;

import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.Hashing;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.util.Objects;

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
     * The recovery ID that specifies which key generated this signature (as there are two possible
     * keys). Called "recovery ID" as it allows recovering the public key from the signature. See
     * the signature package README for more information.
     */
    public final byte recoveryId;

    public final Natural r;
    public final Natural s;

    // ---------------------------------------------------------------------------------------------

    // TODO: When used to validate new transactions, must reject signatures instead of
    //       canonicalizing them.

    /**
     * Creates a new signature with the given r and s values. Note that s might be canonicalized
     * before being stored into the {@link #s} field.
     *
     * <p>Canonicalization occurs because for every signature (r,s) the signature (r, -s (mod n)) is
     * a valid signature of the same message. See signature package README for more information.
     */
    public Signature (int recoveryId, Natural r, Natural s) {
        if (s.compareTo(HALF_N) > 0)
            s = new Natural(SECP256K1.n.subtract(s));
        Assert.that(r.signum() >= 0, "r must be positive");
        Assert.that(s.signum() >= 0, "s must be positive");
        Assert.that(0 <= recoveryId && recoveryId < 4, "recovery ID (v) must be in [0,3]");
        this.recoveryId = (byte) recoveryId;
        this.r = r;
        this.s = s;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Create a signature from the v, r and s values published on the blockchain. Compared to the
     * usual constructor, this involves parsing the v value to obtain the recovery ID.
     */
    public static Signature fromVRS (BigInteger v, Natural r, Natural s) {
        int vi = v.intValue();
        int recoveryId;
        // check signature package README for details
        if (vi == 27 || vi == 28)
            recoveryId =  vi - 27;
        else if (vi > 37)
            recoveryId = (vi - 35) / 2;
        else
            throw new AssertionError("invalid v value: " + vi);
        return new Signature(recoveryId, r, s);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true only if this is a valid signature for Keccak hash of the given message.
     * <p>The public key is automatically recovered from the signature and the message.
     */
    public boolean verify (byte[] message) {
        byte[] hash = Hashing.keccak(message).bytes;
        ECPoint publicKey = recoverPublicKey(recoveryId, hash, r, s);
        return publicKey != null && verifyWithoutHashing(publicKey, hash);
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns true only if this is a valid signature for Keccak hash of the given message. */
    public boolean verify (ECPoint publicKey, byte[] message) {
        return verifyWithoutHashing(publicKey, Hashing.keccak(message).bytes);
    }

    // ---------------------------------------------------------------------------------------------

    private boolean verifyWithoutHashing (ECPoint publicKey, byte[] message) {
        return SECP256K1.verify(publicKey, r, s, message);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Recover the public key from the signature, as specified in SEC1 §4.1.6.
     * <p>This is static because we need to call this to find the recovery ID, which is needed
     * to create an instance of {@link Signature}.
     */
    public static ECPoint recoverPublicKey
            (int recoveryId, byte[] message, BigInteger r, BigInteger s) {

        // r could have been generated both by x, or by x + n (mod q).
        // See signature package README.
        BigInteger i = BigInteger.valueOf(recoveryId / 2); // 0 or 1
        BigInteger x = r.add(i.multiply(SECP256K1.n));

        if (x.compareTo(SECP256K1.q) >= 0)
            // Cannot have x coordinate larger than the field prime q.
            return null;

        // Compressed keys require you to know an extra bit of data about the y-coordinate (as there
        // are two possibilities), which is encoded in the recovery id as the oddness of y.
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
        return recoveryId == signature.recoveryId && r.equals(signature.r) && s.equals(signature.s);
    }

    @Override public int hashCode () {
        return Objects.hash(recoveryId, r, s);
    }

    @Override public String toString() {
        return String.format("recoveryId: %d, r: %s, d: %s", recoveryId, r, s);
    }

    // ---------------------------------------------------------------------------------------------
}
