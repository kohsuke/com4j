package com4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Wraps COM VARIANT data structure.
 *
 * This class allows you to deal with the raw VARIANT type in case you need it,
 * but in general you should bind <tt>VARIANT*</tt> to {@link Object} or
 * {@link Holder}&lt;Object> for more natural Java binding.
 *
 * <p>
 * TODO: more documentation.
 *
 * <h2>Notes</h2>
 * <ol>
 * <li>
 * Calling methods defined on {@link Number} changes the variant
 * type (i.e., similar to a cast in Java) accordingly and returns its value.
 * </ol>
 *
 * <p>
 * Method names that end with '0' are native methods.
 *
 * <p>
 * TODO: more accessors
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Variant extends Number {
    /**
     * The memory image of the VARIANT.
     */
    final ByteBuffer image;

    /**
     * VARIANT type.
     *
     * This enum only defines constants that are legal for VARIANTs.
     */
    public static enum Type implements ComEnum {
        VT_EMPTY(0),
        VT_NULL(1),
        VT_I2(2),
        VT_I4(3),
        VT_R4(4),
        VT_R8(5),
        VT_CY(6),
        VT_DATE(7),
        VT_BSTR(8),
        VT_DISPATCH(9),
        VT_ERROR(10),
        VT_BOOL(11),
        VT_VARIANT(12),
        VT_UNKNOWN(13),
        VT_DECIMAL(14),
        VT_RECORD(36),
        VT_I1(16),
        VT_UI1(17),
        VT_UI2(18),
        VT_UI4(19),
        VT_INT(22),
        VT_UINT(23),
        VT_ARRAY_I2(0x2000|2),
        VT_ARRAY_I4(0x2000|3),
        VT_ARRAY_R4(0x2000|4),
        VT_ARRAY_R8(0x2000|5),
        VT_ARRAY_CY(0x2000|6),
        VT_ARRAY_DATE(0x2000|7),
        VT_ARRAY_BSTR(0x2000|8),
        VT_ARRAY_BOOL(0x2000|11),
        VT_ARRAY_VARIANT(0x2000|12),
        VT_ARRAY_DECIMAL(0x2000|14),
        VT_ARRAY_I1(0x2000|16),
        VT_ARRAY_UI1(0x2000|17),
        VT_ARRAY_UI2(0x2000|18),
        VT_ARRAY_UI4(0x2000|19),
        VT_ARRAY_INT(0x2000|22),
        VT_ARRAY_UINT(0x2000|23),
//        VT_BYREF
        ;

        private final int value;

        private Type( int value ) {
            this.value = value;
        }

        public int comEnumValue() {
            return value;
        }
    }

    /**
     * Creates an empty {@link Variant}.
     */
    public Variant() {
        image = ByteBuffer.allocateDirect(16);
        image.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Creates an empty {@link Variant} with the given type.
     */
    public Variant(Type type) {
        this();
        setType(type);
    }

    /**
     * Empties the current contents.
     *
     * <p>
     * Sometimes a {@link Variant} holds things like interface pointers or
     * arrays, which require some clean up actions. Therefore, when you
     * want to reuse an existing {@link Variant} that may hold a value,
     * you should first clear it.
     */
    public void clear() {
        clear0(image);
    }

    /**
     * Makes sure the variant is cleared before GC-ed.
     */
    public void finalize() {
        clear();
    }

    /**
     * Calls <tt>VariantClear</tt> method.
     */
    private static native void clear0( ByteBuffer image );

    /**
     * Sets the type of the variant.
     */
    public void setType( Type t ) {
        image.putLong(0,t.comEnumValue());
    }

    /**
     * Gets the type of the variant.
     */
    public Type getType() {
        return EnumDictionary.get(Type.class).constant((int)image.getLong(0));
    }


    private static native void changeType0( int type, ByteBuffer image );

    /**
     * Changes the variant type to the specified one.
     */
    private void changeType( Type t ) {
        changeType0( t.comEnumValue(), image );
    }

    public int intValue() {
        changeType(Type.VT_I4);
        return image.getInt(8);
    }

    public void set(int i) {
        changeType(Type.VT_I4);
        image.putInt(8,i);
    }

    public long longValue() {
        // VARIANT doesn't seem to support 64bit int
        return intValue();
    }

    public float floatValue() {
        changeType(Type.VT_R4);
        return image.getFloat(8);
    }

    public void set(float f) {
        changeType(Type.VT_R4);
        image.putFloat(8,f);
    }

    public double doubleValue() {
        changeType(Type.VT_R8);
        return image.getDouble(8);
    }

    public void set(double d) {
        changeType(Type.VT_R8);
        image.putDouble(8,d);
    }

    public String stringValue() {
        return convertTo(String.class);
    }

    /**
     * Reads this VARIANT as a COM interface pointer.
     */
    public <T extends Com4jObject> T object( Class<T> type ) {
        changeType(Type.VT_UNKNOWN);
        int ptr = image.getInt(8);
        if(ptr==0)  return null;
        Native.addRef(ptr);
        return Wrapper.create(type,ptr);
    }

    /**
     * Converts the variant to the given object type.
     */
    public native <T> T convertTo( Class<T> type );

    // TODO: this isn't quite working
    public static final Variant MISSING = new Variant(Type.VT_ERROR);


    /**
     * Called from the native code to assist VT_DATE -> Date conversion.
     */
    static Date toDate(double d) {
        GregorianCalendar ret = new GregorianCalendar(1899,11,30);
        int days = (int)d;
        d -= days;
        ret.add(Calendar.DATE,days);
        d *= 24;
        int hours = (int)d;
        ret.add(Calendar.HOUR,hours);
        d -= hours;
        d *= 60;
        d += 0.5; // round
        int min = (int)d;
        ret.add(Calendar.MINUTE,min);
        return ret.getTime();
    }

    /**
     * Opposite of the {@link #toDate(double)} method.
     */
    static double fromDate(Date dt) {
        // TODO: implement this method later
        throw new UnsupportedOperationException();
    }
}
