package com4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
final class Wrapper implements InvocationHandler, Com4jObject {

    /**
     * interface pointer.
     */
    private int ptr;

    /**
     * Cached hash code. The value of IUnknown*
     */
    private int hashCode=0;

    /**
     * Used to form a linked list for {@link ComThread#freeList}.
     */
    Wrapper next;

    /**
     * All the invocation to the wrapper COM object must go through this thread.
     */
    private final ComThread thread;

    /**
     * Cached of {@link MethodInfo} keyed by the method decl.
     *
     * TODO: revisit the cache design
     */
    private Map<Method,MethodInfo> cache = Collections.synchronizedMap(
        new WeakHashMap<Method,MethodInfo>());

    Wrapper(int ptr) {
        this.ptr = ptr;
        thread = ComThread.get();
        thread.addLiveObject();
    }

    int getPtr() {
        return ptr;
    }

    protected void finalize() throws Throwable {
        if(ptr!=0)
            thread.addToFreeList(this);
    }

    private static final Object[] EMPTY_ARRAY = new Object[0];

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(ptr==0)
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

        if(invCache==null)
            invCache = new InvocationThunk();
        return invCache.invoke(getMethodInfo(method),args);
    }

    private MethodInfo getMethodInfo(Method method) {
        MethodInfo r = cache.get(method);
        if(r!=null)     return r;
        r = new MethodInfo(method);
        cache.put(method,r);
        return r;
    }

    public void release() {
        if(ptr!=0) {
            thread.execute(new Task() {
                void run() {
                    dispose0();
                }
            });
        }
    }

    /**
     * Called from {@link ComThread} to actually call IUnknown::Release.
     */
    void dispose0() {
        assert ptr!=0;
        Native.release(ptr);
        ptr=0;
    }

    public <T extends Com4jObject> boolean is( Class<T> comInterface ) {
        try {
            GUID iid = COM4J.getIID(comInterface);
            return new QITestTask(iid).invoke()!=0;
        } catch( ComException e ) {
            return false;
        }
    }

    public <T extends Com4jObject> T queryInterface( Class<T> comInterface ) {
        GUID iid = COM4J.getIID(comInterface);
        int nptr = new QITask(iid).invoke();
        if(nptr==0)
            return null;    // failed to cast
        return COM4J.wrap( comInterface, nptr );
    }

    public String toString() {
        return "ComObject:"+Integer.toHexString(ptr);
    }

    public final int hashCode() {
        if(hashCode==0) {
            if(ptr!=0) {
                hashCode = new QITestTask(COM4J.IID_IUnknown).invoke();
            } else {
                hashCode = 0;
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
    private class InvocationThunk extends Task {
        private MethodInfo method;
        private Object[] args;
        private Object returnValue;
        private RuntimeException exception;

        /**
         * Invokes the method on the peer {@link ComThread} and returns
         * its return value.
         */
        public synchronized Object invoke( MethodInfo method, Object[] args ) {
            invCache = null;
            this.method = method;
            this.args = args;

            thread.execute(this);

            invCache = this;

            if( exception!=null ) {
                exception.fillInStackTrace();
                RuntimeException e = exception;
                exception = null;
                throw e;
            } else {
                Object r = returnValue;
                returnValue = null;
                return r;
            }
        }

        /**
         * Called from {@link ComThread} to actually carry out the execution.
         */
        public synchronized void run() {
            try {
                returnValue = method.invoke(ptr,args);
                exception = null;
            } catch( RuntimeException e ) {
                exception = e;
                returnValue = null;
            }
            // clear fields that are no longer necessary
            method = null;
            args = null;
            // let the caller thread know.
            notify();
        }
    }

    /**
     * We cache up to one {@link InvocationThunk}.
     */
    InvocationThunk invCache;


    /**
     * {@link Task} implementation that invokes QueryInterface.
     *
     * @author Kohsuke Kawaguchi (kk@kohsuke.org)
     */
    private final class QITask extends Task {
        private final GUID iid;

        /** the result. */
        private int nptr;

        public QITask(GUID iid) {
            this.iid = iid;
        }

        int invoke() {
            thread.execute(this);
            return nptr;
        }

        void run() {
            nptr = Native.queryInterface(ptr,iid.v[0],iid.v[1]);
        }
    }

    /**
     * Invokes QueryInterface but immediately releases that pointer.
     * Useful for checking if an object implements a particular interface.
     */
    private final class QITestTask extends Task {
        private final GUID iid;

        /** the result. */
        private int nptr;

        public QITestTask(GUID iid) {
            this.iid = iid;
        }

        int invoke() {
            thread.execute(this);
            return nptr;
        }

        void run() {
            nptr = Native.queryInterface(ptr,iid.v[0],iid.v[1]);
            if(nptr!=0)
                Native.release(nptr);
        }
    }
}
