package com.norswap.nanoeth.crypto;

import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Utility functions dealing with vectors of large integers and elliptic curve points.
 */
public final class Vectors {
    private Vectors() {}

    // =============================================================================================
    // region GENERIC
    // =============================================================================================

    /** Returns the left half of the vector. If the size is odd, the right half will be bigger. */
    public static <T> T[] left (T[] vector) {
        return Arrays.copyOfRange(vector, 0, vector.length / 2);
    }

    /** Returns the right half of the vector. If the size is odd, the right half will be bigger. */
    public static <T> T[] right (T[] array) {
        return Arrays.copyOfRange(array, array.length / 2, array.length);
    }

    // endregion
    // =============================================================================================
    // region INTEGERS
    // =============================================================================================

    /** Returns the sum of two vectors of integers. */
    public static BigInteger[] sum (BigInteger[] a, BigInteger[] b) {
        assert a.length == b.length;
        var sum = new BigInteger[a.length];
        for (int i = 0; i < sum.length; i++)
            sum[i] = a[i].add(b[i]);
        return sum;
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the product of a vector of integers by an integer scalar. */
    public static BigInteger[] product (BigInteger[] a, BigInteger x) {
        var sum = new BigInteger[a.length];
        for (int i = 0; i < sum.length; i++)
            sum[i] = a[i].multiply(x);
        return sum;
    }

    // ---------------------------------------------------------------------------------------------

    /** Computes the inner product (aka dot product or scalar product) or two vectors of integers. */
    public static BigInteger innerProduct (BigInteger[] a, BigInteger[] b) {
        assert a.length == b.length;
        var out = BigInteger.ZERO;
        for (int i = 0; i < a.length; i++)
            out = out.add(a[i].multiply(b[i]));
        return out;
    }

    // endregion
    // =============================================================================================
    // region ELLIPTIC CURVE POINTS
    // =============================================================================================

    /** Returns the product of a vector of elliptic curve points by an integer scalar. */
    public static ECPoint[] product (ECPoint[] a, BigInteger x) {
        var sum = new ECPoint[a.length];
        for (int i = 0; i < sum.length; i++)
            sum[i] = a[i].multiply(x);
        return sum;
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the sum of two identically-sized vectors of elliptic curve points. */
    public static ECPoint[] sum (ECPoint[] a, ECPoint[] b) {
        assert a.length == b.length;
        var sum = new ECPoint[a.length];
        for (int i = 0; i < sum.length; i++)
            sum[i] = a[i].add(b[i]);
        return sum;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Computes the "inner product" (aka dot product or scalar product) or a vector of scalars and a
     * vector of elliptic curve points. The vectors may not be empty but may have different sizes,
     * in which case the inner product will be computed on a prefix with the minimum of both sizes.
     */
    public static ECPoint ecInnerProduct (BigInteger[] a, ECPoint[] g) {
        var len = Math.min(a.length, g.length);
        assert len > 0;
        var out = g[0].multiply(a[0]);
        for (int i = 1; i < a.length; i++)
            out = out.add(g[i].multiply(a[i]));
        return out;
    }

    // endregion
    // =============================================================================================
}
