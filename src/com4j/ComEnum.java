package com4j;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * When enums need uncontinuous values, implement this interface and
 * have each constant return its numeric value.
 *
 * <p>
 * When an {@link Enum} class doesn't implement this interface,
 * its {@link Enum#ordinal()} is used as its numeric value.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ComEnum {
    /**
     * The integer assigned to this constant.
     */
    int comEnumValue();
}
