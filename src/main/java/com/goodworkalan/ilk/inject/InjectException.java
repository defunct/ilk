package com.goodworkalan.ilk.inject;

/**
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

    public InjectException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
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
     * @param cause
     * @param arguments
     * @return
     */
    static String _(String format, Throwable cause, Object... arguments) {
        if (cause instanceof Error) {
            if (!(cause instanceof ExceptionInInitializerError)) {
                throw (Error) cause;
            }
        }
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof Class<?>) {
                arguments[i] = ((Class<?>) arguments[i]).getName();
            }
        }
        return String.format(format, arguments);
    }
}
