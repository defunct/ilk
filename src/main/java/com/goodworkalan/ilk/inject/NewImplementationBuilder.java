package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import com.goodworkalan.ilk.Ilk;

public class NewImplementationBuilder extends MissingProviderBuilder {
    private final Class<? extends Annotation> qualifier;

    private final Class<? extends Annotation> scope;
    
    public NewImplementationBuilder(Ilk.Key providerKey, Ilk.Key iface, Ilk.Key implementation, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(providerKey, implementation);
        this.qualifier = qualifier;
        this.scope = scope;
    }

    public Ilk.Box instance(Injector injector) {
        return injector.newInstance(implementation, qualifier, scope);
    }
 }