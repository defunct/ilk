package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.inject.InjectorBuilder.checkScope;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

public class ListBinder<I> {
    private final List<Builder> builders;
    
    private final Ilk<I> type;
    
    public ListBinder(Ilk<I> ilk, List<Builder> builders) {
        this.type = ilk;
        this.builders = builders;
    }
    
    public void provider(Ilk<? extends Provider<? extends I>> provider, Class<? extends Annotation> scope) {
        builders.add(new NewProviderBuilder<I>(provider, type, NoQualifier.class, checkScope(scope)));
    }
    
    public void provider(Provider<? extends I> provider) {
        builders.add(new ProviderBuilder<I>(type, provider));
    }
    
    public void instance(I instance) {
        builders.add(new InstanceProvider<I>(type, instance));
    }
    
    public <C extends I> void implementation(Ilk<C> implementation, Class<? extends Annotation> scope) {
        builders.add(new NewImplementationBuilder(new Ilk<Provider<I>>(type.key) {}.key, type.key, implementation.key, NoQualifier.class, checkScope(scope)));
    }
}
