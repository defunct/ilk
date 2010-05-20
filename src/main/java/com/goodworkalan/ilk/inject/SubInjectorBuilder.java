package com.goodworkalan.ilk.inject;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

class SubInjectorBuilder implements Builder {
    public Ilk.Box instance(Injector injector) {
        return new Ilk<Injector>(Injector.class).box(new Injector(injector));
    }
    
    public Ilk.Box provider(final Injector injector) {
        return new Ilk<Provider<Injector>>() {}.box(new Provider<Injector>() {
            public Injector get() {
                return new Injector(injector);
            }
        });
    }
}
