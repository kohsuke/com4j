package com4j;

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
        int ptr = Native.createInstance(clsid,iid.l1,iid.l2);

        return (T)Proxy.newProxyInstance(
            primaryInterface.getClassLoader(),
            new Class<?>[]{primaryInterface},
            new Wrapper(ptr));
    }

    /**
     * Gets the interface VTID associated with the given interface.
     */
    public static GUID getIID( Class<? extends Com4jObject> _interface ) {
        IID iid = _interface.getAnnotation(IID.class);
        return new GUID(iid.value());
    }

    /**
     * Releases the COM object that the given wrapper holds.
     */
    public static void dispose( Com4jObject obj ) {
        unwrap(obj).release();
    }

    private static Wrapper unwrap( Com4jObject obj ) {
        return (Wrapper)Proxy.getInvocationHandler(obj);
    }

    static {
        System.out.println("loading the native code");
        System.loadLibrary("com4j");
    }
}
