package com4j;

import java.io.File;
import java.lang.reflect.Proxy;
import java.net.URL;

/**
 * The root of the COM4J library.
 *
 * <p>
 * Provides various global services that don't fit into the rest of classes.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class COM4J {
    private COM4J() {} // no instanciation allowed

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
        loadNativeLibrary();
    }

    private static void loadNativeLibrary() {
        try {
            // load the native part of the code.
            // first try java.library.path
            System.loadLibrary("com4j");
            return;
        } catch( Throwable t ) {
            ;
        }

        // try loading com4j.dll in the same directory as com4j.jar
        URL res = COM4J.class.getClassLoader().getResource("com4j/COM4J.class");
        String url = res.toExternalForm();
        if(url.startsWith("jar://")) {
            int idx = url.lastIndexOf('!');
            String filePortion = url.substring(6,idx);
            if(filePortion.startsWith("file://")) {
                File jarFile = new File(filePortion.substring(7));
                File dllFile = new File(jarFile.getParentFile(),"com4j.dll");
                System.load(dllFile.getPath());
                return;
            }
        }

        throw new UnsatisfiedLinkError("Unable to load com4j.dll");
    }
}
