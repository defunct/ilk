package com.goodworkalan.ilk.inject;

import java.util.LinkedList;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

public class BuilderProvider<T> implements Provider<T> {
    private final LinkedList<QualifiedType> stack;
    
    private final Injector injector;
    
    private final Builder builder;
    
    private final Ilk<T> ilk;
    
    public BuilderProvider(Ilk<T> ilk, LinkedList<QualifiedType> stack, Builder builder, Injector injector) {
        this.stack = stack;
        this.injector = injector;
        this.builder = builder;
        this.ilk = ilk;
    }
    
    public T get() {
        return builder.instance(stack, injector).cast(ilk);
    }
}
