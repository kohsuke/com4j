package com4j;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Specifies which method parameter in the COM method
 * is used as the return value
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ReturnValue {
    /**
     * The index of the parameter with [retval] marker.
     *
     * <p>
     * If 0, the retval parameter is the 1st parameter in the parameter list.
     */
    int index();

    /**
     * True if the parameter is "in/out" (therefore it shows up in the
     * Java parameter list.) Otherwise, the return value is not a part of the
     * Java parameter list.
     */
    boolean inout() default false;

    /**
     * The native type to be unmarshalled.
     *
     * <p>
     * Becase a "retval" parameter is always passed by reference
     * in IDL, this is usually <tt>{@link NativeType}.XXX_ByRef</tt>.
     */
    NativeType type();
}
