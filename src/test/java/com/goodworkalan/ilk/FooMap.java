package com.goodworkalan.ilk;

import java.util.TreeMap;

/**
 * Test parameterized type.
 * 
 * @author Alan Gutierrez
 * 
 * @param <Z>
 *            The map value type.
 * @param <A>
 *            The map key type.
 */
public final class FooMap<Z, A> extends TreeMap<A, Z> {
    /** The serial version id. */
    private static final long serialVersionUID = 1L;
}