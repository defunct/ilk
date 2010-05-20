package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

class ProviderVendor<I> extends Vendor<I> {
    private final Ilk<? extends Provider<? extends I>> provider;

    public ProviderVendor(Ilk<? extends Provider<? extends I>> provider, Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(ilk, qualifier, scope);
        this.provider = provider;
    }

    public Ilk.Box get(Injector injector) {
        injector.startInjection();
        Ilk.Box box = ilk.box(injector.newInstance(provider.key).cast(provider).get());
        injector.endInjection();
        return box;
    }
}
