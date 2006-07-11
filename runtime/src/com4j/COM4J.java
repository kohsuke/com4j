package com4j;

import java.io.File;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;

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

    /**
     * Creates a new COM object of the given CLSID and returns
     * it in a wrapped interface.
     *
     * @param primaryInterface
     *      The created COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param clsid
     *      The CLSID of the COM object to be created. Must be non-null.
     *
     * @return
     *      non-null valid object.
     *
     * @throws ComException
     *      if the instanciation fails.
     */
    public static<T extends Com4jObject>
    T createInstance( Class<T> primaryInterface, GUID clsid ) throws ComException {
        return createInstance(primaryInterface,clsid.toString());
    }

    /**
     * Creates a new COM object of the given CLSID and returns
     * it in a wrapped interface.
     *
     * @param primaryInterface
     *      The created COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param clsid
     *      The CLSID of the COM object in the
     *      "<tt>{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</tt>" format,
     *      or the ProgID of the object (like "Microsoft.XMLParser.1.0")
     *
     * @return
     *      non-null valid object.
     *
     * @throws ComException
     *      if the instanciation fails.
     */
    public static<T extends Com4jObject>
    T createInstance( Class<T> primaryInterface, String clsid ) throws ComException {

        // create instance
        return createInstance(primaryInterface,clsid,CLSCTX.ALL);
    }

    /**
     * Creates a new COM object of the given CLSID and returns
     * it in a wrapped interface.
     *
     * <p>
     * Compared to {@link #createInstance(Class,String)},
     * this method allows the caller to specify <tt>CLSCTX_XXX</tt>
     * constants to control the server instanciation.
     *
     * @param clsctx
     *      Normally this is {@link CLSCTX#ALL}, but can be any combination
     *      of {@link CLSCTX} constants.
     *
     * @see CLSCTX
     */
    public static<T extends Com4jObject>
    T createInstance( Class<T> primaryInterface, String clsid, int clsctx ) throws ComException {

        // create instance
        return new CreateInstanceTask<T>(clsid,clsctx,primaryInterface).execute();
    }

    private static class CreateInstanceTask<T extends Com4jObject> extends Task<T> {
        private final String clsid;
        private final int clsctx;
        private final Class<T> intf;

        public CreateInstanceTask(String clsid, int clsctx, Class<T> intf) {
            this.clsid = clsid;
            this.clsctx = clsctx;
            this.intf = intf;
        }

        public T call() {
            GUID iid = getIID(intf);
            return Wrapper.create( intf, Native.createInstance(clsid,clsctx,iid.v[0],iid.v[1]) );
        }
    }

    /**
     * Gets an already object from the running object table.
     *
     * @param primaryInterface
     *      The returned COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param clsid
     *      The CLSID of the object to be retrieved.
     *
     * @throws ComException
     *      if the retrieval fails.
     *
     * @see
     *  <a href="http://msdn2.microsoft.com/en-us/library/ms221467.aspx">MSDN documentation</a>
     */
    public static <T extends Com4jObject> T getActiveObject(Class<T> primaryInterface, GUID clsid ) {
        return new GetActiveObjectTask<T>(clsid,primaryInterface).execute();
    }

    /**
     * Gets an already object from the running object table.
     *
     * @param primaryInterface
     *      The returned COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param clsid
     *      The CLSID of the COM object to be retrieved, in the
     *      "<tt>{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</tt>" format,
     *      or the ProgID of the object (like "Microsoft.XMLParser.1.0")
     *
     * @return
     *      non-null valid object.
     *
     * @throws ComException
     *      if the retrieval fails.
     *
     * @see #getActiveObject(Class,GUID)
     */
    public static <T extends Com4jObject> T getActiveObject(Class<T> primaryInterface, String clsid ) {
        return getActiveObject(primaryInterface,new GUID(clsid));
    }

    private static class GetActiveObjectTask<T extends Com4jObject> extends Task<T> {
        private final GUID clsid;
        private final Class<T> intf;

        public GetActiveObjectTask(GUID clsid, Class<T> intf) {
            this.clsid = clsid;
            this.intf = intf;
        }

        public T call() {
            GUID iid = getIID(intf);
            int o1 = Native.getActiveObject(clsid.v[0], clsid.v[1]);
            int o2 = Native.queryInterface(o1, iid.v[0], iid.v[1]);
            Native.release(o1);
            return Wrapper.create(intf,o2);
        }
    }

    /**
     * Gets the interface GUID associated with the given interface.
     *
     * <p>
     * This method retrieves the associated {@link IID} annotation from the
     * interface and return it.
     *
     * @throws IllegalArgumentException
     *      if the interface doesn't have any {@link IID} annotation.
     *
     * @return
     *      always return no-null valid {@link GUID} object.
     */
    public static GUID getIID( Class<?> _interface ) {
        IID iid = _interface.getAnnotation(IID.class);
        if(iid==null)
            throw new IllegalArgumentException(_interface.getName()+" doesn't have @IID annotation");
        return new GUID(iid.value());
    }

    /**
     * Loads a type library from a given file and returns its IUnknown.
     *
     * <p>
     * Exposed for <tt>tlbimp</tt>.
     */
    public static Com4jObject loadTypeLibrary( final File typeLibraryFile ) {
        return new Task<Com4jObject>() {
            public Com4jObject call() {
                return Wrapper.create(
                    Native.loadTypeLibrary(typeLibraryFile.getAbsolutePath()));
            }
        }.execute();
    }

    /**
     * Maps the memory region into {@link ByteBuffer} so that it can be
     * then accessed nicely from Java code.
     *
     * <p>
     * When bridging native code to Java, it's often necessary to be able
     * to read/write arbitrary portion of the memory, and this method
     * lets you do that.
     *
     * <p>
     * Neither this code nor {@link ByteBuffer} does anything about
     * making sure that the memory region pointed by {@code ptr} remains
     * valid. It's the caller's responsibility.
     *
     * @see http://java.sun.com/j2se/1.4.2/docs/guide/jni/jni-14.html#NewDirectByteBuffer
     *
     * @param ptr
     *      The pointer value that points to the top of the buffer.
     * @param size
     *      The size of the memory region to be mapped to {@link ByteBuffer}.
     *
     * @return
     *      always non-null valid {@link ByteBuffer}.
     */
    public static ByteBuffer createBuffer( int ptr, int size ) {
        return Native.createBuffer(ptr,size);
    }

    /**
     * GUID of IUnknown.
     */
    public static final GUID IID_IUnknown = new GUID("{00000000-0000-0000-C000-000000000046}");

    /**
     * GUID of IDispatch.
     */
    public static final GUID IID_IDispatch = new GUID("{00020400-0000-0000-C000-000000000046}");



    /**
     * Registers a {@link ComObjectListener} to the current thread.
     *
     * <p>
     * The registered listener will receive a notification each time
     * a new proxy is created.
     *
     * @throws IllegalArgumentException
     *      If the listener is null or it is already registered.
     *
     * @see #removeListener(ComObjectListener)
     */
    public static void addListener( ComObjectListener listener ) {
        ComThread.get().addListener(listener);
    }

    /**
     * Removes a registered {@link ComObjectListener} from the current thread.
     *
     * @param listener
     *      this listner has to be registered via {@link #addListener(ComObjectListener)}.
     *
     * @throws IllegalArgumentException
     *      If the listener is not currently registered.
     *
     * @see #addListener(ComObjectListener)
     */
    public static void removeListener( ComObjectListener listener ) {
        ComThread.get().removeListener(listener);
    }

    /**
     * Cleans up COM resources for the current thread.
     *
     * <p>
     * This method can be invoked explicitly by a thread that used COM objects,
     * to clean up resources, such as references to out-of-process COM objects.
     *
     * <p>
     * In COM terminology, this effectively amounts to calling {@code CoUninitialize}.
     *
     * After this method is invoked, a thread can still go use other COM resources.
     */
    public static void cleanUp() {
        ComThread.detach();
    }

    static int queryInterface( int ptr, GUID iid ) {
        return Native.queryInterface(ptr,iid.v[0],iid.v[1]);
    }

    static Wrapper unwrap( Com4jObject obj ) {
        if( obj instanceof Wrapper )
            return (Wrapper)obj;
        else
            return (Wrapper)Proxy.getInvocationHandler(obj);
    }

    // called by the native side to get the raw pointer value of Com4jObject.
    static int getPtr( Com4jObject obj ) {
        if(obj==null)   return 0;
        return unwrap(obj).getPtr();
    }

    static {
        loadNativeLibrary();
        Native.init();
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
        if(url.startsWith("jar:")) {
            int idx = url.lastIndexOf('!');
            String filePortion = url.substring(4,idx);
            while(filePortion.startsWith("/"))
                filePortion = filePortion.substring(1);

            if(filePortion.startsWith("file:/")) {
                filePortion = filePortion.substring(6);
                if(filePortion.startsWith("//"))
                    filePortion = filePortion.substring(2);
                filePortion = URLDecoder.decode(filePortion);
                File jarFile = new File(filePortion);
                File dllFile = new File(jarFile.getParentFile(),"com4j.dll");
                System.load(dllFile.getPath());
                return;
            }
        }

        throw new UnsatisfiedLinkError("Unable to load com4j.dll");
    }
}
