package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import com.goodworkalan.ilk.Ilk;

/**
 * Provides a specific instance of a type.
 * 
 * @author Alan Gutierrez
 * 
 * @param <I>
 *            The type to vend.
 */
class InstanceVendor<I> extends Vendor<I> {
    /** The instance. */
    private final Ilk.Box instance;
    
    /**
     * Create a provider that always returns the given instance.
     * 
     * @param instance
     *            The instance.
     */
    public InstanceVendor(Ilk<I> ilk, Ilk.Box instance, Class<? extends Annotation> qualifier) {
        super(ilk, qualifier, null, null);
        this.instance = instance;
        
        instance.cast(ilk);
    }
    
    // TODO Document.
    public Ilk.Box get(Injector injector) {
        return instance;
    }
}
