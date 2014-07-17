package com4j;

/**
 * When an enum needs uncontinuous values, implement this interface and
 * have each constant return its numeric value.
 *
 * <p>
 * When an {@link Enum} class doesn't implement this interface,
 * its {@link Enum#ordinal()} is used as its numeric value.
 * </p>
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ComEnum {
    /**
     * The integer assigned to this constant.
     * @return the value of the enum constant
     */
    int comEnumValue();
}
