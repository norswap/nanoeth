package com.norswap.nanoeth.crypto;

import com.norswap.nanoeth.crypto.TonelliShanks.Solution;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.Randomness;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.field.FiniteField;
import org.bouncycastle.math.field.FiniteFields;
import java.math.BigInteger;
import java.security.SecureRandom;

import static com.norswap.nanoeth.utils.Utils.array;

/**
 * TODO
 * <p>
 * The Bandersnatch curve can be defined by multiple equations. The preferred equations
 * uses the Twisted Edwards curve model with equation {@code a * x^2 + y^2 == 1 + d * x^2 * y^2}
 * where {@code x, y} are point coordinates and {@link #a} and {@link #d} are constants.
 * <p>
 * Because all occurences of {@code x} and {@code y} in the curve equation are squared, a Twisted
 * Edwards curve is symmetric on both axis. For the purpose of point negation, the y-axis is to be
 * considered (unlike Weierstrass curves, where the x-axis is the only axis symmetry).
 */
public final class Bandersnatch extends Curve {

    // ---------------------------------------------------------------------------------------------

    /** Size of the finite field used by the curve. */
    private static final BigInteger q
        = new Natural("0x73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001");
        // decimal: 52435875175126190479447740508185965837690552500527637822603658699938581184513

    /** Parameter {@code a} in the Twisted Edwards curve equation. */
    private static final BigInteger a
        = BigInteger.valueOf(-5).add(q);
        // hex:     0x73EDA753299D7D483339D80809A1D80553BDA402FFFE5BFEFFFFFFFEFFFFFFFC
        // decimal: 52435875175126190479447740508185965837690552500527637822603658699938581184508

    /** Parameter {@code d} in the Twisted Edwards curve equation. */
    private static final BigInteger d
        = new Natural("0x6389c12633c267cbc66e3bf86be3b6d8cb66677177e54f92b369f2f5188d58e7");
        // decimal: 45022363124591815672509500913686876175488063829319466900776701791074614335719
        // numerator   (decimal): 13882720812614122064902226397295860780
        // denominator (decimal): 171449701953573178309673572579671231137
        // d == numerator / denominator (mod q)

    /** cf. {@link Curve#n()}. */
    private static final Natural n = new Natural(
        "0x1cfb69d4ca675f520cce760202687600ff8f87007419047174fd06b52876e7e1");
        // decimal: 13108968793781547619861935127046491459309155893440570251786403306729687672801

    /** cf. {@link Curve#H() }. */
    private static final Natural H = new Natural(4);

    /** {@link #a}, as a {@link ECFieldElement} */
    private static final ECFieldElement fieldA = fieldElement(a);

    /** {@link #d}, as a {@link ECFieldElement} */
    private static final ECFieldElement fieldD = fieldElement(d);

    /** {@link #n} as a {@link ECFieldElement} */
    private static final ECFieldElement fieldN = new BandersnatchFieldElement(n);

    private static final ECFieldElement ONE = fieldElement(BigInteger.ONE);

    private static final TonelliShanks SQRT_CALC = new TonelliShanks(q);

    // ---------------------------------------------------------------------------------------------

    /** Finite field used by the curve. */
    public static final FiniteField BANDERSNATCH_FIELD
        = FiniteFields.getPrimeField(q);

    /** Bouncy Castle implementation of the Curve. */
    public static final BandersnatchCurve BANDERSNATCH_CURVE
        = new BandersnatchCurve(BANDERSNATCH_FIELD, n, H, a, d);

    /** Singleton Bandersnatch curve instance. */
    public static final Bandersnatch BANDERSNATCH;
    static {
        var Gx = new Natural("0x29c132cc2c0b34c5743711777bbe42f32b79c022ad998465e1e71866a252ae18");
        // decimal: 18886178867200960497001835917649091219057080094937609519140440539760939937304
        var Gy = new Natural("0x2a6c669eda123e0f157d8b50badcd586358cad81eee464605e3167b6cc974166");
        // decimal: 19188667384257783945677642223292697773471335439753913231509108946878080696678
        var G  = BANDERSNATCH_CURVE.createPoint(Gx, Gy);
        BANDERSNATCH = new Bandersnatch(q, n, H, G, BANDERSNATCH_CURVE, HMAC_SIGNER);
    }

    // ---------------------------------------------------------------------------------------------

    private Bandersnatch (BigInteger q, BigInteger n, BigInteger H, ECPoint G,
            ECCurve curve, ECDSASigner signer) {
        super(q, n, H, G, curve, signer);
    }

    // ---------------------------------------------------------------------------------------------

    private static BandersnatchFieldElement fieldElement (BigInteger x) {
        return new BandersnatchFieldElement(x);
    }

    // ---------------------------------------------------------------------------------------------

    public static final class BandersnatchCurve extends ECCurve {

        public final BigInteger a;
        public final BigInteger d;

        private BandersnatchCurve (FiniteField field, BigInteger n, BigInteger H, BigInteger a, BigInteger d) {
            super(field);
            this.a = a;
            this.d = d;
            this.order = n; // despite the name order, this is the subgroup's order
            this.cofactor = H;
            // indicates the points use projective coordinates (there is a z value)
            this.coord = COORD_HOMOGENEOUS;
        }

        @Override public int getFieldSize () {
            return q.bitLength();
        }

        @Override public BandersnatchFieldElement fromBigInteger (BigInteger x) {
            return new BandersnatchFieldElement(x);
        }

        @Override public boolean isValidFieldElement (BigInteger x) {
            return x.signum() >= 0 && x.compareTo(q) < 0;
        }

        @Override public BandersnatchFieldElement randomFieldElement (SecureRandom r) {
            return fromBigInteger(Randomness.randomInteger(r, q));
        }

        @Override public BandersnatchFieldElement randomFieldElementMult (SecureRandom r) {
            BigInteger integer;
            do {
                integer = Randomness.randomInteger(r, q);
            } while (integer.signum() == 0);
            return fromBigInteger(integer);
        }

        @Override protected ECCurve cloneCurve() {
            return this;
        }

        public BandersnatchPoint createPointOrNull (BandersnatchFieldElement x) {
            // The curve equation is (a * x^2) + y^2 == 1 + (d * x^2 * y^2),
            // which gives y == sqrt((1 - a * x^2) / (1 - d * x^2))
            var x2 = x.square();
            var num = ONE.subtract(fieldA.multiply(x2));
            var den = ONE.subtract(fieldD.multiply(x2));
            var y = ((BandersnatchFieldElement) num.divide(den)).sqrtOrNull();
            return y != null
                ? createRawPoint(x, y)
                : null;
        }

        public BandersnatchPoint createPoint (BigInteger x) {
            var point = createPointOrNull(fromBigInteger(x));
            if (point == null)
                // caused by y not being a square an sqrt failing
                throw new IllegalArgumentException("Invalid x value for curve");
            return point;
        }

        public BandersnatchPoint createRandomPoint() {
            BandersnatchPoint point = null;
            while (point == null) {
                var x = randomFieldElementMult(Randomness.SECURE);
                point = createPointOrNull(x);
            }
            return point;
        }

        @Override protected ECPoint decompressPoint (int yTilde, BigInteger x) {
            var point = createPoint(x);
            return (point.getYCoord().testBitZero() == (yTilde == 1))
                ? point
                : new BandersnatchPoint(
                    point.getXCoord(),
                    point.getYCoord().negate(), // use symmetrical value with same x
                    point.getZCoord(0));
        }

        @Override public BandersnatchPoint createPoint (BigInteger x, BigInteger y) {
            return new BandersnatchPoint(fromBigInteger(x), fromBigInteger(y));
        }

        @Override protected BandersnatchPoint createRawPoint (ECFieldElement x, ECFieldElement y) {
            return new BandersnatchPoint(x, y);
        }

        @Override protected BandersnatchPoint createRawPoint (ECFieldElement x, ECFieldElement y, ECFieldElement[] zs) {
            if (zs.length > 1)
                throw new IllegalArgumentException("more than one z coord");
            if (zs.length == 1)
                return new BandersnatchPoint(x, y, zs[0]);
            else // length == 0
                return new BandersnatchPoint(x, y);
        }

        @Override public ECPoint getInfinity() {
            return BandersnatchPoint.ZERO;
        }
    }

    // ---------------------------------------------------------------------------------------------

    public static final class BandersnatchPoint extends ECPoint {

        /**
         * Constant representing the zero / infinity point. There can be other points with the same
         * coordinates, and there are multiple non-canonical coordinates representing this point,
         * so this should not be used for comparisons (use {@link #isInfinity()} instead).
         */
        public static final BandersnatchPoint ZERO
            = new BandersnatchPoint(BandersnatchFieldElement.ZERO, ONE);

        public BandersnatchPoint (ECFieldElement x, ECFieldElement y) {
            super(BANDERSNATCH_CURVE, x, y, array(ONE));
            assert satisfiesCurveEquation();
        }

        public BandersnatchPoint (ECFieldElement x, ECFieldElement y, ECFieldElement z) {
            super(BANDERSNATCH_CURVE, x, y, array(z));
            assert satisfiesCurveEquation() : "Invalid point coordinates";
        }

        @Override public boolean isInfinity() {
            return x.equals(BandersnatchFieldElement.ZERO)
                && (y.equals(getZCoord(0)) || zs == null && y.equals(ONE));
        }

        @Override public boolean satisfiesCurveEquation() {
            if (isInfinity())
                return true;

            // The curve equation is (a * x^2) + y^2 == 1 + (d * x^2 * y^2),
            // the projective version is (a * x^2 * z^2) + (y^2 * z^2) == z^4 + (d * x^2 * y^2)
            // The reason is that in the projective version x = x0 * z, and y = y0 * z (where
            // x0,y0 are the non-projective coordinates of the same point).
            // By substituting these values in the regular equation, we can see that the equality
            // is preserved.

            var z  = getZCoord(0);
            var x2 = x.square();
            var y2 = y.square();
            var z2 = z.square();
            var left  = fieldA.multiply(x2).multiply(z2).add(y2.multiply(z2));
            var right = z2.square().add(fieldD.multiply(x2).multiply(y2));
            return left.equals(right);
        }

        @Override protected ECPoint detach() {
            return new BandersnatchPoint(getAffineXCoord(), getAffineYCoord());
        }

        @Override protected boolean getCompressionYTilde() {
            return this.getAffineYCoord().testBitZero();
        }

        @Override public ECPoint add (ECPoint b) {
            if (this.isInfinity())
                return b;
            if (b.isInfinity())
                return this;

            var x2 = b.getXCoord();
            var y2 = b.getYCoord();

//            x,y,z = self.x, self.y, self.z
//            xx,yy,zz = other.x, other.y, other.z
//            A = z*zz
//            B = A**2
//            C = x*xx
//            D = y*yy
//            E = self.curve.d*C*D
//            F = B-E
//            G = B+E
//            X = A*F*((x+y) * (xx+yy) - C - D)
//            Y = A*G*(D-self.curve.a*C)
//            Z = F*G
//            return Point(X, Y, Z, self.curve)

            var z = getZCoord(0);
            var z2 = b.getZCoord(0);
            var A = z.multiply(z2);
            var B = A.square();
            var C = x.multiply(x2);
            var D = y.multiply(y2);
            var E = fieldD.multiply(C).multiply(D);
            var F = B.subtract(E);
            var G = B.add(E);

            // I unrolled this computation (compared to the python ref implementation)
            // and introduced the names "H" and "I".
            var xy   = x.add(y);
            var x2y2 = x2.add(y2);
            var H    = xy.multiply(x2y2).subtract(C).subtract(D);
            var aC   = fieldA.multiply(C);
            var I    = D.subtract(aC);

            var X = A.multiply(F).multiply(H);
            var Y = A.multiply(G).multiply(I);
            var Z = F.multiply(G);

            // TODO un-normalize
            return new BandersnatchPoint(X, Y, Z);
        }

        @Override public ECPoint negate() {
            return new BandersnatchPoint(x.negate(), y, getZCoord(0));
        }

        @Override public ECPoint subtract (ECPoint b) {
            return add(b.negate());
        }

        @Override public ECPoint twice() {
            // TODO better doubling logic
            /*
            x,y,z = self.x, self.y, self.z
            B = (x+y)**2
            C = x**2
            D = y**2
            E = self.curve.a*C
            F = E+D
            H = z**2
            J = F-2*H
            X = (B-C-D)*J
            Y = F*(E-D)
            Z = F*J
            return Point(X, Y, Z, self.curve)
             */
            return this.add(this);
        }

        @Override public ECPoint multiply (BigInteger scalar) {
            if (false)
                return multiplyFast(scalar);

            // TODO this is the dumb and mean algorithm -- use endomorphism to speed this up
            ECPoint point = this;
            if (scalar.signum() < 0) {
                scalar = scalar.negate();
                point = point.negate();
            }
            ECPoint result = ZERO;

            for (int i = scalar.bitLength() - 1; i >= 0; i--) {
                result = result.twice().normalize();
                if (scalar.testBit(i))
                    result = result.add(point).normalize();
            }

//            for (int i = scalar.bitLength() - 1; i >= 0; i--) {
//                result = result.twice();
//                if (scalar.testBit(i))
//                    result = result.add(point);
//            }

//            System.out.println("results:");
//            System.out.println(result);
//            var fast = multiplyFast(scalar);
//            System.out.println(fast);
//            assert result.equals(fast);

            return result;
        }

        static final BandersnatchFieldElement b = new BandersnatchFieldElement(new BigInteger(
            "37446463827641770816307242315180085052603635617490163568005256780843403514036"));
        static final BandersnatchFieldElement c = new BandersnatchFieldElement(new BigInteger(
            "49199877423542878313146170939139662862850515542392585932876811575731455068989"));

        // FROM RUST

        static final BandersnatchFieldElement COEFF_N11 = new BandersnatchFieldElement(new BigInteger(
           "113482231691339203864511368254957623327"));

        static final BandersnatchFieldElement COEFF_N12 = new BandersnatchFieldElement(new BigInteger(
            "10741319382058138887739339959866629956"));

        static final BandersnatchFieldElement COEFF_N21 = new BandersnatchFieldElement(new BigInteger(
            "21482638764116277775478679919733259912"));

        static final BandersnatchFieldElement COEFF_N22 = new BandersnatchFieldElement(new BigInteger(
            "-113482231691339203864511368254957623327"));

        public ECPoint psi() {
            /*
                def psi(self):
                    # computed in 9 multiplications and 3 additions.
                    x,y,z = self.x, self.y, self.z
                    b = self.curve.b
                    c = self.curve.c
                    xy = x*y
                    y2 = y**2
                    z2 = z**2
                    bz2 = b*z2
                    fy = c * (z2-y2)
                    gy = b * (y2 + bz2)
                    hy = y2 - bz2
                    return BandersnatchPoint(fy*hy, gy*xy, hy * xy, self.curve)
             */
           var z = getZCoord(0);
           var xy = x.multiply(y);
           var y2 = y.square();
           var z2 = z.square();
           var bz2 = b.multiply(z2);
           var fy = c.multiply(z2.subtract(y2));
           var gy = b.multiply(y2.add(bz2));
           var hy = y2.subtract(bz2);
           return new BandersnatchPoint(fy.multiply(hy), gy.multiply(xy), hy.multiply(xy));
        }

        public ECPoint multiplyFast (BigInteger scalar) {
            /*
            psiP = self.psi()
            beta = vector([n,0]) * self.curve.N_inv
            b = vector([int(beta[0]), int(beta[1])]) * self.curve.N
            k1 = n-b[0]
            k2 = -b[1]
            return self.multi_scalar_mul(k1, psiP, k2)
             */
            var s = new BandersnatchFieldElement(scalar);
            var psi = psi();

            // Decompose the scalar into k1, k2, s.t. scalar = k1 + lambda k2
            // via a Babai's nearest plane algorithm.
            // (copied from Rust reference implementation)

//            var beta1 = s.multiply(COEFF_N11).divide(fieldN);
//            var beta2 = s.multiply(COEFF_N12).divide(fieldN);

            var beta1 = new BandersnatchFieldElement(scalar.multiply(COEFF_N11.toBigInteger()).divide(n));
            var beta2 = new BandersnatchFieldElement(scalar.multiply(COEFF_N12.toBigInteger()).divide(n));
            var b1 = beta1.multiply(COEFF_N11).add(beta2.multiply(COEFF_N21));
            var b2 = beta1.multiply(COEFF_N12).add(beta2.multiply(COEFF_N22));
            var k1 = s.subtract(b1);
            var k2 = b2.negate();

            /*
            let beta_1 = scalar_z.clone() * n11;
            let beta_2 = scalar_z * n12;

            let beta_1 = beta_1 / r.clone();
            let beta_2 = beta_2 / r;

            // b = vector([int(beta[0]), int(beta[1])]) * self.curve.N
            let beta_1 = Fr::from(beta_1);
            let beta_2 = Fr::from(beta_2);
            let b1 = beta_1 * Self::COEFF_N11 + beta_2 * Self::COEFF_N21;
            let b2 = beta_1 * Self::COEFF_N12 + beta_2 * Self::COEFF_N22;
             */

//            System.out.println("interm");
//            System.out.println(scalar);
//            System.out.println(s);
//            System.out.println(beta1);
//            System.out.println(beta2);
//            System.out.println(b1);
//            System.out.println(b2);
//            System.out.println(k1);
//            System.out.println(k2);
//            System.out.println("end");

            return multiScalarMultiplication(k1, psi, k2);
        }

        private ECPoint multiScalarMultiplication (
                ECFieldElement k1, ECPoint other, ECFieldElement k2) {

            // TODO normalize? the rust version does it

            ECPoint self = this;
            var ik1 = k1.toBigInteger();
            var ik2 = k2.toBigInteger();
            if (ik1.signum() < 0) {
                ik1 = ik1.negate();
                self = self.negate();
            }
            if (ik2.signum() < 0) {
                ik2 = ik2.negate();
                other = other.negate();
            }
            var sum = self.add(other);

            var k1len = ik1.bitLength();
            var k2len = ik2.bitLength();
            var maxBitSize = Math.max(k1len, k2len);

            // TODO is that the point at infinity?
            ECPoint R = new BandersnatchPoint(
                BandersnatchFieldElement.ZERO,
                BandersnatchFieldElement.ONE,
                BandersnatchFieldElement.ONE);

            for (int i = maxBitSize - 1; i >= 0; --i) {
                R = R.twice();
                var set1 = i < k1len && ik1.testBit(i);
                var set2 = i < k2len && ik2.testBit(i);
                if (set1 && !set2)
                    R = R.add(self);
                else if (!set1 && set2)
                    R = R.add(other);
                else if (set1)
                    R = R.add(sum);
            }
            return R;


            /*
            if k1<0:
                k1=-k1
                P = P.neg()
            if k2<0:
                k2=-k2
                other = other.neg()
            selfPlusOther = self.add(other)
            bits_k1 = ZZ(k1).bits()
            bits_k2 = ZZ(k2).bits()
            while len(bits_k1) < len(bits_k2):
                bits_k1.append(0)
            while len(bits_k2) < len(bits_k1):
                bits_k2.append(0)
            R = Point(0, 1, 1, self.curve)
            for i in range(len(bits_k1)-1,-1,-1):
                R = R.double()
                if bits_k1[i] == 1 and bits_k2[i] == 0:
                    R = R.add(self)
                if bits_k1[i] == 0 and bits_k2[i] == 1:
                    R = R.add(other)
                if bits_k1[i] == 1 and bits_k2[i] == 1:
                    R = R.add(selfPlusOther)
            return R
             */

        }
    }

    // ---------------------------------------------------------------------------------------------

    public static final class BandersnatchFieldElement extends ECFieldElement {

        public static final BandersnatchFieldElement ZERO
                = new BandersnatchFieldElement(BigInteger.ZERO);

        public static final BandersnatchFieldElement ONE
            = new BandersnatchFieldElement(BigInteger.ONE);

        public final BigInteger x;

        public BandersnatchFieldElement (BigInteger x) {
            this.x = x.mod(q);
        }

        @Override public BigInteger toBigInteger () {
            return x;
        }

        @Override public String getFieldName() {
            return "Bandersnatch field";
        }

        @Override public int getFieldSize() {
            return q.bitLength();
        }

        @Override public ECFieldElement add (ECFieldElement b) {
            return new BandersnatchFieldElement(x.add(b.toBigInteger()));
        }

        @Override public ECFieldElement addOne() {
            return new BandersnatchFieldElement(x.add(BigInteger.ONE));
        }

        @Override public ECFieldElement subtract (ECFieldElement b) {
            return new BandersnatchFieldElement(x.subtract(b.toBigInteger()));
        }

        @Override public ECFieldElement multiply (ECFieldElement b) {
            return new BandersnatchFieldElement(x.multiply(b.toBigInteger()));
        }

        @Override public ECFieldElement divide (ECFieldElement b) {
            return new BandersnatchFieldElement(x.multiply(b.toBigInteger().modInverse(q)));
        }

        @Override public ECFieldElement negate() {
            return new BandersnatchFieldElement(x.negate());
        }

        @Override public ECFieldElement square() {
            return new BandersnatchFieldElement(x.multiply(x));
        }

        @Override public ECFieldElement invert() {
            return new BandersnatchFieldElement(x.modInverse(q));
        }

        /** Returns the square root of this element, if it's a square, or null otherwise. */
        public ECFieldElement sqrtOrNull() {
            Solution solution = SQRT_CALC.sqrt(x, q);
            return solution.exists
                ? new BandersnatchFieldElement(solution.root1)
                : null;
        }

        @Override public ECFieldElement sqrt() {
            Solution solution = SQRT_CALC.sqrt(x, q);
            if (!solution.exists)
                throw new IllegalArgumentException("not a square");
            else
                return new BandersnatchFieldElement(solution.root1);
        }

        public boolean isSquare() {
            Solution solution = SQRT_CALC.sqrt(x, q);
            return solution.exists;
        }

        @Override public int hashCode () {
            return x.hashCode();
        }

        @Override public boolean equals (Object obj) {
            return obj instanceof BandersnatchFieldElement
                && x.equals(((BandersnatchFieldElement) obj).x);
        }
    }

    // ---------------------------------------------------------------------------------------------
}
