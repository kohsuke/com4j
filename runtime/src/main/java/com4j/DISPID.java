package com4j;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Dispatch ID of the method.
 *
 * <p>
 * Java doesn't let us obtain the ordinal of a method,
 * so we need to annotate that information explicitly.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DISPID {
    /**
     * DISPID used by {@code IDispatch}.
     */
    int value();
}
