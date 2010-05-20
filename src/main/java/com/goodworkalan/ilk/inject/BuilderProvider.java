package com.goodworkalan.ilk.inject;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

/**
 * A bridge from the <code>Builder</code> interface  {@link Provider Provider&lt;T&gt;}.
 * <p>
 * 
 * @author alan
 *
 * @param <T>
 */
class BuilderProvider<T> implements Provider<T> {
    private final Ilk<T> ilk;
    
    private final Builder builder;
    
    private final Injector injector;
    
    public BuilderProvider(Ilk<T> ilk, Builder builder, Injector injector) {
        this.injector = injector;
        this.builder = builder;
        this.ilk = ilk;
    }
    
    public T get() {
        return builder.instance(injector).cast(ilk);
    }
}
