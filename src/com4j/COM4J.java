package com4j;

import java.io.File;
import java.lang.reflect.Proxy;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class COM4J {

    public static<T extends Com4jObject>
    T createInstance( Class<T> primaryInterface, GUID guid ) {
        return createInstance(primaryInterface,guid.toString());
    }

    public static<T extends Com4jObject>
    T createInstance( Class<T> primaryInterface, String clsid ) {

        GUID iid = getIID(primaryInterface);

        // create instance
        return wrap( primaryInterface, Native.createInstance(clsid,iid.v[0],iid.v[1]) );
    }

    /**
     * Gets the interface GUID associated with the given interface.
     */
    public static GUID getIID( Class<? extends Com4jObject> _interface ) {
        IID iid = _interface.getAnnotation(IID.class);
        return new GUID(iid.value());
    }

    /**
     * Loads a type library from a given file and returns its IUnknown.
     */
    public static Com4jObject loadTypeLibrary( File typeLibraryFile ) {
        return wrap(Com4jObject.class, Native.loadTypeLibrary(typeLibraryFile.getAbsolutePath()));
    }

    /**
     * GUID of IUnknown.
     */
    public static final GUID IID_IUnknown = new GUID("{00000000-0000-0000-C000-000000000046}");

    /**
     * GUID of IDispatch.
     */
    public static final GUID IID_IDispatch = new GUID("{00020400-0000-0000-C000-000000000046}");


    static <T>
    T wrap( Class<T> primaryInterface, int ptr ) {
        return primaryInterface.cast(Proxy.newProxyInstance(
            primaryInterface.getClassLoader(),
            new Class<?>[]{primaryInterface},
            new Wrapper(ptr)));
    }

    static int queryInterface( int ptr, GUID iid ) {
        return Native.queryInterface(ptr,iid.v[0],iid.v[1]);
    }

    static Wrapper unwrap( Com4jObject obj ) {
        return (Wrapper)Proxy.getInvocationHandler(obj);
    }

    static {
        System.loadLibrary("com4j");
    }
}
