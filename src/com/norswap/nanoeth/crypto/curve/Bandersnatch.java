package com.norswap.nanoeth.crypto.curve;

import com.norswap.nanoeth.crypto.TonelliShanks;
import com.norswap.nanoeth.data.Natural;
import java.math.BigInteger;

import static java.math.BigInteger.ONE;

/**
 * Implementation of the Bandersnatch elliptic curve, notably used in the Verkle tree
 * implementation.
 * <p>
 * This curve is not used in signatures.
 * <p>
 * The Bandersnatch curve can be defined by multiple equations. The preferred equations
 * uses the Twisted Edwards curve model with equation {@code a * x^2 + y^2 == 1 + d * x^2 * y^2}
 * where {@code x, y} are point coordinates and {@link #a} and {@link #d} are constants.
 * <p>
 * Because all occurences of {@code x} and {@code y} in the curve equation are squared, a Twisted
 * Edwards curve is symmetric on both axis. For the purpose of point negation, the y-axis is to be
 * considered (unlike Weierstrass curves, where the x-axis is the only axis symmetry).
 */
public final class Bandersnatch extends Curve<Bandersnatch.Point> {

    // ---------------------------------------------------------------------------------------------

    public static final class Point {
        public final BigInteger x, y, z;
        public Point (BigInteger x, BigInteger y, BigInteger z) {
            this.x = x; this.y = y; this.z = z;
        }
    }

    private static final Point ZERO = new Point(BigInteger.ZERO, ONE, ONE);

    // ---------------------------------------------------------------------------------------------

    /** The a value of the curve equation. */
    public final BigInteger a = BigInteger.valueOf(-5).add(q);
    // hex:     0x73EDA753299D7D483339D80809A1D80553BDA402FFFE5BFEFFFFFFFEFFFFFFFC
    // decimal: 52435875175126190479447740508185965837690552500527637822603658699938581184508

    /** The d value of the curve equation. */
    public final BigInteger d =
        new Natural("0x6389c12633c267cbc66e3bf86be3b6d8cb66677177e54f92b369f2f5188d58e7");
        // decimal: 45022363124591815672509500913686876175488063829319466900776701791074614335719
        // numerator   (decimal): 13882720812614122064902226397295860780
        // denominator (decimal): 171449701953573178309673572579671231137
        // d == numerator / denominator (mod q)

    // ---------------------------------------------------------------------------------------------

    // See accessors with identical names.

    private static final Natural q
        = new Natural("0x73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001");
        // decimal: 52435875175126190479447740508185965837690552500527637822603658699938581184513

    private final static Natural n
         = new Natural("0x1cfb69d4ca675f520cce760202687600ff8f87007419047174fd06b52876e7e1");
        // decimal: 13108968793781547619861935127046491459309155893440570251786403306729687672801

    private final static Natural H
        = new Natural(4);

    private static final Natural N
        = new Natural("0x73eda753299d7d483339d80809a1d803fe3e1c01d06411c5d3f41ad4a1db9f84");
        // decimal: 52435875175126190479447740508185965837236623573762281007145613226918750691204
        // N == n * H

    private final static Point G = new Point(
        new Natural("0x29c132cc2c0b34c5743711777bbe42f32b79c022ad998465e1e71866a252ae18"),
        // decimal: 18886178867200960497001835917649091219057080094937609519140440539760939937304
        new Natural("0x2a6c669eda123e0f157d8b50badcd586358cad81eee464605e3167b6cc974166"),
        // decimal: 19188667384257783945677642223292697773471335439753913231509108946878080696678
        ONE);

    // ---------------------------------------------------------------------------------------------

    /** Used to compute finite fields square roots. */
    private static final TonelliShanks SQRT_CALCULATOR = new TonelliShanks(q);

    // ---------------------------------------------------------------------------------------------

    @Override public BigInteger q() {
        return q;
    }

    @Override public BigInteger N() {
        return N;
    }

    @Override public BigInteger n() {
        return n;
    }

    @Override public BigInteger H() {
        return H;
    }

    @Override public Point G() {
        return G;
    }

    @Override public Point zero() {
        return ZERO;
    }

    @Override public ECSigner signer() {
        return null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public BigInteger getPointX (Point point) { return point.x; }
    @Override public BigInteger getPointY (Point point) { return point.y; }
    @Override public BigInteger getPointZ (Point point) { return point.z; }

    // ---------------------------------------------------------------------------------------------

    private static BigInteger mod (BigInteger x) {
        return (x.compareTo(q) >= 0) ? x.mod(q) : x;
    }

    private static BigInteger square (BigInteger x) {
        return mod(x.multiply(x));
    }

    private static BigInteger sqrt (BigInteger x) {
        // TODO 1. rewrite this to return the smaller root or null
        // TODO 2. add documentation saying that this does not need to be modulus'ed
        var solution = SQRT_CALCULATOR.sqrt(x, q);
        return solution.exists
            ? solution.root1
            : null;
    }

    private static BigInteger add (BigInteger x1, BigInteger x2) {
        return mod(x1.add(x2));
    }

    private static BigInteger sub (BigInteger x1, BigInteger x2) {
        return mod(x1.subtract(x2));
    }

    private static BigInteger mult (BigInteger x1, BigInteger x2) {
        return mod(x1.multiply(x2));
    }

    private static BigInteger mult (BigInteger x1, BigInteger x2, BigInteger x3) {
        return mod(mod(x1.multiply(x2)).multiply(x3));
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean isZero (Point point) {
        return point.x.signum() == 0 && point.y.equals(point.z);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean isValid (Point point) {
        if (isZero(point)) return true;

        // The curve equation is (a * x^2) + y^2 == 1 + (d * x^2 * y^2),
        // the projective version is (a * x^2 * z^2) + (y^2 * z^2) == z^4 + (d * x^2 * y^2)
        // The reason is that in the projective version x = x0 * z, and y = y0 * z (where
        // x0,y0 are the non-projective coordinates of the same point).
        // By substituting these values in the regular equation, we can see that the equality
        // is preserved.

        var x2 = square(point.x);
        var y2 = square(point.y);
        var z2 = square(point.z);
        var left  = add(mult(a, x2, z2), mult(y2, z2));
        var right = add(square(z2), mult(d, x2, y2));
        return left.equals(right);
    }


    // ---------------------------------------------------------------------------------------------

    @Override public Point pointOrNull (BigInteger x) {
        // The curve equation is (a * x^2) + y^2 == 1 + (d * x^2 * y^2),
        // which gives y == sqrt((1 - a * x^2) / (1 - d * x^2))

        x = mod(x);
        var x2  = square(x);
        var num = sub(ONE, mult(a, x2));
        var den = sub(ONE, mult(d, x2));
        var y   = sqrt(num.divide(den));

        return y != null ? point(x, y) : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Point point (BigInteger x, BigInteger y, BigInteger z) {
        return new Point(mod(x), mod(y), mod(z));
    }

    // ---------------------------------------------------------------------------------------------
}
