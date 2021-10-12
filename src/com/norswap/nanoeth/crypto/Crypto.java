package com.norswap.nanoeth.crypto;

import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.Hashing;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.security.Security;

/**
 * Cryptography-related utilities and constants that do not fit in other classes.
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

    /**
     * Computes a <b>non-perfectly hiding</b> Pedersen commitment to the (non-empty) given vector,
     * using the given basis points.
     * <p>
     * The returned commitment is {@code sum(vector[i] * basis[i])} (cf. {@link
     * Vectors#ecInnerProduct(BigInteger[], ECPoint[])}.
     * <p>
     * The basis may be larger than the vector, in which case only a prefix of it is used.
     * <p>
     * Unlike {@link #pedersenCommitment(BigInteger, BigInteger[], ECPoint[])}, no random value is
     * included, which means that two commitments to the same vector will be the same (which leaks
     * information about the vector). It also makes these commitments unsuitable for vectors of a
     * single element.
     * <p>
     * This matches the {@code compute_commitment_root} in the Verkle EIP draft.
     */
    public static ECPoint pedersenCommitment (BigInteger[] vector, ECPoint[] basis) {
        assert basis.length >= vector.length && basis.length > 0;
        return Vectors.ecInnerProduct(vector, basis);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Computes a Pedersen commitment to the given vector, using the given random value (often
     * called r) and the given basis points (the first of which is often called H).
     * <p>
     * The returned commitment is {@code r * H + sum(vector[i] * numsPoints[i+1])}.
     */
    public static ECPoint pedersenCommitment (BigInteger random, BigInteger[] vector, ECPoint[] basis) {
        assert basis.length >= vector.length + 1;
        var commitment = basis[0].multiply(random); // rH
        for (int i = 0; i < vector.length; i++)
            commitment = commitment.add(basis[i + 1].multiply(vector[i]));
        return commitment;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Generates a set of "nothing-up-my-sleeve" points using the given curve, hashing the
     * compressed encoding of the {@link Curve#G() G point} of the curve to obtain a point X value,
     * iterating the last byte if the value is invalid.
     * <p>
     * If more than one point is required, we use the compressed encoding of the previously generated
     * point as the new base for hashing, and repeat this process.
     * <p>
     * These points have unknown "logarithms" (i.e. the value k such that P = kG, where G is the
     * generator of the curve's subgroup). They can be used as the basis for {@link
     * #pedersenCommitment(BigInteger, BigInteger[], ECPoint[]) Pedersen commitments}.
     */
    public static ECPoint[] nothingUpMySleevePoints (Curve curve, int amount) {
        byte[] base = Hashing.keccak(curve.G().getEncoded(true)).bytes;
        ECPoint[] points = new ECPoint[amount];
        int i = 0;
        while (i < amount) {
            try {
                var num = new Natural(base);
                points[i] = curve.point(num, true); // always pick the odd value, just a convention
                base = Hashing.keccak(points[i].getEncoded(true)).bytes;
                ++i;
            } catch (IllegalArgumentException e) {
                base[base.length - 1] = (byte) (base[base.length - 1] + 1);
                // not a valid x coordinate (about half of coordinates are validate)
                // iterate the base, and retry
            }
        }
        return points;
    }

    // ---------------------------------------------------------------------------------------------
}
