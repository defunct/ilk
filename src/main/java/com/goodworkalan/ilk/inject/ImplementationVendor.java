package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Ilk.Box;

class ImplementationVendor<I> extends Vendor<I> {
    private final Ilk.Key implementation;
    
    public ImplementationVendor(Ilk<I> ilk, Ilk.Key implementation, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, Ilk.Reflector reflector) {
        super(ilk, qualifier, scope, reflector);
        this.implementation = implementation;
    }
    
    public ImplementationVendor(Ilk<I> ilk, Ilk.Key implementation) {
        this(ilk, implementation, null, null, null);
    }

    @Override
    protected Box get(Injector injector) {
        return injector.newInstance(implementation);
    }
}