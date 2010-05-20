package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

class ProviderVendor<I> implements Vendor {
    private final Ilk<? extends Provider<? extends I>> provider;

    private final Ilk<I> type;
    
    private final Class<? extends Annotation> qualifier;
    
    private final Class<? extends Annotation> scope;
    
    public ProviderVendor(Ilk<? extends Provider<? extends I>> provider, Ilk<I> type, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        this.provider = provider;
        this.type = type;
        this.qualifier = qualifier;
        this.scope = scope;
    }

    public Ilk.Box instance(Injector injector) {
        return type.box(provider(injector).cast(provider).get());
    }
    
    public Ilk.Box provider(Injector injector) {
        injector.startInjection();
        Ilk.Box box = injector.newInstance(provider.key, qualifier, scope);
        injector.endInjection();
        return box;
    }
}
