package com.goodworkalan.ilk.inject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A general purpose exception that indicates that an error occurred in one 
 * of the classes in the inject package.
 *   
 * @author Alan Gutierrez
 */
public final class InjectException extends RuntimeException {
    /** The serial version id. */
    private static final long serialVersionUID = 1L;

    /** Unable to create a class because there are more than injectable constructors. */
    public final static int MULTIPLE_INJECTABLE_CONSTRUCTORS = 301;
    
    /** The exception error code. */
    public final int code;
    
    /** The exception properties. */
    public final Map<String, Object> properties = new LinkedHashMap<String, Object>();

    /**
     * Wrap the given cause exception in an <code>InjectException</code> with
     * the given error code.
     * 
     * @param code
     *            The error code.
     * @param cause
     *            The cause exception.
     * @param arguments
     *            The positioned format arguments.
     */
    InjectException(int code, Throwable cause) {
        super(null, cause);
        this.code = code;
    }
    
    public InjectException put(String name, Object value) {
        properties.put(name, value);
        return this;
    }
    
    @Override
    public String getMessage() {
        return "[" + code + "]: " + properties.toString();
    }
}
