package com.goodworkalan.ilk.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Scope;

/**
 * Indicates that an instance should be cached in a scope associated with the
 * child injector that created it. If the injector is not a child injector, then
 * it is quite obviously stored in the root injector.
 * 
 * @author Alan Gutierrez
 */
@Documented
@Target( { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Scope
public @interface InjectorScoped {
}
