package com4j.tlbimp;

/**
 * Signals a failure in the binding process.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class BindingException extends Exception {
    public BindingException(String message) {
        super(message);
    }

    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BindingException(Throwable cause) {
        super(cause);
    }
}
