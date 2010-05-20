package com.goodworkalan.ilk.inject;

import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.goodworkalan.danger.CodedDanger;

/**
 * A general purpose exception that indicates that an error occurred in one 
 * of the classes in the inject package.
 *   
 * @author Alan Gutierrez
 */
public final class InjectException
extends CodedDanger {
    /** The serial version id. */
    private static final long serialVersionUID = 1L;

    /** The cache of exception message resource bundles. */
    private final static ConcurrentMap<String, ResourceBundle> BUNDLES = new ConcurrentHashMap<String, ResourceBundle>();

    /** Unable to create a class because there are more than injectable constructors. */
    public final static int MULTIPLE_INJECTABLE_CONSTRUCTORS = 101;
    
    public final static int INVOCATION_TARGET = 204;

    /**
     * Create an <code>InjectException</code> with the given error <code>code</code>.
     * 
     * @param code
     *            The error code.
     * @param arguments
     *            The positioned format arguments.
     */
    public InjectException(int code, Object...arguments) {
        super(BUNDLES, code, null, arguments);
    }

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
    public InjectException(int code, Throwable cause, Object... arguments) {
        super(BUNDLES, code, cause, arguments);
    }
}
