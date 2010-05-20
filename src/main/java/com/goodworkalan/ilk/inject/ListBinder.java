package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.inject.InjectorBuilder.checkScope;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

public class ListBinder<I> {
    private final List<Vendor> builders;
    
    private final Ilk<I> type;
    
    public ListBinder(Ilk<I> ilk, List<Vendor> builders) {
        this.type = ilk;
        this.builders = builders;
    }
    
    public void provider(Ilk<? extends Provider<? extends I>> provider, Class<? extends Annotation> scope) {
        builders.add(new ProviderVendor<I>(provider, type, NoQualifier.class, checkScope(scope)));
    }
    
    public void provider(Provider<? extends I> provider) {
        builders.add(new ProviderInstanceVendor<I>(type, provider));
    }
    
    public void instance(I instance) {
        builders.add(new InstanceVendor<I>(type, instance));
    }
    
    public void implementation(Ilk<? extends I> implementation, Class<? extends Annotation> scope) {
        builders.add(new ImplementationVendor(new Ilk<Provider<I>>(type.key) {}.key, type.key, implementation.key, NoQualifier.class, checkScope(scope)));
    }
}
