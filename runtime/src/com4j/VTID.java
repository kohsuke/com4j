package com4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Virtual table index of the method.
 *
 * <p>
 * Java doesn't let us obtain the ordinal of a method,
 * so we need to annotate that information explicitly.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface VTID {
    /**
     * Ordinal of this COM method among methods on the same interface.
     *
     * <p>
     * 0 is always QueryInterface, 1 is always AddRef, and
     * 2 is always Release.
     */
    int value();
}
