package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;

import com.goodworkalan.ilk.Ilk;

/**
 * Supplies an instance of an object or a <code>Provider&lt;T&gt;</code>.
 * <p>
 * This internal interface is used in lieu of using
 * {@link javax.inject.Provider Provider&lt;T&gt;} directly. Instead of
 * providing object instances directly, the <code>Vendor</code> interface
 * encapsulates objects in type-safe {@link Ilk.Box} containers. The
 * <code>Ilk.Box</code> will preserve the actual type information for generic
 * types, so that generic objects can be checked for assignability before they
 * are returned by the injector or injected.
 * 
 * @author Alan Gutierrez
 */
public abstract class Vendor<I> {
    /** The super type token of the type to vend. */
    protected final Ilk<I> ilk;
    
    protected final Class<? extends Annotation> qualifier;
    
    protected final Class<? extends Annotation> scope;

    /**
     * Create a vendor with the given super type token.
     */
    protected Vendor(Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        this.ilk = ilk;
        this.qualifier = checkQualifier(qualifier);
        this.scope = checkScope(scope);
    }
    
    protected abstract Ilk.Box get(Injector injector);
    
    /**
     * Check that the given annotation is a scope annotation, or if it is null
     * convert it into a hidden no-scope annotation.
     * 
     * @param scope
     *            The scope annotation.
     * @return The given annotation or a hidden no scope annotation if it is
     *         null.
     * @exception IllegalArgumentException
     *                If the given annotation is not annotated with the
     *                scope annotation.
     */
    static Class<? extends Annotation> checkScope(Class<? extends Annotation> scope) {
        if (scope == null) {
            return NoScope.class;
        }
        if (scope.getAnnotation(Scope.class) == null) {
            throw new IllegalArgumentException();
        }
        return scope;
    }

    /**
     * Check that the given annotation is a qualifier annotation, or if it is
     * null convert it into a hidden no-qualifier annotation.
     * 
     * @param qualifier
     *            The qualifier annotation.
     * @return The given annotation or a hidden no qualifier annotation if it is
     *         null.
     * @exception IllegalArgumentException
     *                If the given annotation is not annotated with the
     *                qualifier annotation.
     */
    static Class<? extends Annotation> checkQualifier(Class<? extends Annotation> qualifier) {
        if (qualifier == null) {
            return NoQualifier.class;
        }
        if (qualifier.getAnnotation(Qualifier.class) == null) {
            throw new IllegalArgumentException();
        }
        return qualifier;
    }
    /**
     * Supply an instance of an object using the given injector to obtain an
     * injected parameters.
     * 
     * @param injector
     *            The dependency injector.
     * @return An object instance boxed with its actual type information.
     */
    Ilk.Box instance(Injector injector) {
        injector.startInjection();
        try {
            Ilk.Box box = injector.getBoxOrLockScope(ilk.key, qualifier, scope);
            if (box == null) {
                box = get(injector);
                injector.addBoxToScope(ilk.key, qualifier, scope, box);
            }
            return box;
        } finally {
            injector.endInjection();
        }
    }

    /**
     * Supply a provoder for an object using the given injector to obtain any
     * injected parameters.
     * 
     * @param injector
     *            The dependency injector.
     * @return An provider instance boxed with its actual type information.
     */

    /**
     * Construct a provider using reflection in order to preserve the actual
     * type information. The reflective methods of <code>Ilk.Key</code> will
     * check the actual type information in the <code>Ilk.Box</code> parameters
     * against the actual type information in the type contained by the key. It will
     * return the newly constructed objects encapsulated in a <code>Ilk.Box</code>
     * with their actual type information.
     */
    Ilk.Box provider(Injector injector) {
        Ilk.Key provider = new Ilk<Provider<I>>() { }.key;
        Type type = ((ParameterizedType) provider.type).getActualTypeArguments()[0];
        Ilk.Box boxedVendor = new Ilk<Vendor<I>>() {}.box(this);
        Ilk.Box boxedInjector = new Ilk<Injector>(Injector.class).box(injector);
        return Injector.needsIlkConstructor(provider, type, boxedVendor, boxedInjector);
    }
}
