package com4j;

/**
 * Signals incorrect use of com4j annotations.
 *
 * <p>
 * The runtime throws this exception when it finds errors in the way
 * the com4j annotations are used.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class IllegalAnnotationException extends RuntimeException {
    public IllegalAnnotationException() {
    }

    public IllegalAnnotationException(String message) {
        super(message);
    }

    public IllegalAnnotationException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalAnnotationException(Throwable cause) {
        super(cause);
    }

}
