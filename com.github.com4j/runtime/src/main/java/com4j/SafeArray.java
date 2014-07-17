package com4j;

/**
 * TODO: General purpose wrapper for COM SAFEARRAY.
 *
 * <p>
 * This class is provided for rare circumstances where the Java code
 * needs to control SAFEARRAY more precisely.
 *
 * <p>
 * Users are encouraged to use plain Java arrays
 * as much as possible. For example, the following Java method:
 * <pre>
 * void foo( short[] args );
 * </pre>
 * would be bridged to the following COM method:
 * <pre>
 * HRESULT foo( [in] SAFEARRAY(short)* args );
 * </pre>
 *
 * <p>
 * This works for the most of the cases, and is much easier to use.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class SafeArray {
    /**
     * Pointer to the allocated SAFEARRAY.
     */
    private int ptr;

    public SafeArray( Variant.Type type, Bound[] bounds ) {
    }

    /**
     * Bound of an array index.
     */
    public static final class Bound {
        public int lbound;
        public int ubound;
    }

    public native Object get( int... indices );
    public native void set( int... indices );
}
