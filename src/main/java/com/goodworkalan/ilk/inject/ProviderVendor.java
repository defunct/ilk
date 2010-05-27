package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;

class ProviderVendor<I> extends Vendor<I> {
    private final Ilk<? extends Provider<? extends I>> provider;

    public ProviderVendor(Ilk<? extends Provider<? extends I>> provider, Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, IlkReflect.Reflector reflector) {
        super(ilk, qualifier, scope, reflector);
        this.provider = provider;
    }

    public Ilk.Box get(Injector injector) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return ilk.box(injector.newInstance(reflector, provider.key).cast(provider).get());
    }
}
