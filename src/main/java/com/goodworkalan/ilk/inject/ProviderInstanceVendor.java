package com.goodworkalan.ilk.inject;

import java.lang.annotation.Annotation;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

/**
 * Provides an specific instance of a provider of a type.
 * 
 * @author Alan Gutierrez
 * 
 * @param <I>
 *            The type to vend.
 */
class ProviderInstanceVendor<I> extends Vendor<I> {
    /** The provider. */
    private final Provider<? extends I> provider;
    
    /**
     * Create a provider instance vendor.
     * @param ilk
     * @param provider
     * @param qualifier
     * @param scope
     */
    public ProviderInstanceVendor(Ilk<I> ilk, Provider<? extends I> provider, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(ilk, qualifier, scope, null);
        this.provider = provider;
    }

    /**
     * 
     */
    public Ilk.Box get(Injector injector) {
        return ilk.box(provider.get());
    }
}
