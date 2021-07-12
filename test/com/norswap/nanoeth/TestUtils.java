package com.norswap.nanoeth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class TestUtils {
    private TestUtils () {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Same as {@link #getField(Object, Class, String)}, using the object's class as second
     * parameter.
     */
    public static <T> T getField (Object object, String name) {
        return getField(object, object.getClass(), name);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the value of the (potentially private) named field of the given class (which has to
     * be the class declaring the field) on the given object.
     *
     * <p>The field is set accessible in the process.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getField (Object object, Class<?> klass, String name) {
        try {
            var field = klass.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the given (potentially private) method fo the given class (which has to be the class
     * declaring the method).
     *
     * <p>The method is set accessible in the process.
     */
    public static Method getMethod (Class<?> klass, String name, Class<?>... parameterTypes) {
        try {
            Method m = klass.getDeclaredMethod(name, parameterTypes);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** Invoke the given method, wrapping any check exception in an {@link Error}. */
    public static Object invoke (Method m, Object receiver, Object... args) {
        try {
            return m.invoke(receiver, args);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Invoke the given method, wrapping any check exception in an {@link Error}, and casting the
     * result to the required type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeCast (Method m, Object receiver, Object... args) {
        try {
            return (T) m.invoke(receiver, args);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    // ---------------------------------------------------------------------------------------------
}
