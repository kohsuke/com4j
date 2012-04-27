package com4j;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * {@link ComMethod} that represents a single method invocation
 * through JNI.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
final class StandardComMethod extends ComMethod {

    final int vtIndex;

    StandardComMethod( Method m ) {
        super(m);

        VTID vtid = m.getAnnotation(VTID.class);
        if(vtid==null)
            throw new IllegalAnnotationException("@VTID is missing: "+m.toGenericString());
        vtIndex = vtid.value();
    }

    Object invoke( long ptr, Object[] args ) {
        messageParameters(args);

        try {
            Object r = Native.invoke( ptr, vtIndex, args, paramConvs,
                returnIndex, returnIsInOut, returnConv.code );
            return returnConv.toJava(method.getReturnType(), method.getGenericReturnType(), r);
        } catch( ComException e ) {
            try {
                IErrorInfo pErrorInfo = Native.getErrorInfo(ptr, (Class)method.getDeclaringClass());
                if(pErrorInfo!=null) {
                    e.setErrorInfo(new ErrorInfo(pErrorInfo));
                    pErrorInfo.dispose();   // don't keep it for too long
                }
            } catch (ComException x) {
                // some user reported that some program fails to report error info.
                // originally error information is normally more useful, so just report that.
            }
            throw e;
        } finally {
            for( int i=0; i<args.length; i++ ) {
                if(args[i] instanceof Holder && params[i].getNoByRef()!=null) {
                    Holder h = (Holder)args[i];
                    Type holderParamType = getTypeParameter(genericParamTypes[i], 0);
                    h.value = params[i].getNoByRef().toJava(erasure(holderParamType), holderParamType, h.value);
                }
            }
        }
    }


    private static Type getTypeParameter( Type t, int index ) {
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            return pt.getActualTypeArguments()[index];
        } else {
            return Object.class;
        }
    }

    private static Class<?> erasure( Type t ) {
        if (t instanceof Class) {
            return (Class<?>) t;
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
            TypeVariable<?> tv = (TypeVariable<?>) t;
            Type[] ub = tv.getBounds();
            if(ub.length==0)    return Object.class;
            else                return erasure(ub[0]);
        }
        throw new IllegalArgumentException(t.toString());
    }
}
