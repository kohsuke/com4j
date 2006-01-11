package com4j;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Marks a "default property" method.
 *
 * <p>
 * This is used in conjunction with {@link ReturnValue#defaultPropertyThrough()}
 * to mark methods are implicitly invoked.
 *
 * @author Kohsuke Kawaguchi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DefaultMethod {
}
