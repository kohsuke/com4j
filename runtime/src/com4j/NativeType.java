package com4j;

import com4j.stdole.IEnumVARIANT;

import static com4j.Const.BYREF;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

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
    BSTR(1,4),

    /**
     * <tt>BSTR*</tt>.
     *
     * TODO: support StringBuffer
     * <p>
     * Expected Java type:
     *      {@link Holder}&lt;String&gt;
     */
    BSTR_ByRef(1|BYREF,4),


    /**
     * <tt>LPWSTR</tt>.
     *
     * More concretely, it becomes a L'\0'-terminated
     * UTF-16LE format.
     */
    Unicode(2,4),
    /**
     * String will be marshaled as "char*".
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
    CSTR(3,4),

    /**
     * <tt>INT8</tt> (byte).
     *
     * <p>
     * Expected Java type:
     *      byte
     *      {@link Number}
     */
    Int8(100,1),
    
    /**
     * {@link #Int8} passed by reference
     * TODO should we add enum message/unmessage?
     */
    Int8_ByRef(100|BYREF,1), //FIXME: BYREF -> pointer size is 4 bytes
    
    /**
     * <tt>INT16</tt> (short).
     *
     * <p>
     * Expected Java type:
     *      short
     *      {@link Number}
     */
    Int16(101,2){
        @Override
        Object toNative(Object param) {
            if(param instanceof Enum){
                Enum e = (Enum) param;
                return (short) EnumDictionary.get(e.getClass()).value(e);
            }
            return param;
        }
        @Override
        Object toJava(Class<?> signature, Type genericSignature, Object param) {
            if(param instanceof Enum){
                return EnumDictionary.get((Class<? extends Enum>)signature).constant((Short)param);
            }
            return param;
        }
    },
    
    /**
     * {@link #Int16} passed by reference
     */
    Int16_ByRef(101|BYREF,2){
        @Override
        Object toNative(Object param) {
            if(param instanceof Enum){
                Enum e = (Enum) param;
                return (short) EnumDictionary.get(e.getClass()).value(e);
            }
            return param;
        }
        @Override
        Object toJava(Class<?> signature, Type genericSignature, Object param) {
            if(param instanceof Enum){
                return EnumDictionary.get((Class<? extends Enum>)signature).constant((Short)param);
            }
            return param;
        }
    },

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
    Int32(102,4) {
        // the native code will see the raw pointer value as Integer
        Object toNative(Object param) {
            if(param instanceof Enum){
                Enum e = (Enum) param;
                return EnumDictionary.get(e.getClass()).value(e);
            }
            return param;
        }

        Object toJava(Class<?> type, Type genericSignature, Object param) {
            if( Enum.class.isAssignableFrom(type) ) {
                return EnumDictionary.get((Class<? extends Enum>)type).constant((Integer)param);
            }
            return param;
        }
    },
    Int32_ByRef(102|BYREF,4) {
        Object toNative(Object param) {
            if(param instanceof Enum){
                Enum e = (Enum) param;
                return EnumDictionary.get(e.getClass()).value(e);
            }
            return param;
        }

        Object toJava(Class<?> type, Type genericSignature, Object param) {
            if( Enum.class.isAssignableFrom(type) ) {
                return EnumDictionary.get((Class<? extends Enum>)type).constant((Integer)param);
            }
            return param;
        }
    },

    /**
     * The native type is 'BOOL' (defined as 'int')
     * where <tt>true</tt> maps to -1 and <tt>false</tt> maps to 0.
     *
     * <p>
     * Expected Java type:
     *      boolean
     *      {@link Boolean}
     */
    Bool(103,4),

    /**
     * The native type is 'VARIANT_BOOL' where TRUE=1 and FALSE=0.
     * Note that <tt>sizeof(VARIANT_BOOL)==2</tt>.
     *
     * <p>
     * Expected Java type:
     *      boolean
     *      {@link Boolean}
     */
    VariantBool(104,2),
    VariantBool_ByRef(104|BYREF,4),

    /**
     * Marshalled as 64-bit integer.
     *
     * <p>
     * Java "long" is 64 bit.
     *
     * <p>
     * Expected Java type:
     *      long
     *      {@link Number}
     */
    Int64(105,8), // should we add enum message/unmessage?
    Int64_ByRef(105|BYREF,8),

    /**
     * <tt>float</tt>.
     *
     * <p>
     * Expected Java type:
     *      boolean
     *      {@link Number}
     */
    Float(120,4),
    Float_ByRef(120|BYREF,4),

    /**
     * <tt>double</tt>.
     *
     * <p>
     * Expected Java type:
     *      boolean
     *      {@link Number}
     */
    Double(121,8),
    Double_ByRef(121|BYREF,4),

    /**
     * Used only with {@link ReturnValue} for returning
     * HRESULT of the method invocation as "int".
     */
    HRESULT(200,4),

    /**
     * The native type is determined from the Java method return type.
     * See the documentation for more details.
     * TODO: link to the doc.
     */
    Default(201,9999),

    /**
     * COM interface pointer.
     *
     * <p>
     * Expected Java type:
     *      {@link Com4jObject}
     */
    ComObject(300,4) {
        // the native code will see the raw pointer value as Long
        Object toNative(Object param) {
            if(param==null)
                return 0L;
            return ((Com4jObject)param).getPointer();
        }

        Object toJava(Class<?> type, Type genericSignature, Object param) {
            if(param==null)     return null;
            if(type==Iterator.class) {
                Class<?> itemType = Object.class;
                if(genericSignature instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) genericSignature;
                    Type it = pt.getActualTypeArguments()[0];
                    if(it instanceof Class)
                        itemType = (Class<?>)it;
                }
                Com4jObject base = Wrapper.create((Long) param);
                IEnumVARIANT enumVar = base.queryInterface(IEnumVARIANT.class);
                base.dispose();
                return new ComCollection(itemType,enumVar);
            }
            // interface pointers we get from out parameters are owned by the caller,
            // so there's no need to do addRef
            return Wrapper.create( (Class<? extends Com4jObject>)type, (Long)param );
        }
    },

    /**
     * COM interface pointer by reference.
     *
     * <p>
     * Expected Java type:
     *      {@code Holder<ComObject>}
     */
    ComObject_ByRef(300|BYREF,4) {
        // the native code will see the raw pointer value as Integer
        Object toNative(Object param) {
            Holder h = (Holder)param;
            h.value = ComObject.toNative(h.value);
            return h;
        }
        Object toJava(Class<?> type, Type genericSignature, Object param) {
            Holder h = (Holder)param;
            h.value = ComObject.toNative(h.value);
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
    GUID(301,4) {
        // pass in the value as two longs
        Object toNative(Object param) {
            GUID g = (GUID)param;
            return g.v;
        }
        Object toJava(Class<?> signature, Type genericSignature, Object param) {
            if(param==null)     return null;
            return new GUID( (long[])param );
        }
    },

    /**
     * <tt>VARIANT</tt>.
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
    VARIANT(302,16) {
        @Override
        Object toNative(Object param) {
            if(param instanceof Enum){
                Enum e = (Enum) param;
                return EnumDictionary.get(e.getClass()).value(e);
            }
            return param;
        }

        @Override
        Object toJava(Class<?> signature, Type genericSignature, Object param) {
            if(param instanceof Variant) {
                Variant v = (Variant)param;
                Object r = v.convertTo(signature);
                v.clear();
                return r;
            } else if(param instanceof Enum){
                return EnumDictionary.get((Class<? extends Enum>)signature).constant((Integer)param);
            }
            return param;
        }
    },

    /**
     * <tt>VARIANT*</tt>.
     *
     * <p>
     * This works like {@link #VARIANT}, except that a reference
     * is passed, instead of a VARIANT itself.
     */
    VARIANT_ByRef(302|BYREF,4) {
        @Override
        Object toNative(Object param) {
            if(param instanceof Enum){
                Enum e = (Enum) param;
                return EnumDictionary.get(e.getClass()).value(e);
            }
            return param;
        }

        @Override
        Object toJava(Class<?> signature, Type genericSignature, Object param) {
            if(param instanceof Enum){
                return EnumDictionary.get((Class<? extends Enum>)signature).constant((Integer)param);
            }
            return param;
        }
    },

    /**
     * <tt>IDispatch*</tt>
     *
     * <p>
     * Expected Java type:
     *      {@link Com4jObject}
     */
    Dispatch(303,4) {
        // the native code will see the raw pointer value as Long
        Object toNative(Object param) {
            if(param==null) return 0L;
            long ptr = ((Com4jObject)param).getPointer();
            long disp = COM4J.queryInterface( ptr, COM4J.IID_IDispatch );
            return disp;
        }

        Object toJava(Class<?> type, Type genericSignature, Object param) {
            if(param==null)     return null;
            long disp = (Long)param;
            if(disp==0)      return null;

            Class<? extends Com4jObject> itf = (Class<? extends Com4jObject>) type;
            Com4jObject r = Wrapper.create(itf, Native.queryInterface(disp, COM4J.getIID(itf)) );

            Native.release( disp );
            return r;
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
     *      direct {@link java.nio.Buffer}s ({@link java.nio.Buffer}s created from methods like
     *      {@link java.nio.ByteBuffer#allocateDirect(int)}
     */
    PVOID(304,4),


    /**
     * <tt>void**</tt>.
     *
     * <p>
     * The assumed semantics is that you receive a buffer.
     *
     * <p>
     * Expected Java type:
     *      {@link Holder}&lt;{@link java.nio.Buffer}&gt; ({@link java.nio.Buffer}s created from methods like
     *      {@link java.nio.ByteBuffer#allocateDirect(int)}
     */
    PVOID_ByRef(304|BYREF,4),

    /**
     * <tt>DATE</tt>.
     *
     * See http://msdn.microsoft.com/library/default.asp?url=/library/en-us/vccore/html/_core_The_DATE_Type.asp
     * <p>
     * Expected Java type:
     *      {@link java.util.Date}
     *      {@link Calendar}
     */
    Date(400,8) {
        // the native code will see the raw pointer value as Integer
        Object toNative(Object param) {
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

        Object toJava(Class<?> signature, Type genericSignature, Object param) {
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
     * <tt>CURRENCY</tt>.
     *
     * According to MSDN:
     * <blockquote>
     * CURRENCY is implemented as an 8-byte two's-complement integer value scaled
     * by 10,000. This gives a fixed-point number with 15 digits to the left of
     * the decimal point and 4 digits to the right. The CURRENCY data type is
     * extremely useful for calculations involving money, or for any fixed-point
     * calculations where accuracy is important.
     * </blockquote>
     *
     * <p>
     * Expected Java type:
     *      {@link java.math.BigDecimal}
     */
    Currency(401,8),
    Currency_ByRef(401|BYREF,8), // FIXME? ByRef should always have a size of four bytes, I think..


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
    SafeArray(500,24),

    // TODO: Not supported yet: SafeArray_ByRef(500|BYREF, 4)

    ;





    /**
     * Unique identifier of this constant.
     * Passed to the native code.
     */
    final int code;

    /**
     * Size of the native type in bytes.
     */
    final int size;

    private static final Map<Integer,NativeType> codeMap = new HashMap<Integer, NativeType>();

    static {
        for (NativeType nt : values()) {
            codeMap.put(nt.code,nt);
        }
    }

    NativeType( int code, int size ) {
        this.code = code;
        this.size = size;
    }

    /**
     * Changes the parameter type before the parameter is passed to the native code.
     * <p>
     * This allows {@link NativeType}s to take more Java-friendly argument and
     * convert it to more native code friendly form behind the scene.
     *
     * @param param can be null.
     */
    Object toNative(Object param) {
        return param;
    }
    /**
     * Changes the parameter type before the method call returns.
     * <p>
     * The opposite of {@link #toNative(Object)}. Only useful for
     * BYREFs.
     *
     * @param signature         the parameter type in its raw form.
     * @param genericSignature  the parameter type in its generified form.
     * @param param
     */
    Object toJava(Class<?> signature, Type genericSignature, Object param) {
        return param;
    }

    /**
     * If the constant has the BYREF version, return it.
     * Otherwise null.
     */
    public final NativeType byRef() {
        if(code==(code|BYREF))
            return null;
        return codeMap.get(code|BYREF);
    }

    public final NativeType getNoByRef() {
        if(code==(code&(~BYREF)))
            return null;
        return codeMap.get(code&(~BYREF));
    }


    private static final long MSPD = 24*60*60*1000;
    private static final TimeZone defaultTimeZone = TimeZone.getDefault();

}
