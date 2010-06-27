package com.goodworkalan.ilk.inject;

import static com.goodworkalan.ilk.Types.getRawClass;
import static com.goodworkalan.ilk.inject.InjectException._;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;

/**
 * Provides an implementation of a provider of a type.
 * 
 * @author Alan Gutierrez
 * 
 * @param <I>
 *            The type to vend.
 */
class ProviderVendor<I> extends Vendor<I> {
    /** The super type token for the provider. */
    private final Ilk<? extends Provider<? extends I>> provider;

    /**
     * Create a provider vendor.
     * 
     * @param provider
     *            The provider super type token.
     * @param ilk
     *            The super type token of the type to provide.
     * @param qualifier
     *            The binding qualifier. The scope in which to store the
     *            constructed object.
     * @param reflector
     *            The reflector to use to construct the provider instance and
     *            invoke the setters.
     */
    public ProviderVendor(Ilk<? extends Provider<? extends I>> provider, Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, IlkReflect.Reflector reflector) {
        super(ilk, qualifier, scope, reflector);
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
    public Ilk.Box get(Injector injector)
    throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return ilk.box(injector.newInstance(reflector, provider.key).cast(provider).get());
    }

    /**
     * Override the default provider implementation to return the provider
     * instance specified by this provider vendor.
     * 
     * @param injector
     *            The injector.
     * @return A boxed provider instance.
     */
    @Override
    Ilk.Box provider(Injector injector) {
        try {
            return injector.newInstance(reflector, provider.key);
        } catch (Throwable e) {
          throw new InjectException(_("Unable to create new instance of [%s].", e, getRawClass(provider.key.type)), e);
        }
    }
}
