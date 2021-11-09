package com.norswap.nanoeth.crypto.curve;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.crypto.signers.RandomDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

/**
 * A signer associated with a specific elliptic curve, used to sign and verify arbitrary byte
 * messages. Uses a {@link ECDSASigner} under the wraps.
 */
public final class ECSigner {
    // =============================================================================================

    /**
     * A signer that generates the signature's "k" value using HMAC, as proposed in IETF RFC6979,
     * §3.2. See signature package README for details.
     */
    public static ECDSASigner HMAC_SIGNER =
            new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));

    // ---------------------------------------------------------------------------------------------

    /**
     * A signer that generates the random signature "k" values. See signature package README for
     * details.
     */
    public static ECDSASigner RANDOM_K_SIGNER =
            new ECDSASigner(new RandomDSAKCalculator());

    // =============================================================================================

    /** Bouncy castle signer wrapped by this signer. */
    public final ECDSASigner signer;

    /** Bouncy castle elliptic curve parameters, necessary for signing*/
    public final ECDomainParameters domainParams;

    // =============================================================================================

    /** Creates a new signer using {@link #HMAC_SIGNER}. */
    public ECSigner (ECDomainParameters domainParams) {
        this(HMAC_SIGNER, domainParams);
    }

    public ECSigner (ECDSASigner signer, ECDomainParameters domainParams) {
        this.signer = signer;
        this.domainParams = domainParams;
    }

    // =============================================================================================

    /**
     * Returns the (r, s) pair obtained by signing the message using the private key with this curve
     * and its {@link #signer signer}. Does not hash the message before signing it.
     */
    public BigInteger[] sign (BigInteger privateKey, byte[] message) {
        var params = new ECPrivateKeyParameters(privateKey, domainParams);
        signer.init(true, params);
        return signer.generateSignature(message);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the (r, s) pair obtained by signing the message using the private key with this curve
     * and the given {@code signer}. Does not hash the message before signing it.
     */
    public BigInteger[] sign (ECDSASigner signer, BigInteger privateKey, byte[] message) {
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

    // =============================================================================================
}
