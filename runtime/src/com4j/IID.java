package com4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a COM interface by its IID.
 *
 * <p>
 * This annotation is used on interfaces derived from
 * {@link Com4jObject} to designate the IID of that interface.
 *
 * <p>
 * The runtime uses this information for various purposes.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface IID {
    /**
     * GUID as a string like "<tt>{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</tt>".
     */
    String value();
}
