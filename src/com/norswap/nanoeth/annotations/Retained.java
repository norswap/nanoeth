package com.norswap.nanoeth.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that the parameter is a mutable object, and that it is potentially stored somewhere as
 * a result of calling the function, and that care should thus be taken when mutating that object.
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Retained {}
