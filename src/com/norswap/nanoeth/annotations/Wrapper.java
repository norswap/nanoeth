package com.norswap.nanoeth.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicate that the class is a wrapper over a single data field, whose sole purpose is
 * to establish that the value of the field satisfies some properties.
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Wrapper {}
