package com4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ComMethod} that invokes through {@code IDispatch.Invoke}.
 *
 * @author Kohsuke Kawaguchi
 */
final class DispatchComMethod extends ComMethod {

    final int dispId;
    final int flag;
    final Class<?> retType;

    DispatchComMethod( Method m ) {
        super(m);

        DISPID id = m.getAnnotation(DISPID.class);
        if(id ==null)
            throw new IllegalAnnotationException("@DISPID is missing: "+m.toGenericString());
        dispId = id.value();

        flag = getFlag();

        Class retType = m.getReturnType();
        if (retType.isPrimitive() && boxTypeMap.containsKey(retType))
            retType = boxTypeMap.get(retType);
        this.retType = retType;
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

    Object invoke(long ptr, Object[] args) {
        messageParameters(args);

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
    @SuppressWarnings("unused")
    private static final int DISPATCH_PROPERTYPUTREF = 0x8;

    private static final Map<Class,Class> boxTypeMap = new HashMap<Class,Class>();

    static {
        boxTypeMap.put(byte.class,Byte.class);
        boxTypeMap.put(short.class,Short.class);
        boxTypeMap.put(int.class,Integer.class);
        boxTypeMap.put(long.class,Long.class);
        boxTypeMap.put(float.class,Float.class);
        boxTypeMap.put(double.class,Double.class);
        boxTypeMap.put(boolean.class,Boolean.class);
        boxTypeMap.put(char.class,Character.class);
    }
}
