package com4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * {@link InvocationHandler} that backs up a COM object.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (ScM, (C) 2008, 2009, Michael-Schnell@gmx.de)
 */
final class Wrapper implements InvocationHandler, Com4jObject {
    /**
     * name of this wrapper. This is for debug purposes.
     */
    private String name;


    /**
     * interface pointer.
     */
    private final int ptr;
    
    private boolean isDisposed = false;

    /**
     * Cached hash code. The value of {@code IUnknown*}.
     */
    private int hashCode=0;

    /**
     * All the invocation to the wrapper COM object must go through this thread.
     */
    final ComThread thread;

    /**
     * Cached of {@link ComMethod} keyed by the method declaration.
     *
     * TODO: revisit the cache design
     */
    private Map<Method,ComMethod> cache = Collections.synchronizedMap(
        new WeakHashMap<Method,ComMethod>());

    /**
     * Wraps a new COM object. The pointer needs to be addRefed by the caller if needed.
     */
    private Wrapper(int ptr) {
        if(ptr==0)   throw new IllegalArgumentException();
        assert ComThread.isComThread();

        this.ptr = ptr;
        thread = ComThread.get();
        thread.addLiveObject(this);
    }

    /**
     * Creates a new proxy object to a given COM pointer.
     * <p>
     * Must be run from a {@link ComThread}. This method doesn't do AddRef.
     */
    static <T extends Com4jObject>
    T create( Class<T> primaryInterface, int ptr ) {
        Wrapper w = new Wrapper(ptr);
        T r = primaryInterface.cast(Proxy.newProxyInstance(
            primaryInterface.getClassLoader(),
            new Class<?>[]{primaryInterface},
                w));
        return r;
    }

    /**
     * Creates a new proxy object to a given COM pointer.
     * <p>
     * Must be run from a {@link ComThread}.
     */
    static Com4jObject create( int ptr ) {
        Wrapper w = new Wrapper(ptr);
        return w;
    }


    /**
     * Returns the wrapped interface pointer as an integer
     * @return The wrapped interface pointer.
     */
    @Override
    public int getPtr() {
        return ptr;
    }

    @Override
    public ComThread getComThread(){
      return thread;
    }

    /**
     * Adds this wrapper to the freeList of the thread
     */
    protected void finalize() throws Throwable {
        this.dispose();
        super.finalize();
    }

    private static final Object[] EMPTY_ARRAY = new Object[0];

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(isDisposed)
            throw new IllegalStateException("COM object is already disposed");
        if(args==null)  // this makes the processing easier
            args = EMPTY_ARRAY;

        Class<?> declClazz = method.getDeclaringClass();

        if( declClazz==Com4jObject.class || declClazz==Object.class ) {
            // method declared on Com4jObject is not meant to be delegated.
            try {
                return method.invoke(this,args);
            } catch( IllegalAccessException e ) {
                throw new IllegalAccessError(e.getMessage());
            } catch( InvocationTargetException e ) {
                throw e.getTargetException();
            }
        }
        UseDefaultValues useDefaultValues = method.getAnnotation(UseDefaultValues.class);

        if(useDefaultValues != null){
          int defValCount = useDefaultValues.optParamIndex().length;
          Object[] newArgs = new Object[args.length + defValCount];
          // fill in the given arguments to the right place:
          for(int i = 0; i < args.length; i++){
            newArgs[useDefaultValues.paramIndexMapping()[i]] = args[i];
          }
          // Fill in the (optional) default values:
          ComMethod comMethod = getMethod(method);
          for(int i = 0; i < defValCount; i++){
            Object defParam =  comMethod.defaultParameters[i];
            newArgs[useDefaultValues.optParamIndex()[i]] = defParam;
          }
          args = newArgs;
        }

        if(invCache==null)
            invCache = new InvocationThunk();
        try {
            return invCache.invoke(getMethod(method),args);
        } catch (ExecutionException e) {
            if(e.getCause() instanceof ComException)
                throw new ComException((ComException)e.getCause());
            throw e;
        }
    }

    private ComMethod getMethod(Method method) {
        ComMethod r = cache.get(method);
        if(r!=null)     return r;

        r = createComMethod(method);
        cache.put(method,r);
        return r;
    }

    private ComMethod createComMethod(Method method) {
        ReturnValue rv = method.getAnnotation(ReturnValue.class);
        if(rv!=null && rv.defaultPropertyThrough().length>0)
            return new DefaultedComMethod(method,rv);

        // prefer the custom interface.
        VTID vtid = method.getAnnotation(VTID.class);
        if(vtid != null){
            return new StandardComMethod(method);
        }

        DISPID dispid = method.getAnnotation(DISPID.class);
        if(dispid!=null)
            return new DispatchComMethod(method);

        throw new IllegalAnnotationException("Missing annotation: You need to specify at least one of @DISPID or @VTID");
    }

    /**
     * Disposes the native part of this Wrapper. That is, calling Release on the interface pointer. After a wrapper is disposed,
     * every call to a COM method will raise an {@link IllegalStateException}
     */
    public void dispose() {
        if(!isDisposed) {
            new Task<Void>() {
                public Void call() {
                    dispose0();
                    return null;
                }
            }.execute(thread); // Issue 39 fixed.
        }
    }

    /**
     * Called from {@link ComThread} to actually call IUnknown::Release.
     * If this Wrapper was already disposed no action is made.
     */
    void dispose0() {
        if(!isDisposed){
          Native.release(ptr);
          isDisposed = true;
          thread.removeLiveObject(this);
        }
    }

    public <T extends Com4jObject> boolean is( Class<T> comInterface ) {
        try {
            GUID iid = COM4J.getIID(comInterface);
            return new QITestTask(iid).execute(thread)!=0;
        } catch( ComException e ) {
            return false;
        }
    }

    /**
     * Returns whether this object was already disposed.
     * @return true if this object was disposed, false otherwise.
     */
    public boolean isDisposed() {
      return isDisposed;
    }
    
    public <T extends Com4jObject> T queryInterface( final Class<T> comInterface ) {
        return new Task<T>() {
            public T call() {
                GUID iid = COM4J.getIID(comInterface);
                int nptr = Native.queryInterface(ptr,iid);
                if(nptr==0)
                    return null;    // failed to cast
                return create( comInterface, nptr );
            }
        }.execute(thread);
    }

    public <T> EventProxy<?> advise(final Class<T> eventInterface, final T object) {
        return new Task<EventProxy<?>>() {
            public EventProxy<?> call() {
                IConnectionPointContainer cpc = queryInterface(IConnectionPointContainer.class);
                if(cpc==null)
                    throw new ComException("This object doesn't have event source",-1);
                GUID iid = COM4J.getIID(eventInterface);
                Com4jObject cp = cpc.FindConnectionPoint(iid);
                EventProxy<T> proxy = new EventProxy<T>(eventInterface, object);
                proxy.nativeProxy = Native.advise(cp.getPtr(), proxy,iid.v[0], iid.v[1]);

                // clean up resources to be nice
                cpc.dispose();
                cp.dispose();

                return proxy;
            }
        }.execute();
    }

    @Override
    public void setName(String name){
        this.name = name;
    }

    public String toString() {
        if(name == null) {
            return "ComObject:"+Integer.toHexString(ptr);
        } else {
            return name+":"+Integer.toHexString(ptr);
        }
    }

    public final int hashCode() {
        if(hashCode==0) {
            if(isDisposed) {
              hashCode = 0;
            } else {
              hashCode = new QITestTask(COM4J.IID_IUnknown).execute(thread);
            }
        }
        return hashCode;
    }

    public final boolean equals( Object rhs ) {
        if(!(rhs instanceof Com4jObject))   return false;
        return hashCode()==rhs.hashCode();
    }

    /**
     * Used to pass parameters/return values between the host thread
     * and the peer {@link ComThread}.
     */
    private class InvocationThunk extends Task<Object> {
        private ComMethod method;
        private Object[] args;

        /**
         * Invokes the method on the peer {@link ComThread} and returns
         * its return value.
         * @param method The {@link ComMethod} to invoke
         * @param args The arguments of the method
         * @return Returns the return value of the invoked method
         */
        public synchronized Object invoke( ComMethod method, Object[] args ) {
            invCache = null;
            this.method = method;
            this.args = args;

            try {
                return execute(thread);
            } finally {
                invCache = this;
            }
        }

        /**
         * Called from {@link ComThread} to actually carry out the execution.
         * @return Returns the return value of the invoked method
         */
        public synchronized Object call() {
            Object r = method.invoke(ptr,args);
            // clear fields that are no longer necessary
            method = null;
            args = null;
            return r;
        }
    }

    /**
     * We cache up to one InvocationThunk.
     */
    InvocationThunk invCache;



    /**
     * Invokes QueryInterface but immediately releases that pointer.
     * Useful for checking if an object implements a particular interface.
     */
    private final class QITestTask extends Task<Integer> {
        private final GUID iid;

        public QITestTask(GUID iid) {
            this.iid = iid;
        }

        public Integer call() {
            int nptr = Native.queryInterface(ptr,iid);
            if(nptr!=0) {
              Native.release(nptr);
            }
            return nptr;
        }
    }
}
