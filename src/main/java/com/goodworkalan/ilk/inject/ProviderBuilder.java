package com.goodworkalan.ilk.inject;

import java.util.LinkedList;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

class ProviderBuilder<I, C extends I> implements Builder {
    private final Ilk<I> ilk;

    private final Provider<C> provider;
    
    public ProviderBuilder(Ilk<I> ilk, Provider<C> provider) {
        this.ilk = ilk;
        this.provider = provider;
    }

    /**
     * @param stipulations The map of types to instructions on how to provide them.
     * @param factories The map of types to a resolved strategy on how to build them.
     */
    public Ilk.Box instance(LinkedList<QualifiedType> stack, Injector injector) {
        return ilk.box(provider.get());
    }
    
    public Ilk.Box provider(LinkedList<QualifiedType> stack, Injector injector) {
        return Injector.provider(ilk).box(new Provider<I>() {
            public I get() {
                return provider.get();
            }
        });
    }
}
