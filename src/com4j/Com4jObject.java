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

    /**
     * Prints the raw interface pointer that this object represents.
     */
    String toString();

    /**
     * Releases a reference to the wrapped COM object.
     *
     * <p>
     * Since Java objects tend to live longer in memory until it's GC-ed,
     * and applications have generally no control over when it's GC-ed,
     * calling the release method earlier enables applications to release
     * the COM objects deterministically.
     */
    void release();

    /**
     * Invokes the queryInterface of the wrapped COM object and attempts
     * to obtain a different interface of the same object.
     *
     * @return null
     *      if the queryInterface fails.
     */
    <T extends Com4jObject> T queryInterface( Class<T> comInterface );
}
