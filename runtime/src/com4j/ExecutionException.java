package com4j;

/**
 * Signals a general {@link RuntimeException} thrown during com4j processing.
 *
 * @author Kohsuke Kawaguchi
 */

@SuppressWarnings("serial")
public class ExecutionException extends RuntimeException {
    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionException(Throwable cause) {
        super(cause);
    }
}
