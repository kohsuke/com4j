package com4j;

/**
 * Signals incorrect use of com4j annotations.
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
