package com.norswap.nanoeth.utils;

import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Misc general-purpose utilities that do not have a place in other utility classes.
 */
public final class Utils {
    private Utils () {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Terse way to create arrays: {@code array("a", "b")} instead of {@code new String[]{"a",
     * "b"}}.
     */
    @SafeVarargs
    public static <T> T[] array (T... items) {
        return items;
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns true iff all supplies values are distinct. */
    @SafeVarargs
    public static <T> boolean allDistinct (T... values) {
        return new HashSet<>(List.of(values)).size() == values.length;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns an array obtained by applying the function {@code f} to each item in {@code array}.
     *
     * <p>The returned array is obtained by calling {@code arraySupplier} with the desired size.
     */
    public static <T,R> R[] map (T[] array, IntFunction<R[]> arraySupplier, Function<T, R> f) {
        R[] out = arraySupplier.apply(array.length);
        for (int i = 0; i < array.length; i++)
            out[i] = f.apply(array[i]);
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns an array obtained by applying the function {@code f} to each item in {@code array}.
     *
     * <p>The returned array is obtained by calling {@code arraySupplier} with the desired size.
     */
    public static <T,R> R[] mapThrowing
            (T[] array, IntFunction<R[]> arraySupplier, ThrowingFunction<T, R> f)
            throws Throwable {

        R[] out = arraySupplier.apply(array.length);
        for (int i = 0; i < array.length; i++)
            out[i] = f.apply(array[i]);
        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Similar to {@link Function}, but allowed to throw exceptions.
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Throwable;
    }

    // ---------------------------------------------------------------------------------------------
}
