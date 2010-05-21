package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

class ProviderInstanceVendor<I> extends Vendor<I> {
    private final Provider<? extends I> provider;
    
    public ProviderInstanceVendor(Ilk<I> ilk, Provider<? extends I> provider, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(ilk, qualifier, scope, null);
        this.provider = provider;
    }

    public Ilk.Box get(Injector injector) {
        return ilk.box(provider.get());
    }
}
