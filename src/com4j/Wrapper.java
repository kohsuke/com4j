package com4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.annotation.Annotation;
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
     * Cached of {@link MethodInfo} keyed by the method decl.
     *
     * TODO: revisit the cache design
     */
    private Map<Method,MethodInfo> cache = Collections.synchronizedMap(
        new WeakHashMap<Method,MethodInfo>());

    Wrapper(int ptr) {
        this.ptr = ptr;
    }

    int getPtr() {
        return ptr;
    }

    protected void finalize() throws Throwable {
        release();
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

    public <T extends Com4jObject> boolean is( Class<T> comInterface ) {
        try {
            GUID iid = COM4J.getIID(comInterface);
            int nptr = Native.queryInterface(ptr, iid.v[0], iid.v[1] );
            Native.release(nptr);
            return true;
        } catch( ComException e ) {
            return false;
        }
    }

    public <T extends Com4jObject> T queryInterface( Class<T> comInterface ) {
        try {
            GUID iid = COM4J.getIID(comInterface);
            int nptr = Native.queryInterface(ptr, iid.v[0], iid.v[1] );
            return COM4J.wrap( comInterface, nptr );
        } catch( ComException e ) {
            return null;    // failed to cast
        }
    }

    public String toString() {
        return "ComObject:"+Integer.toHexString(ptr);
    }

    public final int hashCode() {
        if(ptr==0)
            throw new IllegalStateException("COM object is already disposed");

        if(hashCode==0) {
            hashCode = COM4J.queryInterface( ptr, COM4J.IID_IUnknown );
            Native.release(hashCode);
        }
        return hashCode;
    }

    public final boolean equals( Object rhs ) {
        if(!(rhs instanceof Com4jObject))   return false;
        return hashCode()==rhs.hashCode();
    }



    static class MethodInfo {
        final Method method;

        final int vtIndex;
        // list of params.code
        final int[] paramConvs;
        final NativeType[] params;
        final int returnIndex;
        final boolean returnIsInOut;
        final NativeType returnConv;
        final Class<?>[] paramTypes;

        MethodInfo( Method m ) {
            method = m;

            VTID vtid = m.getAnnotation(VTID.class);
            if(vtid==null)
                throw new IllegalAnnotationException("@VTID is missing: "+m.toGenericString());
            vtIndex = vtid.value();

            Annotation[][] pa = m.getParameterAnnotations();
            int paramLen = pa.length;


            ReturnValue rt = m.getAnnotation(ReturnValue.class);
            if(rt!=null) {
                if(rt.index()==-1)  returnIndex=pa.length;
                else                returnIndex=rt.index();
                returnIsInOut = rt.inout();
                returnConv = rt.type();
            } else {
                // guess the default
                if( method.getReturnType()==Void.TYPE ) {
                    // no return type
                    returnIndex = -1;
                    returnIsInOut = false;
                    returnConv = NativeType.Default;    // unused
                } else {
                    returnIndex = paramLen;
                    returnIsInOut = false;
                    returnConv = getDefaultConversion(method.getReturnType());
                }
            }

            Type[] javaParamTypes = m.getGenericParameterTypes();

            paramTypes = m.getParameterTypes();
            paramConvs = new int[paramLen];
            params = new NativeType[paramLen];
            for( int i=0; i<paramLen; i++ ) {
                NativeType n=null;
                for( Annotation a : pa[i] )
                    if( a instanceof MarshalAs )
                        n = ((MarshalAs)a).value();
                if(n==null) {
                    // missing annotation
                    n = getDefaultConversion(javaParamTypes[i]);
                }
                params[i] = n;
                paramConvs[i] = n.code;
            }
        }

        Object invoke( int ptr, Object[] args ) {
            for( int i=0; i<args.length; i++ )
                args[i] = params[i].massage(args[i]);

            try {
                Object r = Native.invoke( ptr, vtIndex, args, paramConvs,
                    method.getReturnType(), returnIndex, returnIsInOut, returnConv.code );
                return returnConv.unmassage(method.getReturnType(),r);
            } finally {
                for( int i=0; i<args.length; i++ )
                    args[i] = params[i].unmassage(paramTypes[i],args[i]);
            }
        }
    }


    /**
     * Computes the default conversion for the given type.
     */
    private static NativeType getDefaultConversion(Type t) {
        if( t instanceof Class ) {
            Class<?> c = (Class<?>)t;
            if(Com4jObject.class.isAssignableFrom(c))
                return NativeType.ComObject;
            if(Enum.class.isAssignableFrom(c))
                return NativeType.Int32;
            if(GUID.class==t)
                return NativeType.GUID;
            if(Integer.TYPE==t)
                return NativeType.Int32;
            if(Short.TYPE==t)
                return NativeType.Int16;
            if(Byte.TYPE==t)
                return NativeType.Int8;
            if(String.class==t)
                return NativeType.BSTR;
            if(Boolean.TYPE==t)
                return NativeType.VariantBool;
            if(Object.class==t)
                return NativeType.VARIANT_ByRef;
        }

        if( t instanceof ParameterizedType ) {
            ParameterizedType p = (ParameterizedType) t;
            if( p.getRawType()==Holder.class ) {
                // let p=Holder<V>
                Type v = p.getActualTypeArguments()[0];
                if( v instanceof Class && Com4jObject.class.isAssignableFrom((Class<?>)v))
                    return NativeType.ComObject_ByRef;
                if(String.class==v)
                    return NativeType.BSTR_ByRef;
                if(Integer.class==v)
                    return NativeType.Int32_ByRef;
            }
        }

        throw new IllegalAnnotationException("no default conversion available for "+t);
    }

}
