package com4j;

/**
 * Data holder used for "out" or "in/out" parameter.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Holder<T> {
    public T value;

    public Holder() {
    }
    
    public Holder(T value) {
        this.value = value;
    }
}
