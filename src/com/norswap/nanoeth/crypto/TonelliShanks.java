package com.norswap.nanoeth.crypto;

import java.math.BigInteger;

import static java.math.BigInteger.*;

/**
 * Implementation of the Tonelli-Shanks algorithm for finding the square roots of a prime finite
 * field element.
 * <p>
 * Source: https://rosettacode.org/wiki/Tonelli-Shanks_algorithm#Java
 * <p>
 * Explanation of the algorithm: https://en.wikipedia.org/wiki/Tonelli%E2%80%93Shanks_algorithm
 */
public class TonelliShanks {

    // ---------------------------------------------------------------------------------------------

    /** Prime modulus. */
    public final BigInteger p;

    // ---------------------------------------------------------------------------------------------

    // Precompute constants.

    private static final BigInteger FOUR = BigInteger.valueOf(4);
    private final BigInteger Psub1;
    private final BigInteger Psub1div2;

    // ---------------------------------------------------------------------------------------------

    public TonelliShanks (BigInteger p) {
        this.p = p;
        this.Psub1 = p.subtract(ONE);
        this.Psub1div2 = Psub1.divide(TWO);
    }

    // ---------------------------------------------------------------------------------------------

    public static class Solution {
        public final BigInteger root1;
        public final BigInteger root2;
        public final boolean exists;

        Solution(BigInteger root1, BigInteger root2, boolean exists) {
            this.root1 = root1;
            this.root2 = root2;
            this.exists = exists;
        }
    }

    // ---------------------------------------------------------------------------------------------

    // TODO legendre symbol + behaviour at 0
    /** Returns 1 if {@code a} admits a square root, -1 (mod p) otherwise. */
    private BigInteger eulerCriterion (BigInteger a) {
        return a.modPow(Psub1div2, p);
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns {@code a^e (mod p)}. */
    private BigInteger powModP(BigInteger a, BigInteger e) {
        return a.modPow(e, p);
    }

    // ---------------------------------------------------------------------------------------------

    public Solution sqrt (final BigInteger n, final BigInteger p) {

        if (!eulerCriterion(n).equals(ONE))
            return new Solution(ZERO, ZERO, false);

        // Repeatedly divide (p-1) by 2.
        BigInteger q = Psub1;
        BigInteger twoExponent = ZERO;
        while (q.and(ONE).equals(ZERO)) {
            twoExponent = twoExponent.add(ONE);
            q = q.shiftRight(1);
        }

        if (twoExponent.equals(ONE)) { // if (p-1) = 2q
            // sqrt(n) = n^((p+1)/4))
            // n = n^((p+1)/2)
            // n = n^((p-1+2)/2)
            // n = n^((2q+2)/2)
            // n = n^(2q/2) n^(2/2)
            // n = n^q n                (*)
            // n = n

            // (*) because (p-1) = 2q, we have
            // n^2q = n^(p-1) = 1
            // n^q = sqrt(n^2q) = sqrt(1) = 1
            // (i.e. n is a q-th root of unity)
            // I found https://crypto.stackexchange.com/questions/63614/ useful in thinking about this

            BigInteger r1 = powModP(n, p.add(ONE).divide(FOUR));
            return new Solution(r1, p.subtract(r1), true);
        }

        BigInteger z = TWO;
        while (!eulerCriterion(z).equals(Psub1)) z = z.add(ONE);
        BigInteger c = powModP(z, q);
        BigInteger r = powModP(n, q.add(ONE).divide(TWO));
        BigInteger t = powModP(n, q);
        BigInteger m = twoExponent;

        while (true) {
            if (t.equals(ONE)) return new Solution(r, p.subtract(r), true);
            BigInteger i = ZERO;
            BigInteger zz = t;
            while (!zz.equals(BigInteger.ONE) && i.compareTo(m.subtract(ONE)) < 0) {
                zz = zz.multiply(zz).mod(p);
                i = i.add(ONE);
            }
            BigInteger b = c;
            BigInteger e = m.subtract(i).subtract(ONE);
            while (e.compareTo(ZERO) > 0) {
                b = b.multiply(b).mod(p);
                e = e.subtract(ONE);
            }
            r = r.multiply(b).mod(p);
            c = b.multiply(b).mod(p);
            t = t.multiply(c).mod(p);
            m = i;
        }
    }

    // ---------------------------------------------------------------------------------------------
}