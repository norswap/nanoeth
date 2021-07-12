package com.norswap.nanoeth.utils;

/**
 * Utilities for performing runtime checks.
 */
public final class Assert {
    private Assert() {}

    // ---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–-

    /**
     * @throws AssertionError if the condition does not hold, with a message created by {@link
     * String#format(String, Object...)}
     */
    public static void that (boolean condition, String format, Object... args) {
        if (!condition)
            throw new AssertionError(String.format(format, args));
    }

    // ---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–---–-
}
