package com4j;

/**
 * Root of all the com4j interfaces.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface Com4jObject {
    /**
     * Tests the identity of two COM objects.
     *
     * <p>
     * If one COM object implements two interfaces, in Java
     * you see them as two different objects. Thus you
     * cannot rely on <tt>==</tt> to check if they represent
     * the same COM object.
     */
    boolean equals(Object o);

    /**
     * Hash code consistent with {@link #equals(java.lang.Object)} }
     */
    int hashCode();
}
