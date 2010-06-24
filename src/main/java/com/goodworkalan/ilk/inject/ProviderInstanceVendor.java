package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

// TODO Document.
class ProviderInstanceVendor<I> extends Vendor<I> {
    // TODO Document.
    private final Provider<? extends I> provider;
    
    // TODO Document.
    public ProviderInstanceVendor(Ilk<I> ilk, Provider<? extends I> provider, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(ilk, qualifier, scope, null);
        this.provider = provider;
    }

    // TODO Document.
    public Ilk.Box get(Injector injector) {
        return ilk.box(provider.get());
    }
}
