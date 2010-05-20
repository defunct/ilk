package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import com.goodworkalan.ilk.Ilk;

class ImplementationVendor extends VendorProviderVendor {
    /** The type qualifier annotation. */
    private final Class<? extends Annotation> qualifier;

    /** The scope annotation. */
    private final Class<? extends Annotation> scope;
    
    private final Ilk.Key implementation;
    
    public ImplementationVendor(Ilk.Key provider, Ilk.Key iface, Ilk.Key implementation, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(provider);
        this.qualifier = qualifier;
        this.scope = scope;
        this.implementation = implementation;
    }

    public Ilk.Box instance(Injector injector) {
        return injector.newInstance(implementation, qualifier, scope);
    }
 }