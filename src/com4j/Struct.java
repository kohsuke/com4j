package com4j;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Designates a class as a structure.
 *
 * <p>
 * A structure can be marshaleld to a byte array and vice versa.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface Struct {
    void unmarshal( byte[] src );
    byte[] marshal();
}
