package com.goodworkalan.ilk.inject;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Unit tests for the {@link WeakIdentityReference} class.
 *
 * @author Alan Gutierrez
 */
public class WeakIdentityReferenceTest {
    /** Test the hash code. */
    @Test
    public void hash() {
        Object o = new Object();
        assertEquals(System.identityHashCode(o), new WeakIdentityReference(o, null).hashCode());
    }
    
    /** Test equality. */
    @Test
    public void equality() {
        Object o = new Object();
        Object reference = new WeakIdentityReference(o, null);
        assertTrue(reference.equals(new WeakIdentityReference(o, null)));
        assertTrue(reference.equals(reference));
        assertFalse(reference.equals(new WeakIdentityReference(1, null)));
    }
}
