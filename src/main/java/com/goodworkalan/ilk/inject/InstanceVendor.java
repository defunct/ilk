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
    private final I instance;
    
    /**
     * Create a provider that always returns the given instance.
     * 
     * @param instance
     *            The instance.
     */
    public InstanceVendor(Ilk<I> ilk, I instance, Class<? extends Annotation> qualifier) {
        super(ilk, qualifier, NoScope.class);
        this.instance = instance;
    }
    
    protected Ilk.Box get(Injector injector) {
        return ilk.box(instance);
    }
}
