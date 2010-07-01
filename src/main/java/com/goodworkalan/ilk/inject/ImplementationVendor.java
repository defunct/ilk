package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;

/**
 * Vender for a specific implementation of an interface. This vendor will
 * construct a new instance of the requested implementation.
 * 
 * @author Alan Gutierrez
 * 
 * @param <I>
 *            The interface type.
 */
class ImplementationVendor<I> extends Vendor<I> {
    /** The type key for the implementation. */
    private final Ilk.Key implementation;

    /**
     * Create an implementation vendor that binds the given super type token to
     * the given implementation.
     * 
     * @param ilk
     *            The super type token of the binding.
     * @param implementation
     *            The implementation.
     * @param qualifier
     *            The binding qualifier.
     * @param scope
     *            The scope in which to store the constructed object.
     * @param reflector
     *            The reflector to use to construct the instance and invoke the
     *            setters.
     */
    public ImplementationVendor(Ilk<I> ilk, Ilk.Key implementation, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, IlkReflect.Reflector reflector) {
        super(ilk, qualifier, scope, reflector);
        this.implementation = implementation;
    }

    /**
     * Get a boxed instance of the implementation using the given injector.
     * 
     * @param injector
     *            The injector.
     * @throws InstantiationException
     *             If the implementation is abstract.
     * @throws IllegalAccessException
     *             If the constructor is inaccessible.
     * @throws InvocationTargetException
     *             If an excpetion is raised by the constructor.
     */
    @Override
    public Ilk.Box get(Injector injector) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return injector.newInstance(reflector, implementation);
    }
}