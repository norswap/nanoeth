package com.norswap.nanoeth.utils;

import java.util.HashSet;
import java.util.List;

/**
 * Misc general-purpose utilities that do not have a place in other utility classes.
 */
public final class Utils {
    private Utils () {}

    // ---------------------------------------------------------------------------------------------

    /** Returns true iff all supplies values are distinct. */
    @SafeVarargs
    public static <T> boolean allDistinct (T... values) {
        return new HashSet<>(List.of(values)).size() == values.length;
    }

    // ---------------------------------------------------------------------------------------------
}
