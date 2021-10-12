package com.norswap.nanoeth.utils;

/**
 * General math utilities.
 */
public final class MathUtils {
    private MathUtils() {}

    // ---------------------------------------------------------------------------------------------

    /** Returns true iff x is a power of 2. */
    public static boolean isPowerOf2 (int x) {
        // https://stackoverflow.com/questions/600293
        return (x & (x - 1)) == 0;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * If {@code x} {@link #isPowerOf2(int) is a power of 2}, returns its base-2 logarithm.
     */
    public static int log2 (int x) {
        assert isPowerOf2(x) && x != 0;
        return Integer.numberOfTrailingZeros(x);
    }

    // ---------------------------------------------------------------------------------------------
}
