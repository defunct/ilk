package com.goodworkalan.ilk.inject;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Ilk.Box;

/**
 * Provides a specific instance of a type.
 * 
 * @author Alan Gutierrez
 * 
 * @param <T>
 *            The type.
 */
class InstanceProvider<T> implements Builder {
    /** The instance. */
    private final T instance;

    private final Ilk<T> ilk;
    
    /**
     * Create a provider that always returns the given instance.
     * 
     * @param instance
     *            The instance.
     */
    public InstanceProvider(Ilk<T> ilk, T instance) {
        this.ilk = ilk;
        this.instance = instance;
    }
    
    public Ilk.Box instance(Injector injector) {
        return ilk.box(instance);
    }
    
    public Box provider(Injector injector) {
        return Injector.provider(ilk).box(new Provider<T>() {
            public T get() {
                return instance;
            }
        });
    }
}
