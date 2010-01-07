package com4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controls how a Java parameter should be marshaled
 * to a native type.
 *
 * <p>
 * This annotation is used on parameters of wrapped COM methods
 * to control the conversion to the native types.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface MarshalAs {
    /**
     * Returns the NativeType to which this Java parameter should be marshaled.
     * @return the NativeType to which this Java parameter should be marshaled.
     */
    NativeType value();
}
