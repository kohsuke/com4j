package com4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class Wrapper implements InvocationHandler {

    /**
     * interface pointer.
     */
    private int ptr;

    /**
     * Cached of {@link MethodInfo} keyed by the method decl.
     *
     * TODO: revisit the cache design
     */
    private Map<Method,MethodInfo> cache = Collections.synchronizedMap(
        new WeakHashMap<Method,MethodInfo>());

    Wrapper(int ptr) {
        this.ptr = ptr;
    }

    protected void finalize() throws Throwable {
        release();
    }


    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(ptr==0)
            throw new IllegalStateException("COM object is already disposed");

        return getMethodInfo(method).invoke(ptr,args);
    }

    private MethodInfo getMethodInfo(Method method) {
        MethodInfo r = cache.get(method);
        if(r!=null)     return r;
        r = new MethodInfo(method);
        cache.put(method,r);
        return r;
    }

    public void release() {
        if(ptr!=0)
           Native.release(ptr);
        ptr=0;
    }


    static class MethodInfo {
        final Method method;

        final int vtIndex;
        final int[] paramConvs;
        final int returnIndex;
        final boolean returnIsInOut;
        final int returnConv;

        MethodInfo( Method m ) {
            method = m;

            VTID vtid = m.getAnnotation(VTID.class);
            if(vtid==null)
                throw new IllegalAnnotationException("@VTID is missing: "+m.toGenericString());
            vtIndex = vtid.value();

            ReturnValue rt = m.getAnnotation(ReturnValue.class);
            if(rt!=null) {
                returnIndex = rt.index();
                returnIsInOut = rt.inout();
                returnConv = rt.type().code;
            } else {
                // guess the default
                if( method.getReturnType()==Void.TYPE ) {
                    // no return type
                    returnIndex = -1;
                    returnIsInOut = false;
                    returnConv = -1;
                } else {
                    // TODO
                    throw new UnsupportedOperationException();
                }
            }

            // TODO
            paramConvs = new int[] { NativeType.BSTR.code, NativeType.BSTR_ByRef.code };
        }

        Object invoke( int ptr, Object[] args ) {
            return Native.invoke( ptr, vtIndex, args, paramConvs,
                method.getReturnType(), returnIndex, returnIsInOut, returnConv );
        }
    }
}
