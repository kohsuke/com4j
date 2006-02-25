package com4j;

import java.lang.reflect.Method;

/**
 * {@link ComMethod} that invokes through {@code IDispatch.Invoke}.
 *
 * @author Kohsuke Kawaguchi
 */
final class DispatchComMethod extends ComMethod {
    final Method method;

    final int dispId;
    final int flag;
    final Class<?> retType;

    DispatchComMethod( Method m ) {
        method = m;

        VTID vtid = m.getAnnotation(VTID.class);
        if(vtid!=null)
            throw new IllegalAnnotationException("@VTID and @DISPID are mutually exclusive: "+m.toGenericString());

        DISPID id = m.getAnnotation(DISPID.class);
        if(id ==null)
            throw new IllegalAnnotationException("@DISPID is missing: "+m.toGenericString());
        dispId = id.value();

        flag = getFlag();

        retType = m.getReturnType();
    }

    private int getFlag() {
        PropGet get = method.getAnnotation(PropGet.class);
        PropPut put = method.getAnnotation(PropPut.class);
        if(get!=null && put!=null)
            throw new IllegalAnnotationException("@PropPut and @PropGet are mutually exclusive: "+method.toGenericString());
        if(get!=null)
            return DISPATCH_PROPERTYGET;
        if(put!=null)
            return DISPATCH_PROPERTYPUT;

        return DISPATCH_METHOD;
    }

    Object invoke(int ptr, Object[] args) {
        Variant v = Native.invokeDispatch(ptr,dispId,flag,args);
        if(v==null)
            return null;

        if(retType==void.class)
            return null;

        return v.convertTo(retType);
    }

    private static final int DISPATCH_METHOD         = 0x1;
    private static final int DISPATCH_PROPERTYGET    = 0x2;
    private static final int DISPATCH_PROPERTYPUT    = 0x4;
    private static final int DISPATCH_PROPERTYPUTREF = 0x8;
}
