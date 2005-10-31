package com4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Array;
import java.lang.reflect.TypeVariable;
import java.nio.Buffer;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

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
        for( int i=0; i<args.length; i++ ) {
            if(args[i] instanceof Holder && params[i].getNoByRef()!=null) {
                // massage the value of Holder, not the Holder itself
                Holder h = (Holder)args[i];
                h.value = params[i].getNoByRef().massage(h.value);
            } else {
                args[i] = params[i].massage(args[i]);
            }
        }

        try {
            Object r = Native.invoke( ptr, vtIndex, args, paramConvs,
                method.getReturnType(), returnIndex, returnIsInOut, returnConv.code );
            return returnConv.unmassage(method.getReturnType(), method.getGenericReturnType(), r);
        } finally {
            for( int i=0; i<args.length; i++ ) {
                if(args[i] instanceof Holder && params[i].getNoByRef()!=null) {
                    Holder h = (Holder)args[i];
                    Type holderParamType = getTypeParameter(genericParamTypes[i], 0);
                    h.value = params[i].getNoByRef().unmassage( erasure(holderParamType), holderParamType, h.value );
                } else {
                    args[i] = params[i].unmassage(paramTypes[i], genericParamTypes[i], args[i]);
                }
            }
        }
    }


    private static final Map<Class,NativeType> defaultConversions = new HashMap<Class, NativeType>();

    static {
        defaultConversions.put( Iterator.class, NativeType.ComObject );
        defaultConversions.put( GUID.class, NativeType.GUID );
        defaultConversions.put( double.class, NativeType.Double );
        defaultConversions.put( float.class, NativeType.Float );
        defaultConversions.put( int.class, NativeType.Int32 );
        defaultConversions.put( short.class, NativeType.Int16 );
        defaultConversions.put( byte.class, NativeType.Int8 );
        defaultConversions.put( boolean.class, NativeType.VariantBool );
        defaultConversions.put( String.class, NativeType.BSTR );
        defaultConversions.put( Object.class, NativeType.VARIANT_ByRef );
        defaultConversions.put( Variant.class, NativeType.VARIANT_ByRef );
        defaultConversions.put( Date.class, NativeType.Date );
    }

    /**
     * Computes the default conversion for the given type.
     */
    private static NativeType getDefaultConversion(Type t) {
        if( t instanceof Class ) {
            Class<?> c = (Class<?>)t;
            NativeType r = defaultConversions.get(c);
            if(r!=null) return r;

            if(Com4jObject.class.isAssignableFrom(c))
                return NativeType.ComObject;
            if(Enum.class.isAssignableFrom(c))
                return NativeType.Int32;
            if(Buffer.class.isAssignableFrom(c))
                return NativeType.PVOID;
            if(Calendar.class.isAssignableFrom(c))
                return NativeType.Date;
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
                if(Integer.class==v || Enum.class.isAssignableFrom((Class<?>)v))
                    return NativeType.Int32_ByRef;
                if(Boolean.class==v)
                    return NativeType.VariantBool_ByRef;
            }
            if( p.getRawType()==Iterator.class ) {
                return NativeType.ComObject;
            }
        }

        throw new IllegalAnnotationException("no default conversion available for "+t);
    }

    private static Type getTypeParameter( Type t, int index ) {
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            return pt.getActualTypeArguments()[index];
        } else {
            return Object.class;
        }
    }

    private static Class erasure( Type t ) {
        if (t instanceof Class) {
            return (Class) t;
        }
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            return erasure(pt.getRawType());
        }
        if (t instanceof WildcardType) {
            WildcardType wt = (WildcardType) t;
            Type[] ub = wt.getUpperBounds();
            if(ub.length==0)    return Object.class;
            else                return erasure(ub[0]);
        }
        if (t instanceof GenericArrayType) {
            GenericArrayType ga = (GenericArrayType) t;
            return Array.newInstance(erasure(ga.getGenericComponentType()),0).getClass();   // ARGH!
        }
        if (t instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) t;
            Type[] ub = tv.getBounds();
            if(ub.length==0)    return Object.class;
            else                return erasure(ub[0]);
        }
        throw new IllegalArgumentException(t.toString());
    }
}
