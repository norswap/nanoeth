package com.norswap.nanoeth.utils;

import java.security.SecureRandom;

/**
 * Utilities related to random number generation.
 */
public final class Randomness {
    private Randomness() {}

    // ---------------------------------------------------------------------------------------------

    /** Cached {@link SecureRandom} instance, as building those is expensive. */
    public static final SecureRandom SECURE = new SecureRandom();

    // ---------------------------------------------------------------------------------------------
}
