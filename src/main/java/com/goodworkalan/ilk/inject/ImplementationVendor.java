package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;

class ImplementationVendor<I> extends Vendor<I> {
    private final Ilk.Key implementation;
    
    public ImplementationVendor(Ilk<I> ilk, Ilk.Key implementation, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, IlkReflect.Reflector reflector) {
        super(ilk, qualifier, scope, reflector);
        this.implementation = implementation;
    }
    
    public ImplementationVendor(Ilk<I> ilk, Ilk.Key implementation) {
        this(ilk, implementation, null, null, null);
    }

    @Override
    public Ilk.Box get(Injector injector) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return injector.newInstance(reflector, implementation);
    }
}