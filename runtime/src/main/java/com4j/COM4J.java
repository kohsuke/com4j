package com4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The root of the COM4J library.
 *
 * <p>
 * Provides various global services that don't fit into the rest of classes.
 * </p>
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (ScM, (C) 2008, 2009 Michael-Schnell@gmx.de)
 */
public abstract class COM4J {
    private COM4J() {} // no instantiation allowed

    /**
     * Creates a new COM object of the given CLSID and returns
     * it in a wrapped interface.
     *
     * @param primaryInterface The created COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param clsid The CLSID of the COM object to be created. Must be non-null.
     *
     * @param <T> the type of the return value and the type parameter of the class object of primaryInterface
     *
     * @return non-null valid object.
     *
     * @throws ComException if the instantiation fails.
     */
    public static<T extends Com4jObject>
    T createInstance( Class<T> primaryInterface, GUID clsid ) throws ComException {
        return createInstance(primaryInterface,clsid.toString());
    }

    /**
     * Creates a new COM object of the given CLSID and returns
     * it in a wrapped interface.
     *
     * @param primaryInterface The created COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param clsid The CLSID of the COM object in the
     *      "<tt>{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</tt>" format,
     *      or the ProgID of the object (like "Microsoft.XMLParser.1.0")
     *
     * @param <T> the type of the return value and the type parameter of the class object of primaryInterface
     *
     * @return non-null valid object.
     *
     * @throws ComException if the instantiation fails.
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
     * constants to control the server instantiation.
     *
     * @param primaryInterface type parameter of the primaryInterface type
     * @param clsid a string representation of the class id
     * @param clsctx Normally this is {@link CLSCTX#ALL}, but can be any combination of {@link CLSCTX} constants.
     * @param <T> the type of the return value and the type parameter of the class object of primaryInterface
     * @return the new instance of the COM object
     * @throws ComException if an error occurred in the native COM part
     *
     * @see CLSCTX
     */
    public static<T extends Com4jObject>
    T createInstance( Class<T> primaryInterface, String clsid, int clsctx ) throws ComException {

        // create instance
        return new CreateInstanceTask<T>(clsid,clsctx,primaryInterface).execute();
    }

    /**
     * Wraps the creation of a COM object in a {@link Task}
     * @param <T> the type of the return value of {@link #call()}
     */
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
     * Wraps an externally obtained interface pointer into a COM wrapper object.
     *
     * <p>
     * This method doesn't call addRef on the given interface pointer. Instead, this method
     * takes over the ownership of the given pointer.
     *
     * @param primaryInterface
     *      The interface type to wrap the pointer into.
     * @param ptr
     *      The rar interface pointer value.
     */
    public static<T extends Com4jObject>
    T wrap( final Class<T> primaryInterface, final long ptr ) throws ComException {
        return new Task<T>() {
            @Override
            public T call() {
                return Wrapper.create(primaryInterface,ptr);
            }
        }.execute();
    }

    /**
     * Wraps an externally obtained single-threaded apartment interface pointer from the current
     * thread into a COM wrapper object.
     * <p>
     * This method must be called by the same thread that the COM object belongs to.
     * Object wrappers created with this method can only be accessed from that thread.
     * <p>
     * This method doesn't call addRef on the given interface pointer. Instead, this method
     * takes over the ownership of the given pointer.
     * </p>
     *
     * @param primaryInterface
     *      The interface type to wrap the pointer into.
     * @param ptr
     *      The rar interface pointer value.
     */
    public static<T extends Com4jObject>
    T wrapSta( final Class<T> primaryInterface, final long ptr ) throws ComException {
        ComThread thread = ComThreadSingle.get();
        return new Task<T>() {
            @Override
            public T call() {
                return Wrapper.create(primaryInterface,ptr);
            }
        }.execute(thread);
    }

    /**
     * Gets an already running object from the running object table.
     *
     * @param <T> the type of the return value and the type parameter of the class object of primaryInterface
     *
     * @param primaryInterface The returned COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param clsid The CLSID of the object to be retrieved.
     *
     * @return the retrieved object
     *
     * @throws ComException if the retrieval fails.
     *
     * @see <a href="http://msdn2.microsoft.com/en-us/library/ms221467.aspx">MSDN documentation</a>
     */
    public static <T extends Com4jObject> T getActiveObject(Class<T> primaryInterface, GUID clsid ) {
        return new GetActiveObjectTask<T>(clsid,primaryInterface).execute();
    }

    /**
     * Gets an already object from the running object table.
     *
     * @param <T> the type of the return value and the type parameter of the class object of primaryInterface
     *
     * @param primaryInterface The returned COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param clsid The CLSID of the COM object to be retrieved, in the
     *      "<tt>{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</tt>" format,
     *      or the ProgID of the object (like "Microsoft.XMLParser.1.0")
     *
     * @return non-null valid object.
     *
     * @throws ComException if the retrieval fails.
     *
     * @see #getActiveObject(Class,GUID)
     */
    public static <T extends Com4jObject> T getActiveObject(Class<T> primaryInterface, String clsid ) {
        return getActiveObject(primaryInterface,new GUID(clsid));
    }

    /**
     * Wraps {@link Native#getActiveObject(long, long)}, {@link Native#queryInterface(long, long, long)}
     * and {@link Wrapper#create(int)} into a {@link Task}
     *
     * @param <T> the type of the return value of {@link #call()}
     */
    private static class GetActiveObjectTask<T extends Com4jObject> extends Task<T> {
        private final GUID clsid;
        private final Class<T> intf;

        public GetActiveObjectTask(GUID clsid, Class<T> intf) {
            this.clsid = clsid;
            this.intf = intf;
        }

        public T call() {
            GUID iid = getIID(intf);
            long o1 = Native.getActiveObject(clsid.v[0], clsid.v[1]);
            long o2 = Native.queryInterface(o1, iid.v[0], iid.v[1]);
            Native.release(o1);
            return Wrapper.create(intf,o2);
        }
    }

    /**
     * Returns a reference to a COM object primarily by loading a file.
     *
     * <p>
     * This method implements the semantics of the {@code GetObject} Visual Basic
     * function. See <a href="http://msdn2.microsoft.com/en-us/library/e9waz863(VS.71).aspx">MSDN reference</a>
     * for its semantics.
     *
     * <p>
     * This function really has three different mode of operation:
     *
     * <ul>
     * <li>
     * If both {@code fileName} and {@code progId} are specified,
     * a COM object of the given progId is created and its state is loaded
     * from the given file name. This is normally used to activate a OLE server
     * by loading a file.
     *
     * <li>
     * If just {@code fileName} is specified, it is treated as a moniker.
     * The moniker will be bound and the resulting COM object will be returned.
     * In a simple case a moniker is a file path, in which case the associated
     * application is activated and loads the data. But monikers in OLE are
     * extensible, so in more general case the semantics really depends on
     * the moniker provider.
     *
     * <li>
     * If just {@code progId} is specified, this method would just work like
     * {@link #getActiveObject(Class, String)}.
     *
     * </ul>
     * @param <T> the type of the return value and the type parameter of the class object of primaryInterface
     *
     * @param primaryInterface The returned COM object is returned as this interface.
     *      Must be non-null. Passing in {@link Com4jObject} allows
     *      the caller to create a new instance without knowing
     *      its primary interface.
     * @param fileName path to the file
     * @param progId the progID in string representation
     *
     * @return non-null valid object.
     *
     * @throws ComException if the retrieval fails.
     */
    public static <T extends Com4jObject> T getObject(Class<T> primaryInterface, String fileName, String progId ) {
        return new GetObjectTask<T>(fileName,progId,primaryInterface).execute();
    }

    /**
     * Wraps the call to {@link Native#getObject(String, String)}, {@link Native#queryInterface(long, long, long)}
     * and {@link Wrapper#create(int)} into a {@link Task}
     *
     * @param <T> the type of the return value of {@link #call()}
     */
    private static class GetObjectTask<T extends Com4jObject> extends Task<T> {
        private final String fileName,progId;
        private final Class<T> intf;

        private GetObjectTask(String fileName, String progId, Class<T> intf) {
            this.fileName = fileName;
            this.progId = progId;
            this.intf = intf;
        }

        public T call() {
            GUID iid = getIID(intf);
            long o1 = Native.getObject(fileName,progId);
            long o2 = Native.queryInterface(o1, iid.v[0], iid.v[1]);
            Native.release(o1);
            return Wrapper.create(intf,o2);
        }
    }

    /**
     * Returns the singleton ROT instance.
     * @return the singleton ROT instance
     */
    public static ROT getROT(){
      return ROT.getInstance();
    }

    /**
     * Gets the interface GUID associated with the given interface.
     *
     * <p>
     * This method retrieves the associated {@link IID} annotation from the
     * interface and return it.
     *
     * @param _interface reference to an object that has the {@link IID} annotation.
     * @return always return no-null valid {@link GUID} object.
     * @throws IllegalArgumentException if the interface doesn't have any {@link IID} annotation.
     *
     */
    public static GUID getIID( Class<?> _interface ) {
        IID iid = _interface.getAnnotation(IID.class);
        if(iid==null)
            throw new IllegalArgumentException(_interface.getName()+" doesn't have @IID annotation");
        return new GUID(iid.value());
    }

    /**
     * <p>
     * Loads a type library from a given file and returns its IUnknown.
     * </p>
     *
     * Exposed for <tt>tlbimp</tt>.
     *
     * @param typeLibraryFile the path to the file containing the type library
     * @return reference to the type library object
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
     * @see "http://java.sun.com/j2se/1.4.2/docs/guide/jni/jni-14.html#NewDirectByteBuffer"
     *
     * @param ptr The pointer value that points to the top of the buffer.
     * @param size The size of the memory region to be mapped to {@link ByteBuffer}.
     *
     * @return always non-null valid {@link ByteBuffer}.
     */
    public static ByteBuffer createBuffer( long ptr, int size ) {
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

    /** IID of IPicture */
    public static final GUID IID_IPicture     = new GUID("{7BF80980-BF32-101A-8BBB-00AA00300CAB}");
    /** IID of IPictureDisp */
    public static final GUID IID_IPictureDisp = new GUID("{7BF80981-BF32-101A-8BBB-00AA00300CAB}");
    /** IID of IFont */
    public static final GUID IID_IFont     = new GUID("{BEF6E002-A874-101A-8BBA-00AA00300CAB}");
    /** IID of IFontDisp **/
    public static final GUID IID_IFontDisp = new GUID("{BEF6E003-A874-101A-8BBA-00AA00300CAB}");


    /**
     * Registers a {@link ComObjectListener} to the current thread.
     *
     * <p>
     * The registered listener will receive a notification each time
     * a new proxy is created.
     *
     * @param listener the listener to be added
     * @throws IllegalArgumentException If the listener is null or it is already registered.
     *
     * @see #removeListener(ComObjectListener)
     */
    public static void addListener( ComObjectListener listener ) {
        ComThreadMulti.get().addListener(listener);
        ComThreadSingle.get().addListener(listener);
    }

    /**
     * Removes a registered {@link ComObjectListener} from the current thread.
     *
     * @param listener the listener to remove.
     *
     * @throws IllegalArgumentException if the listener is not currently registered.
     *
     * @see #addListener(ComObjectListener)
     */
    public static void removeListener( ComObjectListener listener ) {
        ComThreadMulti.get().removeListener(listener);
        ComThreadSingle.get().removeListener(listener);
    }

    /**
     * Cleans up COM resources for the current thread.
     *
     * <p>
     * This method can be invoked explicitly by a thread that used COM objects,
     * to clean up resources, such as references to out-of-process COM objects.
     * </p>
     *
     * In COM terminology, this effectively amounts to calling {@code CoUninitialize}.
     *
     * After this method is invoked, a thread can still go use other COM resources.
     */
    public static void cleanUp() {
        ComThreadMulti.detach();
        ComThreadSingle.detach();
    }

    /**
     * List of application defined task, they are executed _before_ com4j shuts down.
     */
    protected static final List<Runnable> applicationShutdownTasks = Collections.synchronizedList( new ArrayList<Runnable>());

    /**
     * List of shutdown task defined by Com4J itself.
     */
    protected static final List<Runnable> com4JShutdownTasks = Collections.synchronizedList( new ArrayList<Runnable>());

    /**
     * Add a shutdown task.
     * <p>
     *  Com4J adds a shutdown hook to the java runtime. As soon as the java runtime shuts down, Com4J shuts down, too. To enable the developer to release
     *  external resources, accessed via Com4J, such as an automation server object that is running in the background, this method provides a mechanism
     *  to add shutdown tasks, that are executed <strong>before</strong> Com4J really shuts down.
     * </p>
     * <p>
     *  Do not add your own shutdown hooks to the java runtime to do such things, because it is likely that the com4j shutdown hook is running first, and
     *  then you can't access the resources any more.
     * </p>
     * <p>
     *  Tasks are executed sequential in LIFO order.
     * </p>
     * @param task The task to be executed before Com4J shuts down.
     */
    public static void addShutdownTask(Runnable task) {
        applicationShutdownTasks.add(task);
    }

    /**
     * Remove an already registered shutdown task
     * @param task The task that is to remove from the list.
     * @return <code>true</code> if the task was registered.
     */
    public static boolean removeShutdownTask(Runnable task) {
        return applicationShutdownTasks.remove(task);
    }

    /**
     * Adds a shutdown task.
     * <p>
     *  These tasks are shutting down Com4J itself and are executed <strong>after</strong> all application defined shutdown task are executed.
     * </p>
     * @see #addShutdownTask(Runnable)
     * @param task The task to be added.
     */
    static void addCom4JShutdownTask(Runnable task) {
        com4JShutdownTasks.add(task);
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread("Com4J shutdown hook"){
            @Override
            public void run() {
                // first execute the application defined shutdown tasks in LIFO order
                synchronized( applicationShutdownTasks)
                {
                    for(int i = applicationShutdownTasks.size()-1 ; i>=0; i--){
                        applicationShutdownTasks.get(i).run();
                    }
                }
                // then execute the shutdown tasks defined by com4j itself in LIFO order
                synchronized( com4JShutdownTasks)
                {
                    for(int i = com4JShutdownTasks.size()-1 ; i>=0; i--){
                        com4JShutdownTasks.get(i).run();
                    }
                }
            }
        });
    }

    /**
     * Calls {@link Native#queryInterface(long, long, long)}
     * @param ptr the interface pointer
     * @param iid the IID
     * @return the queried interface pointer or null, if the query failed
     *
     * TODO: Think about whether to remove this method or mark it as deprecated. Methods could use {@link Native#queryInterface(long, GUID)} instead.
     */
    static long queryInterface( long ptr, GUID iid ) {
        return Native.queryInterface(ptr,iid.v[0],iid.v[1]);
    }

    /**
     * Unwraps a given {@link Com4jObject} (returns the Wrapper object of the Com4jObject, which might be a proxy object)
     * @param obj the Com4jObject
     * @return the {@link Wrapper} object of the {@link Com4jObject}
     * TODO: Think about whether to remove this method or mark it as deprecated, since this method is not used any more by Com4J itself
     */
    static Wrapper unwrap( Com4jObject obj ) {
        if( obj instanceof Wrapper )
            return (Wrapper)obj;
        else
            return (Wrapper)Proxy.getInvocationHandler(obj);
    }

    /**
     * @deprecated use {@link Com4jObject#getPointer()} instead.
     */
    @Deprecated
    static long getPtr( Com4jObject obj ) {
        if(obj==null)   return 0;
        return obj.getPointer();
    }

    private static final Logger LOGGER = Logger.getLogger(COM4J.class.getName());
    static {
        loadNativeLibrary();
    }

    private static void loadNativeLibrary() {
        Throwable cause;
        try {
            // load the native part of the code.
            // first try java.library.path
            System.loadLibrary("com4j-" + System.getProperty("os.arch"));
            return;
        } catch( Throwable t ) {
            cause = t;
        }

        final String fileName = "com4j-" + System.getProperty("os.arch") + ".dll";

        // try loading com4j.dll in the same directory as com4j.jar
        URL res = COM4J.class.getResource("/com4j/COM4J.class");
        String url = res.toExternalForm();
        if(url.startsWith("jar:")) {
            int idx = url.lastIndexOf('!');
            String filePortion = url.substring(4,idx);
            while(filePortion.startsWith("/"))
                filePortion = filePortion.substring(1);

            if(filePortion.startsWith("file:/")) {
                // Replaced file URL processing with recommended approach:
                // http://wiki.eclipse.org/Eclipse/UNC_Paths
                // to support UNC paths
                File newFile = new File(filePortion.substring(5));
                filePortion = newFile.toString();
                LOGGER.fine("COM4J JAR filePortion: " + newFile);
                try{
                  // this is the same as the deprecated URLDecoder.decode(String) would do.
                  filePortion = URLDecoder.decode(filePortion,  System.getProperty("file.encoding"));
                }catch (UnsupportedEncodingException e ){
                  // according to a comment in the deprecated URLDecoder.decode(String),
                  // this should never happen
                  e.printStackTrace();
                }
                File jarFile = new File(filePortion);
                File dllFile = new File(jarFile.getParentFile(), fileName);
                if(!dllFile.exists()) {
                    // try to extract from within the jar
                    try {
                        InputStream in = COM4J.class.getResourceAsStream(fileName);
                        if (in==null)   throw new IOException(fileName+" not bundled in the resource. Packaging problem?");
                        copyStream(in, new FileOutputStream(dllFile));
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to write com4j.dll", e);
                    }
                }
                System.load(dllFile.getPath());
                return;
            }
        } else
        if(url.startsWith("file:")) {
            File classFile = new File(url.substring(5));
            File dllFile = new File(classFile.getParentFile(),fileName);
            System.load(dllFile.getPath());
            return;
        }


        UnsatisfiedLinkError error = new UnsatisfiedLinkError("Unable to load com4j.dll");
        error.initCause(cause);
        throw error;
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buf = new byte[8192];
            int len;
            while((len=in.read(buf))>=0)
                out.write(buf,0,len);
        } finally {
            in.close();
            out.close();
        }
    }
}
