package com4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.Buffer;
import java.util.Iterator;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
final class MethodInfo {

    final Method method;

    final int vtIndex;
    // list of params.code
    final int[] paramConvs;
    final NativeType[] params;
    final int returnIndex;
    final boolean returnIsInOut;
    final NativeType returnConv;
    final Class<?>[] paramTypes;
    final Type[] genericParamTypes;

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
        genericParamTypes = m.getGenericParameterTypes();
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
            return returnConv.unmassage(method.getReturnType(), method.getGenericReturnType(), r);
        } finally {
            for( int i=0; i<args.length; i++ )
                args[i] = params[i].unmassage(paramTypes[i], genericParamTypes[i], args[i]);
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
            if(Iterator.class==t)
                return NativeType.ComObject;
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
            if(Object.class==t || Variant.class==t)
                return NativeType.VARIANT_ByRef;
            if(Buffer.class.isAssignableFrom(c))
                return NativeType.PVOID;
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
            if( p.getRawType()==Iterator.class ) {
                return NativeType.ComObject;
            }
        }

        throw new IllegalAnnotationException("no default conversion available for "+t);
    }

}
