package com.goodworkalan.ilk.inject.multi;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.Vendor;

/**
 * A vendor that provides a list of objects provided by the injector.
 * <p>
 * Note that this must be a list and not a set since the equality of vendors is
 * not the same as the equality of the objects they vend.
 * 
 * @author ALan Gutierrez
 * 
 * @param <I>
 *            Type of of the list to vend.
 */
public class ListVendor<I> extends Vendor<List<I>> {
    /** The list of vendors. */
    private final List<Vendor<I>> vendors;

    /**
     * Create a list vendor that will create a list by invoking the instance
     * method of each of the vendors in the given list of vendors.
     * 
     * @param vendors
     *            The list of vendors.
     * @param ilk
     *            The super type token of type of the list to vend.
     */
    public ListVendor(List<Vendor<I>> vendors, Ilk<I> ilk, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope) {
        super(new Ilk<List<I>>() {}.assign(new Ilk<Ilk<I>>() {}, ilk), qualifier, scope, IlkReflect.REFLECTOR);
        this.vendors = vendors;
    }

    /**
     * Vend an instance of the list.
     * 
     * @param injector
     *            The injector.
     * @return A boxed instance of the list.
     */
    public Ilk.Box get(Injector injector) {
        List<I> built = new ArrayList<I>();
        for (Vendor<I> vendor : vendors) {
            built.add(injector.instance(vendor));
        }
        return ilk.box(built);
    }
}
