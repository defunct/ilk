package com.goodworkalan.ilk.inject;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

class ProviderInstanceVendor<I> implements Vendor {
    private final Ilk<I> ilk;

    private final Provider<? extends I> provider;
    
    public ProviderInstanceVendor(Ilk<I> ilk, Provider<? extends I> provider) {
        this.ilk = ilk;
        this.provider = provider;
    }

    /**
     * @param stipulations The map of types to instructions on how to provide them.
     * @param factories The map of types to a resolved strategy on how to build them.
     */
    public Ilk.Box instance(Injector injector) {
        return ilk.box(provider.get());
    }
    
    public Ilk.Box provider(Injector injector) {
        return Injector.provider(ilk).box(new Provider<I>() {
            public I get() {
                return provider.get();
            }
        });
    }
}
