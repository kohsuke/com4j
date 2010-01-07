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
@SuppressWarnings("serial")
public class IllegalAnnotationException extends RuntimeException {

    /**
     * Standard constructor
     */
    public IllegalAnnotationException() {
      super();
    }

    /**
     * Constructor taking a message text.
     * @param message the error message of the {@link IllegalAnnotationException}
     */
    public IllegalAnnotationException(String message) {
        super(message);
    }

    /**
     * Constructor taking a message text and a cause.
     * @param message the error message of the {@link IllegalAnnotationException}
     * @param cause the cause of the {@link IllegalAnnotationException}
     */
    public IllegalAnnotationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor taking a cause.
     * @param cause the cause of the {@link IllegalAnnotationException}
     */
    public IllegalAnnotationException(Throwable cause) {
        super(cause);
    }

}
