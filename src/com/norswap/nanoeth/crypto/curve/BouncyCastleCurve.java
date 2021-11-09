package com.norswap.nanoeth.crypto.curve;

import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

/**
 * Encapsulate a curve implemented by the Bouncy Castle library to make it compatible with
 * the {@link Curve} base class.
 */
public final class BouncyCastleCurve extends Curve<ECPoint> {

    // ---------------------------------------------------------------------------------------------

    @Override public BigInteger q() {
        return null;
    }

    @Override public BigInteger N() {
        return null;
    }

    @Override public BigInteger n() {
        return null;
    }

    @Override public BigInteger H() {
        return null;
    }

    @Override public ECPoint G() {
        return null;
    }

    @Override public ECPoint zero() {
        return null;
    }

    @Override public ECSigner signer () {
        return null;
    }

    @Override public BigInteger getPointX (ECPoint o) {
        return null;
    }

    @Override public BigInteger getPointY (ECPoint o) {
        return null;
    }

    @Override public BigInteger getPointZ (ECPoint o) {
        return null;
    }

    @Override public boolean isZero (ECPoint o) {
        return false;
    }

    @Override public boolean isValid (ECPoint o) {
        return false;
    }

    @Override public ECPoint pointOrNull (BigInteger x) {
        return null;
    }

    @Override public ECPoint point (BigInteger x, BigInteger y, BigInteger z) {
        return null;
    }

    // ---------------------------------------------------------------------------------------------
}
