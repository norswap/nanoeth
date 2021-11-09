package com.norswap.nanoeth.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Utilities related to random number generation.
 */
public final class Randomness {
    private Randomness() {}

    // ---------------------------------------------------------------------------------------------

    /** When generating random big integers, the number of bits that should be generated. */
    public static final int RANDOM_VALUE_BIT_LENGTH = 256;

    // ---------------------------------------------------------------------------------------------

    /** Cached {@link SecureRandom} instance, as building those is expensive. */
    public static final SecureRandom SECURE = new SecureRandom();

    // ---------------------------------------------------------------------------------------------

    /** Returns a random integer with the given bit length. */
    public static BigInteger randomInteger (int bitLength) {
        return new BigInteger(bitLength, SECURE);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a random integer with the {@link #RANDOM_VALUE_BIT_LENGTH default random bit
     * length}.
     */
    public static BigInteger randomInteger() {
        return new BigInteger(RANDOM_VALUE_BIT_LENGTH, SECURE);
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns a random list of integer with the given bit length. */
    public static BigInteger[] randomIntegers (int bitLength, int amount) {
        var out = new BigInteger[amount];
        for (int i = 0; i < amount; i++)
            out[i] = randomInteger(bitLength);
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a random list of integer with the {@link #RANDOM_VALUE_BIT_LENGTH default random bit
     * length}.
     */
    public static BigInteger[] randomIntegers (int amount) {
        var out = new BigInteger[amount];
        for (int i = 0; i < amount; i++)
            out[i] = randomInteger(RANDOM_VALUE_BIT_LENGTH);
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a random big integer {@code x} such that {@code 0 <= x < max}, guaranteeing
     * an equal distribution between possible values.
     */
    public static BigInteger randomInteger (Random random, BigInteger max) {
        var bitLength = max.bitLength();
        BigInteger candidate;
        do {
            candidate = new BigInteger(bitLength, random);
        } while (candidate.compareTo(max) >= 0);
        return candidate;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a random big integer {@code x} such that {@code 0 <= x < max}, guaranteeing
     * an equal distribution between possible values.
     */
    public static BigInteger randomInteger (BigInteger max) {
        return randomInteger(SECURE, max);
    }

    // ---------------------------------------------------------------------------------------------
}
