package com.norswap.nanoeth.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that the parameter can take a null value.
 *
 * <p>This is not checked in any way at the moment.
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Nullable {}
