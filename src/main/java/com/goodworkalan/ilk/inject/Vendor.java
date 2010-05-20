package com.goodworkalan.ilk.inject;

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
interface Vendor {
    /**
     * Supply an instance of an object using the given injector to obtain an
     * injected parameters.
     * 
     * @param injector
     *            The dependency injector.
     * @return An object instance boxed with its actual type information.
     */
    public Ilk.Box instance(Injector injector);

    /**
     * Supply a provoder for an object using the given injector to obtain any
     * injected parameters.
     * 
     * @param injector
     *            The dependency injector.
     * @return An provider instance boxed with its actual type information.
     */
    public Ilk.Box provider(Injector injector);        
}
