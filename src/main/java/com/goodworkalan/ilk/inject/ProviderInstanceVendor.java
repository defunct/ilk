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
     * 
     * @param ilk
     *            The super type token of the instance to provide.
     * @param provider
     *            The provider.
     * @param qualifier
     *            The binding qualifier.
     * @param scope
     *            The scope in which to store the constructed instance .
     */
    public ProviderInstanceVendor(Ilk<I> ilk, Provider<? extends I> provider, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(ilk, qualifier, scope, null);
        this.provider = provider;
    }

    /**
     * Get an unscoped boxed instance of the implementation provided by this
     * vendor.
     * 
     * @param injector
     *            The injector.
     * @return A boxed instance of the implementation.
     */
    public Ilk.Box get(Injector injector) {
        return ilk.box(provider.get());
    }
}
