package com4j;

/**
 * Data holder used for "out" or "in/out" parameter.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Holder<T> {
    public T value;

    /**
     * Initializes {@link #value} to <tt>null</tt>.
     */
    public Holder() {
    }
    
    /**
     * Initializes {@link #value} by the given value.
     */
    public Holder(T value) {
        this.value = value;
    }
}
