package com.goodworkalan.ilk.inject;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

class InjectorVendor implements Vendor {
    public Ilk.Box instance(Injector injector) {
        return new Ilk<Injector>(Injector.class).box(injector);
    }
    
    public Ilk.Box provider(final Injector injector) {
        return new Ilk<Provider<Injector>>() {}.box(new Provider<Injector>() {
            public Injector get() {
                return injector;
            }
        });
    }
}
