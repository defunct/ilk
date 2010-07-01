package com.goodworkalan.ilk.inject.alias;

import java.lang.annotation.Annotation;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Ilk.Box;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.Vendor;

/**
 * A vendor that aliases one type and qualifier with another type and qualifier.
 * The alias type can be a super type of the aliased type or an implemented
 * interface of the aliased type.
 * 
 * @author Alan Gutierrez
 * 
 * @param <T>
 *            The type to vend.
 */
public class AliasVendor<T> extends Vendor<T> {
    /** The type to alias. */
    private final Ilk<? extends T> fromIlk;
    
    /** The annotation for the type to alias. */
    private final Class<? extends Annotation> fromQualifier;

    /**
     * Create an alias vendor.
     * 
     * @param ilk
     *            The super type token of the binding.
     * @param qualifier
     *            The binding qualifier.
     * @param fromIlk
     *            The type to alias.
     * @param fromQualifier
     *            The annotation for the type to alias.
     */
    public AliasVendor(Ilk<T> ilk, Class<? extends Annotation> qualifier, Ilk<? extends T> fromIlk, Class<? extends Annotation> fromQualifier) {
        super(ilk, qualifier, null, null);
        this.fromIlk = fromIlk;
        this.fromQualifier = fromQualifier;
    }

    /**
     * Get an instance of the aliased type.
     * 
     * @param injector
     *            The injector.
     * @return The boxed aliased instance.
     */
    @Override
    public Box get(Injector injector) {
        return ilk.box(injector.instance(fromIlk, fromQualifier));
    }
}
