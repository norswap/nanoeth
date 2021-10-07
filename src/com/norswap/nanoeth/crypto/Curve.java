package com.norswap.nanoeth.crypto;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.crypto.signers.RandomDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import java.math.BigInteger;

/**
 * Represent an elliptic curve that can be used for signing.
 *
 * <p>Currently, only the {@link #SECP256K1} curve is supported.
 */
public final class Curve
{
    // =============================================================================================
    // region FIELDS & CONSTRUCTOR
    // =============================================================================================

    // See accessors with identical names.
    private final BigInteger q;
    private final BigInteger N;
    private final BigInteger n;
    private final ECPoint G;

    private final ECDomainParameters domainParams;
    private final ECDSASigner signer;

    // ---------------------------------------------------------------------------------------------

    private Curve (BigInteger q, X9ECParameters x9Params, ECDSASigner signer) {
        this.q = q;
        this.N = x9Params.getN();
        this.n = x9Params.getH().modInverse(q).multiply(N);
        // 1/H * N == n/N * N == n   (mod q)
        this.G = x9Params.getG();
        this.domainParams = new ECDomainParameters(
            x9Params.getCurve(),
            x9Params.getG(),
            x9Params.getN(),
            x9Params.getH());
        this.signer = signer;
    }

    // endregion
    // =============================================================================================
    // region CURVES
    // =============================================================================================

    public static final Curve SECP256K1 = new Curve(
        SecP256K1Curve.q,
        CustomNamedCurves.getByName("secp256k1"),
        // Generate the signature's "k" value using HMAC, as proposed in IETF RFC6979, §3.2.
        // See signature package README for details.
        new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest())));

    // endregion
    // =============================================================================================
    // region ACCESSORS
    // =============================================================================================

    /** The order (size) of the finite field on which the curve is defined. */
    public BigInteger q() {
        return q;
    }

    /** The order (size) of the elliptic curve. */
    public BigInteger N() {
        return N;
    }

    /** The order (size) of the cyclic subgroup generated by {@link #G}. */
    public BigInteger n() {
        return n;
    }

    /** A point that generates a cyclic subgroup of the finite field, of order {@link #n}. */
    public ECPoint G() {
        return G;
    }

    /** Returns the 0 point (aka infinity point) for this curve. */
    public ECPoint zero() {
        return domainParams.getCurve().getInfinity();
    }

    // endregion
    // =============================================================================================
    // region POINTS
    // =============================================================================================

    private static final X9IntegerConverter X9 = new X9IntegerConverter();

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the point designated by the given x-coordinate. The y-coordinate can be computed
     * automatically, but since curves are symmetric on the x axis, there are two possible values
     * (one even, one odd), selected by the {@code yOdd} parameter (see signature package README).
     */
    public ECPoint point (BigInteger x, boolean yOdd) {
        // Obtain a compressed representation, as specified in SEC 1 v2.0, section 2.3.7.
        byte[] compressed = X9.integerToBytes(x, 1 + X9.getByteLength(domainParams.getCurve()));
        compressed[0] = (byte) (yOdd ? 0x03 : 0x02); // encode y's oddness
        // Decode the point, as specified in SEC 1 v2.0, section 2.3.4.
        return domainParams.getCurve().decodePoint(compressed);
    }

    // endregion
    // =============================================================================================
    // region SIGNATURES
    // =============================================================================================

    /**
     * Returns the (r, s) pair obtained by signing the message using the private key with this
     * curve. Does not hash the message before signing it.
     */
    public BigInteger[] sign (BigInteger privateKey, byte[] message) {
        var params = new ECPrivateKeyParameters(privateKey, domainParams);
        signer.init(true, params);
        return signer.generateSignature(message);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Same as {@link #sign} but uses a random k in the signature scheme, to handle the 1/10^36
     * probably case where (r, s) is rejected (see signature package README).
     */
    public BigInteger[] signWithRandomK (BigInteger privateKey, byte[] message) {
        // this happens on average never, don't optimize
        var signer = new ECDSASigner(new RandomDSAKCalculator());
        var params = new ECPrivateKeyParameters(privateKey, domainParams);
        signer.init(true, params);
        return signer.generateSignature(message);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true only if the (r, s) signature could be verified against the given message.
     * Does not hash the message before verifying the signature.
     */
    public boolean verify (ECPoint publicKey, BigInteger r, BigInteger s, byte[] message) {
        // Normally, faster verification is possible using the recovery ID.
        // However, bouncycastle does not seem to implement this.
        var params = new ECPublicKeyParameters(publicKey, domainParams);
        signer.init(false, params);
        return signer.verifySignature(message, r, s);
    }

    // endregion
    // =============================================================================================
}
