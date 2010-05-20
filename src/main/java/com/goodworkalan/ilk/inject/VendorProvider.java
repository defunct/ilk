package com.goodworkalan.ilk.inject;

import javax.inject.Provider;

import com.goodworkalan.ilk.Ilk;

/**
 * A bridge from the {@link Provider Provider&lt;T&gt;} interface to a
 * {@link Vendor} implementation.
 * <p>
 * This class is generated through the reflection methods of {@link Ilk.Key}
 * which check the assignability of arguments encapsulated in {@link Ilk.Box}
 * containers, and always return <code>Ilk.Box</code> containers that
 * encapsulate the return value with actual type arguments, if the return value
 * is generic. When this provider performs the cast of an object to a generic
 * type, it does so knowing that the object was obtained through a constructor
 * or method that produces the provided type according to the actual type
 * arguments of the method or constructor.
 * 
 * @author Alan Gutierrez
 * 
 * @param <I>
 *            The type to provide.
 */
class VendorProvider<I> implements Provider<I> {
    /** The super type token of the type to provide. */
    private final Ilk<I> ilk;
    
    /** The vendor. */
    private final Vendor vendor;
    
    /** The injector. */
    private final Injector injector;

    /**
     * Create a <code>VendorProvider</code> that provides an instance of the
     * type specified by the given super type token, using the given vendor and
     * the given injector.
     * 
     * @param ilk
     *            The super type token of the type to provide.
     * @param vendor
     *            The vendor.
     * @param injector
     *            The injector.
     */
    public VendorProvider(Ilk<I> ilk, Vendor vendor, Injector injector) {
        this.injector = injector;
        this.vendor = vendor;
        this.ilk = ilk;
    }

    /**
     * Get an instance of the type by calling the instance method of the
     * specified vendor passing it the specified injector.
     * 
     * @return An instance of the type.
     */
    public I get() {
        return vendor.instance(injector).cast(ilk);
    }
}
