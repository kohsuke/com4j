package com4j;

import java.util.Calendar;

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
    /**
     * Marshalled as 32-bit integer.
     *
     * <p>
     * Java "int" is 32 bit.
     *
     * <p>
     * Expected Java type:
     *      int
     *      {@link Number}
     *      {@link Enum} (see {@link ComEnum})
     */
    Int32(102) {
        // the native code will see the raw pointer value as Integer
        Object massage(Object param) {
            Class<?> clazz = param.getClass();

            if( Enum.class.isAssignableFrom(clazz) ) {
                // if it's an enum constant, change it to the number
                return EnumDictionary.get((Class<? extends Enum>)clazz).value((Enum)param);
            }
            return param;
        }

        Object unmassage(Class<?> type,Object param) {
            if( Enum.class.isAssignableFrom(type) ) {
                return EnumDictionary.get((Class<? extends Enum>)type).constant((Integer)param);
            }

            return param;
        }
    },
    Int32_ByRef(102|BYREF),

    /**
     * The native type is 'BOOL' (defined as 'int')
     * where <tt>true</tt> maps to -1 and <tt>false</tt> maps to 0.
     *
     * <p>
     * Expected Java type:
     *      boolean
     *      {@link Boolean}
     */
    Bool(103),

    /**
     * The native type is 'VARIANT_BOOL' where TRUE=1 and FALSE=0.
     * Note that <tt>sizeof(VARIANT_BOOL)==2</tt>.
     *
     * <p>
     * Expected Java type:
     *      boolean
     *      {@link Boolean}
     */
    VariantBool(104),

    /**
     * <tt>float</tt>.
     *
     * <p>
     * Expected Java type:
     *      boolean
     *      {@link Number}
     */
    Float(120),

    /**
     * <tt>double</tt>.
     *
     * <p>
     * Expected Java type:
     *      boolean
     *      {@link Number}
     */
    Double(121),

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
            return g.v;
        }
        Object unmassage(Class<?> signature, Object param) {
            if(param==null)     return null;
            return new GUID( (long[])param );
        }
    },

    /**
     * <tt>VARIANT*</tt>.
     *
     * <p>
     * Expected Java type:
     *      {@link Variant}
     *      {@link Object} // TODO: explain the semantics better
     */
    VARIANT_ByRef(302|BYREF),

    /**
     * <tt>IDispatch*</tt>
     *
     * <p>
     * Expected Java type:
     *      {@link Com4jObject}
     */
    Dispatch(303) {
        // the native code will see the raw pointer value as Integer
        Object massage(Object param) {
            int ptr = COM4J.unwrap((Com4jObject)param).getPtr();
            int disp = COM4J.queryInterface( ptr, COM4J.IID_IDispatch );
            return disp;
        }

        Object unmassage(Class<?> type,Object param) {
            if(param==null)     return null;
            int disp = (Integer)param;
            if(disp==0)      return null;
            Native.release( disp );
            return param;
        }
    },

    /**
     * <tt>DATE</tt>.
     *
     * See http://msdn.microsoft.com/library/default.asp?url=/library/en-us/vccore/html/_core_The_DATE_Type.asp
     * <p>
     * Expected Java type:
     *      {@link java.util.Date}
     *      {@link Calendar}
     */
    Date(400) {
        // the native code will see the raw pointer value as Integer
        Object massage(Object param) {
            java.util.Date dt;
            if( param instanceof Calendar ) {
                dt = ((Calendar)param).getTime();
            } else {
                dt = (java.util.Date)param;
            }

            // the number of milliseconds since January 1, 1970, 00:00:00 GMT
            long t = dt.getTime();
            // the number of milliseconds since January 1, 1970, 00:00:00 Local Time
            t  -= dt.getTimezoneOffset()*60*1000;

            // the number of milliseconds since December 30, 1899, 00:00:00 Local Time
            t += 2209132800000L;

            // DATE is an offset from "30 December 1899"
            long MSPD = 24*60*60*1000;
            if(t<0) {
                // -0.3 -> -0.7
                long offset = -(t%MSPD);    // TODO: check
                t = t-MSPD+offset;
            }
            double d = ((double)t)/MSPD;
            return d;
        }
    },

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
