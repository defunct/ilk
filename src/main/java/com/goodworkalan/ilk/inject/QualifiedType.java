package com.goodworkalan.ilk.inject;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;

import com.goodworkalan.ilk.Ilk;

/**
 * An opaque key for qualified types.
 *
 * @author Alan Gutierrez
 */
public class QualifiedType implements Serializable {
    /** The serial version id. */
    private static final long serialVersionUID = 1L;

    /** The qualifier. */
    final Class<? extends Annotation> qualifier;
    
    /** The type key. */
    final Ilk.Key key;

    /**
     * Create a qualified type with the given qualifier and the given type key.
     * 
     * @param qualifier
     *            The qualifier.
     * @param key
     *            The type key.
     */
    QualifiedType(Class<? extends Annotation> qualifier, Ilk.Key key) {
        this.qualifier = qualifier;
        this.key = key;
    }

    /**
     * This object is equal to the given object if it is also a qualified types
     * and the qualifier and key are equals.
     * 
     * @param object
     *            The object to test for equality.
     * @return True if the objects are equal.
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof QualifiedType) {
            QualifiedType qt = (QualifiedType) object;
            return qualifier.equals(qt.qualifier) && key.equals(qt.key);
        }
        return false;
    }

    /**
     * Generate a hash code as a combination of the hash codes of the qualifier
     * and the key.
     * 
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Arrays.asList(qualifier, key).hashCode();
    }
}
