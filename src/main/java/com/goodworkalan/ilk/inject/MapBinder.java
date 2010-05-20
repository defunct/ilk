package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.inject.InjectorBuilder.checkScope;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

public class MapBinder<K, I> {
    private final Map<K, Builder> builders;

    private final Ilk<I> ilk;
    
    public MapBinder(Ilk<I> ilk, Map<K, Builder> builders) {
        this.ilk = ilk;
        this.builders = builders;
    }
    
    public void provider(K key, Ilk<? extends Provider<? extends I>> provider, Class<? extends Annotation> scope) {
        builders.put(key, new NewProviderBuilder<I>(provider,ilk, NoQualifier.class, checkScope(scope)));
    }
    
    public void provider(K key, Provider<? extends I> provider) {
        builders.put(key, new ProviderBuilder<I>(ilk, provider));
    }
    
    public void instance(K key, I instance) {
        builders.put(key, new InstanceProvider<I>(ilk, instance));
    }
    
    public <C extends I> void implementation(K key, Ilk<C> implementation, Class<? extends Annotation> scope) {
        builders.put(key, new NewImplementationBuilder(new Ilk<Provider<I>>(ilk.key) {}.key, ilk.key, implementation.key, NoQualifier.class, checkScope(scope)));
    }
}
