package com4j;




/**
 * Native methods implemented in the dll.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class Native {

    /**
     * Initializes the native code.
     */
    static native void init();

    /**
     * Creates a COM object and returns its pointer.
     */
    static native int createInstance( String clsid, long iid1, long iid2 );

    /**
     * Calls <tt>Release</tt>.
     */
    static native void release( int pComObject );

    /**
     * Invokes a method.
     */
    static native<RetT> RetT invoke( int pComObject, int vtIndex,
         Object[] args, int[] parameterConversions,
         Class<RetT> returnType, int returnIndex, boolean returnIsInOut, int returnConversion );

    static native int queryInterface( int pComObject, long iid1, long iid2 );

    /**
     * Loads a type library from a given file, wraps it, and returns its IUnknown.
     */
    static native int loadTypeLibrary(String name);
}
