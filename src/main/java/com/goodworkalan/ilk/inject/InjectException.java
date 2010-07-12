package com.goodworkalan.ilk.inject;


/**
 * A general purpose exception that indicates that an error occurred in one of
 * the classes in the Ilk Inject package.
 * <p>
 * <blockquote>
 * 
 * <pre>
 * try {
 *     inejctor.inject(box, method);
 * } catch (InjectException e) {
 *     throw new InjectException(_(&quot;Cannot load java.lang.String.&quot;, e), e);
 * } catch (Throwable e) {
 *     throw new InjectException(_(&quot;Cannot reflect upon.&quot;, e, cls), e);
 * }
 * </pre>
 * 
 * </blockquote>
 * 
 * @author alan
 * 
 */
public class InjectException extends RuntimeException {
    /** The serial version id. */
    private static final long serialVersionUID = 1L;

    /**
     * Create an injection exception with the given message and cause.
     * 
     * @param message
     *            The exception message.
     * @param cause
     *            The cause.
     */
    public InjectException(Throwable cause, String message) {
        super(message, cause);
    }

    /**
     * If the given cause is not an exception related to reflection, the
     * exception is rethrown, otherwise, an error message is formatted. This
     * makes for compact exception catch blocks that do not repeat the
     * reflection exception ladder.
     * <p>
     * <blockquote>
     * 
     * <pre>
     * try {
     *     reflect(Class.forName(&quot;java.lang.String&quot;));
     * } catch (ClassNotFoundException e) {
     *     throw new InjectException(_(&quot;Cannot load java.lang.String.&quot;, e), e);
     * } catch (Throwable e) {
     *     throw new InjectException(_(&quot;Cannot reflect upon.&quot;, e, cls), e);
     * }
     * </pre>
     * 
     * </blockquote>
     * 
     * @param format
     *            The message format.
     * @param cause
     *            The cause.
     * @param arguments
     *            The format arguments.
     * @return The message.
     */
    static String _(String format, Object... arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof Class<?>) {
                arguments[i] = ((Class<?>) arguments[i]).getName();
            }
        }
        return String.format(format, arguments);
    }
    
    /**
     * Assert that the given exception is a reflection related
     * <code>Exception</code>. .
     * <p>
     * FIXME Outgoing.
     * 
     * @param e
     *            The throwable.
     * @return The throwable.
     * @exception RuntimeException
     *                if the throwable is an <code>RuntimeException</code> but
     *                not an <code>IllegalArgumentException</code>.
     * @exception Error
     *                if the throwable is an <code>Error</code> but not an
     *                <code>ExceptionInInitializerError</code>.
     */
    public static Throwable $(Throwable e) {
        if (e instanceof Error) { 
            if (!(e instanceof ExceptionInInitializerError)) {
                throw (Error) e;
            }
        } else if (e instanceof RuntimeException) {
            if (!(e instanceof IllegalArgumentException)) {
                throw (RuntimeException) e;
            }
        }
        return e;
    }
}
