package com4j;

import static com4j.Const.*;

/**
 * Native method type.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public enum NativeType {
    /**
     * <tt>BSTR</tt>.
     *
     * TODO: support CharSequence
     * <p>
     * Expected Java type:
     *      String
     */
    BSTR(1),

    /**
     * <tt>BSTR*</tt>.
     *
     * TODO: support StringBuffer
     * <p>
     * Expected Java type:
     *      {@link Holder<String>}
     */
    BSTR_ByRef(1|BYREF),


    /**
     * String will be marshalled as "wchar_t*".
     *
     * More concretely, it becomes a L'\0'-terminated
     * UTF-16LE format.
     */
    Unicode(2),
    /**
     * String will be marshalled as "char*".
     *
     * <p>
     * More concretely, it becomes a '\0'-terminated
     * multi-byte string where characters are encoded
     * according to the platform default encoding.
     *
     * <p>
     * For example, on a typical English Windows system
     * this is just an ASCII string (more or less). On
     * a typical Japanese Windows system, this is
     * Shift-JIS.
     */
    CSTR(3),

    Int8(100),
    Int16(101),
    Int32(102),
    Int32_ByRef(102|BYREF),

    /**
     * The native type is 'BOOL' (defined as 'int')
     * where <tt>true</tt> maps to -1 and <tt>false</tt> maps to 0.
     */
    Bool(103),

    /**
     * Used only with {@link ReturnValue} for returning
     * HRESULT of the method invocation as "int".
     */
    HRESULT(200),

    /**
     * The native type is determined from the Java method return type.
     * See the documentation for mor details.
     * TODO: link to the doc.
     */
    Default(201),

    /**
     * COM interface pointer.
     *
     * <p>
     * Expected Java type:
     *      {@link Com4jObject}
     */
    ComObject(300) {
        // the native code will see the raw pointer value as Integer
        Object massage(Object param) {
            return COM4J.unwrap((Com4jObject)param).getPtr();
        }

        Object unmassage(Class<?> type,Object param) {
            if(param==null)     return null;
            return COM4J.wrap( (Class<? extends Com4jObject>)type, (Integer)param );
        }
    },

    /**
     * COM interface pointer by reference.
     *
     * <p>
     * Expected Java type:
     *      {@link Holder<ComObject>}
     */
    ComObject_ByRef(300|BYREF) {
        // the native code will see the raw pointer value as Integer
        Object massage(Object param) {
            Holder<Object> h = (Holder<Object>)param;
            h.value = ComObject.massage(h.value);
            return h;
        }
        Object unmassage(Class<?> type,Object param) {
            Holder<Object> h = (Holder<Object>)param;
            h.value = ComObject.massage(h.value);
            return h;
        }
    },

    /**
     * GUID.
     *
     * <p>
     * Passed by reference in COM convention but it doesn't have
     * the "out" semantics.
     *
     * <p>
     * Expected Java type:
     *      {@link GUID}
     */
    GUID(301) {
        // pass in the value as two longs
        Object massage(Object param) {
            GUID g = (GUID)param;
            return new long[]{g.l1, g.l2};
        }
        Object unmassage(Class<?> type,Object param) {
            Holder<Object> h = (Holder<Object>)param;
            h.value = ComObject.massage(h.value);
            return h;
        }
    }


    ;





    /**
     * Unique identifier of this constant.
     * Passed to the native code.
     */
    final int code;

    NativeType( int code ) {
        this.code = code;
    }

    /**
     * Changes the parameter type before the parameter is passed to the native code.
     * <p>
     * This allows {@link NativeType}s to take more Java-friendly argument and
     * convert it to more native code friendly form behind the scene.
     *
     * @param param
     *      can be null.
     */
    Object massage(Object param) {
        return param;
    }
    /**
     * Changes the parameter type before the method call returns.
     * <p>
     * The opposite of {@link #massage(java.lang.Object)}. Only useful for
     * BYREFs.
     *
     * @param param
     *      can be null.
     */
    Object unmassage(Class<?> signature, Object param) {
        return param;
    }
}
