package com.goodworkalan.ilk.inject;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Qualifier;

/**
 * Indicate that a driver is licensed.
 *
 * @author Alan Gutierrez
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface Licensed {
}
