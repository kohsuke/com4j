package com4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Iterator;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static com4j.Const.BYREF;

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
    BSTR(1) {
        public NativeType byRef() {
            return BSTR_ByRef;
        }
    },

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
     * <tt>LPWSTR</tt>.
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

    /**
     * <tt>INT8</tt> (byte).
     *
     * <p>
     * Expected Java type:
     *      byte
     *      {@link Number}
     */
    Int8(100) {
        public NativeType byRef() {
            return Int8_ByRef;
        }
    },
    Int8_ByRef(100|BYREF),
    /**
     * <tt>INT16</tt> (short).
     *
     * <p>
     * Expected Java type:
     *      short
     *      {@link Number}
     */
    Int16(101) {
        public NativeType byRef() {
            return Int16_ByRef;
        }
    },
    Int16_ByRef(101|BYREF),

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

        Object unmassage(Class<?> type, Type genericSignature, Object param) {
            if( Enum.class.isAssignableFrom(type) ) {
                return EnumDictionary.get((Class<? extends Enum>)type).constant((Integer)param);
            }

            return param;
        }

        public NativeType byRef() {
            return Int32_ByRef;
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

        Object unmassage(Class<?> type, Type genericSignature, Object param) {
            if(param==null)     return null;
            if(type==Iterator.class) {
                Class itemType = Object.class;
                if(genericSignature instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) genericSignature;
                    Type it = pt.getActualTypeArguments()[0];
                    if(it instanceof Class)
                        itemType = (Class)it;
                }
                return new ComCollection(itemType,Wrapper.create(IEnumVARIANT.class, (Integer)param ));
            }
            return Wrapper.create( (Class<? extends Com4jObject>)type, (Integer)param );
        }

        public NativeType byRef() {
            return ComObject_ByRef;
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
            Holder h = (Holder)param;
            h.value = ComObject.massage(h.value);
            return h;
        }
        Object unmassage(Class<?> type, Type genericSignature, Object param) {
            Holder h = (Holder)param;
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
        Object unmassage(Class<?> signature, Type genericSignature, Object param) {
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
     *      {@link Object}
     *
     * <p>
     * When the Java type is {@link Object}, the type of the created
     * <tt>VARIANT</tt> is determined at run-time from the actual type
     * of the Java object being passed in. The following table describes
     * this inferene process:
     *
     * <table border=1>
     * <tr>
     *  <td>Java type
     *  <td>COM VARIANT type
     * <tr>
     *  <td>{@link Boolean} / boolean
     *  <td>VT_BOOL
     * <tr>
     *  <td>{@link String}
     *  <td>VT_BSTR
     * <tr>
     *  <td>{@link Float} / float
     *  <td>VT_R4
     * <tr>
     *  <td>{@link Double} / double
     *  <td>VT_R8
     * <tr>
     *  <td>{@link Short} / short
     *  <td>VT_I2
     * <tr>
     *  <td>{@link Integer} / int
     *  <td>VT_I4
     * <tr>
     *  <td>{@link Long} / long
     *  <td>VT_I8
     * <tr>
     *  <td>{@link Com4jObject} or its derived types
     *  <td>VT_UNKNOWN
     * </table>
     * TODO: expand the list
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

        Object unmassage(Class<?> type, Type genericSignature, Object param) {
            if(param==null)     return null;
            int disp = (Integer)param;
            if(disp==0)      return null;
            Native.release( disp );
            return param;
        }
    },

    /**
     * <tt>PVOID</tt>.
     *
     * <p>
     * The assumed semantics is that a region of buffer
     * will be passed to the native method.
     *
     * <p>
     * Expected Java type:
     *      direct {@link Buffer}s ({@link Buffer}s created from methods like
     *      {@link ByteBuffer#allocateDirect(int)}
     */
    PVOID(304) {
        public NativeType byRef() {
            return PVOID_ByRef;
        }
    },


    /**
     * <tt>void**</tt>.
     *
     * <p>
     * The assumed semantics is that you receive a buffer.
     *
     * <p>
     * Expected Java type:
     *      {@link Holder}&lt;{@link Buffer}> ({@link Buffer}s created from methods like
     *      {@link ByteBuffer#allocateDirect(int)}
     */
    PVOID_ByRef(304|BYREF),

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
            t += 2209161600000L;

            // DATE is an offset from "30 December 1899"
            if(t<0) {
                // -0.3 -> -0.7
                long offset = -(t%MSPD);    // TODO: check
                t = t-MSPD+offset;
            }
            double d = ((double)t)/MSPD;
            return d;
        }

        Object unmassage(Class<?> signature, Type genericSignature, Object param) {
            double d = (Double)param;
            long t = (long)(d*MSPD);
            t -= 2209161600000L;
            t -= defaultTimeZone.getOffset(t);  // convert back to UTC
            java.util.Date dt = new java.util.Date(t);
            if(Calendar.class.isAssignableFrom(signature)) {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(dt);
                return cal;
            } else {
                return dt;
            }
        }
    },


    /**
     * <tt>SAFEARRAY</tt>.
     *
     * <p>
     * The given java type is converted into a SAFEARRAY before
     * passed to the native method.
     *
     * When the Java type is an array, the component type of the SAFEARRAY
     * is automatically derived from the component type of the Java array.
     * This inference is defined as follows:
     * <ul>
     *  <li>boolean[] -> SAFEARRAY(VT_BOOL)
     *  <li>byte[] -> SAFEARRAY(VT_UI1)
     *  <li>char[] -> SAFEARRAY(VT_UI2)  (??? is this right?)
     *  <li>short[] -> SAFEARRAY(VT_I2)
     *  <li>int[] -> SAFEARRAY(VT_I4)
     *  <li>long[] -> SAFEARRAY(VT_I8)
     *  <li>float[] -> SAFEARRAY(VT_R4)
     *  <li>double[] -> SAFEARRAY(VT_R8)
     *
     *  <li>Object[] -> SAFEARRAY(VT_VARIANT)
     *  <li>String[] -> SAFEARRAY(VT_BSTR)
     * </ul>
     */
    SafeArray(500),

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
     * @param signature
     *      the parameter type in its raw form.
     * @param genericSignature
     *      the parameter type in its generified form.
     * @param param
     */
    Object unmassage(Class<?> signature, Type genericSignature, Object param) {
        return param;
    }

    /**
     * If the constant has the BYREF version, return it.
     * Otherwise null.
     */
    public NativeType byRef() {
        return null;
    }


    private static final long MSPD = 24*60*60*1000;
    private static final TimeZone defaultTimeZone = TimeZone.getDefault();

}
