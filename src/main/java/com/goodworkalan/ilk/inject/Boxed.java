package com.goodworkalan.ilk.inject;

import com.goodworkalan.ilk.Ilk;

/**
 * Used by objects to request injection of the
 * {@link com.goodworkalan.ilk.IlkReflect.Box Ilk.Box} that contains the a desired in
 * injected object. The <code>Boxed&lt;T&gt;</code> class is used by the
 * injector to determine the type of boxed object to inject.
 * 
 * @author Alan Gutierrez
 * 
 * @param <T>
 *            The type to inject.
 */
public class Boxed<T> {
    /** The box. */
    public final Ilk.Box box;

    /**
     * Create an instance of boxed with the given box.
     * 
     * @param box
     *            The box.
     */
    public Boxed(Ilk.Box box) {
        this.box = box;
    }
}
