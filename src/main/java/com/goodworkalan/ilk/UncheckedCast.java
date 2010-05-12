package com.goodworkalan.ilk;

/**
 * A wrapper around an unchecked cast with the unchecked cast warning
 * suppressed.
 * <p>
 * With consistent use of the class, one can identify unchecked casts in code
 * using the object browsing capabilities of a Java IDE.
 * 
 * @author Alan Gutierrez
 * 
 * @param <T>
 *            The type to cast to.
 */
class UncheckedCast {
    /**
     * Cast the given object to the parameterized type of this unchecked cast
     * instance.
     * 
     * @param object
     *            The object to cast.
     * @return The object cast to the target type of this unchecked cast.
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object object) {
        return (T) object;
    }
}
