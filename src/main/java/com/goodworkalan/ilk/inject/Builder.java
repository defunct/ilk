package com.goodworkalan.ilk.inject;

import com.goodworkalan.ilk.Ilk;

/**
 * Builds an instance of an object or of an object provider. This is an internal
 * implementation in, lieu of using {@link javax.inject.Provider
 * Provider&lt;T&gt;} that creates {@link Ilk.Box} instances that contain a
 * pairing of the object with actual type information.
 * 
 * @author Alan Gutierrez
 */
interface Builder {
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
