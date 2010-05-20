package com.goodworkalan.ilk.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.reflective.Reflection;
import com.goodworkalan.reflective.Reflective;
import com.goodworkalan.reflective.ReflectiveException;

/**
 * Implements the <code>Vendor.provider</code> method using a bridge interface
 * between <code>Provider&lt;T&gt;</code> and the <code>Vendor.instance</code>
 * for bindings that do not bind to a <code>Provider&lt;T&gt;</code>.
 * 
 * @author Alan Gutierrez
 */
abstract class VendorProviderVendor implements Vendor {
    /** The super type token of the provider. */
    private final Ilk.Key provider;

    /**
     * Create a <code>VendorProviderVendor</code> that wraps a call to the
     * <code>Vendor.instance</code> method in in a
     * <code>Provider&lt;T&gt;</code> of the type specified by the given super
     * type token provider.
     *
     * @param provider
     *            The super type token of the provider.
     */
    public VendorProviderVendor(Ilk.Key provider) {
        this.provider = provider;
    }

    /**
     * Construct a provider using reflection in order to preserve the actual
     * type information. The reflective methods of <code>Ilk.Key</code> will
     * check the actual type information in the <code>Ilk.Box</code> parameters
     * against the actual type information in the type contained by the key. It will
     * return the newly constructed objects encapsulated in a <code>Ilk.Box</code>
     * with their actual type information.
     */
    public Ilk.Box provider(Injector injector) {
        final Ilk.Box boxedBuilder = new Ilk<Vendor>(Vendor.class).box(this);
        final Ilk.Box boxedInjector = new Ilk<Injector>(Injector.class).box(injector);
        try {
            return new Reflective().reflect(new Reflection<Ilk.Box>() {
                public Ilk.Box reflect()
                throws InstantiationException,
                       IllegalAccessException,
                       InvocationTargetException,
                       NoSuchMethodException {
                    Ilk.Box boxedIlk;
                    Type provided = ((ParameterizedType) provider.type).getActualTypeArguments()[0];
                    if (provided instanceof ParameterizedType) {
                        Ilk<Ilk<?>> ilkIlk = new Ilk<Ilk<?>>(new Ilk.Key((ParameterizedType) provided)){};
                        Ilk.Box type = new Ilk<ParameterizedType>(ParameterizedType.class).box((ParameterizedType) provided);
                        Constructor<?> newIlk = Ilk.class.getConstructor(ParameterizedType.class);
                        boxedIlk = ilkIlk.key.newInstance(new Ilk.Reflect(), newIlk, type);
                    } else {
                        Ilk.Box boxedClass = new Ilk.Box((Class<?>) provided);
                        Ilk<Ilk<?>> ilkIlk = new Ilk<Ilk<?>>(new Ilk.Key((Class<?>) provided)){};
                        Constructor<?> newIlk = Ilk.class.getConstructor(Class.class);
                        boxedIlk = ilkIlk.key.newInstance(new Ilk.Reflect(), newIlk, boxedClass);
                    }
                    Constructor<?> newBuilderProvider = provider.rawClass.getConstructor(Ilk.class, Vendor.class, Injector.class);
                    return provider.newInstance(new Ilk.Reflect() {
                        public Object newInstance(Constructor<?> constructor, Object[] arguments)
                        throws InstantiationException, IllegalAccessException, InvocationTargetException {
                            return constructor.newInstance(arguments);
                        }
                    }, newBuilderProvider, boxedIlk, boxedBuilder, boxedInjector);
                }
            });
        } catch (ReflectiveException e) {
            throw new InjectException(0, e);
        }
    }
}
