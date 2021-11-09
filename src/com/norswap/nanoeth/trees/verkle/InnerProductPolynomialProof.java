package com.norswap.nanoeth.trees.verkle;

import com.norswap.nanoeth.crypto.Curve;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.trees.patricia.PatriciaTree;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.utils.Hashing;
import com.norswap.nanoeth.utils.MathUtils;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.util.Arrays;

import static com.norswap.nanoeth.crypto.Vectors.*;
import static com.norswap.nanoeth.utils.Utils.array;

/**
 * TODO
 */
public class InnerProductPolynomialProof {

    // ---------------------------------------------------------------------------------------------

    /** The elliptic curve used for the proof. This is public information. */
    public final Curve curve;

    /** The vector of basis points used for the proof. This is public information. */
    public final ECPoint[] basis;

    /** The length of the vectors this proof commits to. */
    public final int length;

    /** The commitment to the product of the two vectors. */
    public final ECPoint initialCommitment;

    /** Commitments to the left side of derived vectors, necessary to verify the proof. */
    public final ECPoint[] leftCommitments;

    /** Commitments to the right side of derived vectors, necessary to verify the proof. */
    public final ECPoint[] rightCommitments;

    /** Final "iteration" of the first vector, necessary to verify the proof. */
    public final BigInteger a;

    /** Final "iteration" of the second vector, necessary to verify the proof. */
    public final BigInteger b;

    // ---------------------------------------------------------------------------------------------

    /**
     * Checks that the given length of the g and h basis vectors is compatible with our basis, and
     * that it is a power of two.
     */
    private static boolean isValidLength (ECPoint[] basis, int length) {
        return basis.length >= length * 2 + 1 && MathUtils.isPowerOf2(length) && length != 0;
    }

    // ---------------------------------------------------------------------------------------------

    /** Computes C = ab + gh + abq, the commitment to the inner product "ab".  */
    private static ECPoint computeInnerProductCommitment
        (BigInteger[] a, BigInteger[] b, ECPoint[] g, ECPoint[] h, ECPoint q) {

        ECPoint ag  = ecInnerProduct(a, g);
        ECPoint bh  = ecInnerProduct(b, h);
        ECPoint abq = q.multiply(innerProduct(a, b));
        return ag.add(bh).add(abq);
    }

    // ---------------------------------------------------------------------------------------------

    public InnerProductPolynomialProof (Curve curve, ECPoint[] basis, BigInteger[] a, BigInteger[] b) {

        assert a.length == b.length && isValidLength(basis, a.length);

        this.curve = curve;
        this.basis = basis;
        this.length = a.length;

        ECPoint[] g = g(length);
        ECPoint[] h = h(length);
        final ECPoint q = q(length);

        // in what follows: C = commitment, L = left, R = right

        ECPoint C = computeInnerProductCommitment(a, b, g, h, q);
        this.initialCommitment = C;

        final int log = MathUtils.log2(a.length);
        this.leftCommitments  = new ECPoint[log];
        this.rightCommitments = new ECPoint[log];

        for (int i = 0; i < log; i++) {
            assert a.length > 1;

            BigInteger[] aL = left(a); BigInteger[] aR = right(a);
            BigInteger[] bL = left(b); BigInteger[] bR = right(b);

            ECPoint[] gL = left(g); ECPoint[] gR = right(g);
            ECPoint[] hL = left(h); ECPoint[] hR = right(h);

            BigInteger zL = innerProduct(aR, bL);
            BigInteger zR = innerProduct(aL, bR);

            ECPoint aRgL = ecInnerProduct(aR, gL);
            ECPoint bLhR = ecInnerProduct(bL, hR);
            ECPoint CL   = aRgL.add(bLhR).add(q.multiply(zL));

            ECPoint aLgR = ecInnerProduct(aL, gR);
            ECPoint bRhL = ecInnerProduct(bR, hL);
            ECPoint CR   = aLgR.add(bRhL).add(q.multiply(zR));

            this.leftCommitments  [i] = CL;
            this.rightCommitments [i] = CR;

            BigInteger challenge = computeChallenge(CL, CR);
            BigInteger challengeInverse = challenge.modInverse(curve.n());

            a = sum(aL, product(aR, challenge));
            b = sum(bL, product(bR, challengeInverse));

            g = sum(gL, product(gR, challengeInverse));
            h = sum(hL, product(hR, challenge));

            C = C.add(CL.multiply(challenge)).add(CR.multiply(challengeInverse));

            // verify invariant "C == ag + bh + abq"
            assert C.equals(computeInnerProductCommitment(a, b, g, h, q));
        }

        // record final values
        this.a = a[0];
        this.b = b[0];
    }

    // ---------------------------------------------------------------------------------------------

    private BigInteger barycentricA (BigInteger z, int d) {
        return z.pow(d).subtract(Natural.ONE);
    }

    private BigInteger barycentricADerivative (BigInteger z, int d) {
        return new Natural(d).multiply(z.pow(d-1));
    }

    private BigInteger bi (BigInteger z, BigInteger xi, int d) {
        // TODO can be simplified by passing i and assuming xi = w^i where w is a root of unity
        var q = curve.q();
        var Az = barycentricA(z, d); // A(z)
        var Apxi_1 = barycentricADerivative(xi, d).modInverse(q); // 1 / A'(xi)
        var sub_1 = z.subtract(xi).modInverse(q); // 1 / (z - xi)
        return Az.multiply(Apxi_1).multiply(sub_1).mod(q);
    }

    private BigInteger[] rootsOfUnity (int n, int d) {
        assert MathUtils.isPowerOf2(d);
        final var q = curve.q();
        var bytes = PatriciaTree.EMPTY_TREE_ROOT.bytes.clone();
        BigInteger candidate;
        while (true) {
            candidate = new Natural(bytes);
            var power = q.subtract(new Natural(1)).multiply(new Natural(d).modInverse(q));
            var g = candidate.modPow(power, q);
            var check = g.pow(d / 2).mod(q);
            if (!check.equals(Natural.ONE))
                break;
            bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] + 1);
        }
        var roots = new BigInteger[n];
        roots[0] = candidate;
        for (int i = 1; i < roots.length; i++)
            roots[i] = roots[i - 1].multiply(candidate).mod(q);
        return roots;
        // TODO test this
    }

    // ---------------------------------------------------------------------------------------------

    /** Return the basis vector g of the given length. */
    private ECPoint[] g (int length) {
        return Arrays.copyOfRange(basis, 0, length);
    }

    /** Return the basis vector h of the given length. */
    private ECPoint[] h (int length) {
        return Arrays.copyOfRange(basis, length, 2 * length);
    }

    /** Return the basis point q, given the length of g and h. */
    private ECPoint q (int length) {
        return basis[2 * length];
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Return a challenge derived from the left and right commitment via hashing (fiat-shamir
     * heuristic).
     * <p>
     * The returned integer is a member of the field of the elliptic curve.
     */
    private BigInteger computeChallenge (ECPoint CL, ECPoint CR) {
        var encodedCLCR = ByteUtils.concat(CL.getEncoded(true), CR.getEncoded(true));
        return new Natural(Hashing.keccak(encodedCLCR).bytes).mod(curve.n());
    }

    // ---------------------------------------------------------------------------------------------

    public boolean verify () {
        ECPoint C = initialCommitment;

        // TODO it should be possible to only compute g and h at the end
        //    see https://dankradfeist.de/ethereum/cryptography/2021/07/27/inner-product-arguments.html
        var g = g(length);
        var h = h(length);

        for (int i = 0; i < leftCommitments.length; i++) {
            ECPoint CL = leftCommitments[i];
            ECPoint CR = rightCommitments[i];
            BigInteger challenge = computeChallenge(CL, CR);
            BigInteger challengeInverse = challenge.modInverse(curve.n());

            C = C.add(CL.multiply(challenge)).add(CR.multiply(challengeInverse));

            ECPoint[] gL = left(g); ECPoint[] gR = right(g);
            ECPoint[] hL = left(h); ECPoint[] hR = right(h);

            g = sum(gL, product(gR, challengeInverse));
            h = sum(hL, product(hR, challenge));
        }

        assert g.length == 1 && h.length == 1;
        final var q = q(length);
        final var D = computeInnerProductCommitment(array(a), array(b), g, h, q);
        return C.equals(D);
    }

    // ---------------------------------------------------------------------------------------------
}
