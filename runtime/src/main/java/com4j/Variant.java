package com4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Wraps COM VARIANT data structure.
 *
 *<p>
 * This class allows you to deal with the raw VARIANT type in case you need it,
 * but in general you should bind <tt>VARIANT*</tt> to {@link Object} or
 * {@link Holder}&lt;Object&gt; for more natural Java binding.
 *</p>
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
 * </p>
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (ScM, (C) 2008, 2009, Michael-Schnell@gmx.de)
 */
@SuppressWarnings("serial")
public final class Variant extends Number {
    /**
     * The memory image of the VARIANT.
     */
    final ByteBuffer image;

    /**
     * The ComThread that generated this Variant.
     * @see ComCollection#fetch
     */
    ComThread thread = null;

    /**
     * Bit mask of a Variant array.
     */
    private static final int ARRAY = 0x2000;

    /**
     * The COM error code of the variant MISSING
     */
    private static final int MISSING_ERROR_CODE = 0x80020004;

    /**
     * VARIANT type.
     *
     * This enum only defines constants that are legal for VARIANTs.
     */
    public static enum Type implements ComEnum {
      /**
       * This is not a variant type. It indicates, that the type is unknown. <br>
       * Its name is <code>NO_TYPE</code> instead of something like <code>UNKNOWN</code> to make it
       * more distinguishable from {@link Type#VT_UNKNOWN}, which indicates that the Variant points to an
       * IUnknown interface.
       */
        NO_TYPE(0),
        /**
         * MSDN:
         * <p>
         * No value was specified. If an optional argument to an Automation method is left blank, do not pass a VARIANT of type VT_EMPTY.
         * Instead, pass a VARIANT of type VT_ERROR with a value of DISP_E_PARAMNOTFOUND.
         * </p>
         */
        VT_EMPTY(0),
        /**
         * MSDN:
         * <p>
         * A propagating null value was specified. (This should not be confused with the null pointer.) The null value is used for
         * tri-state logic, as with SQL.
         * </p>
         */
        VT_NULL(1),
        /**
         * MSDN: A 2-byte integer value.
         */
        VT_I2(2),
        /**
         * MSDN: A 4-byte integer value.
         */
        VT_I4(3),
        /**
         * MSDN: An IEEE 4-byte real value.
         */
        VT_R4(4),
        /**
         * MSDN: An 8-byte IEEE real value.
         */
        VT_R8(5),
        /**
         * MSDN:
         * <p>
         * A currency value was specified. A currency number is stored as 64-bit (8-byte), two's complement integer, scaled by 10,000
         * to give a fixed-point number with 15 digits to the left of the decimal point and 4 digits to the right.
         * </p>
         */
        VT_CY(6),
        /**
         * MSDN:
         * <p>
         *  A value denoting a date and time was specified. Dates are represented as double-precision numbers, where midnight, January 1,
         *  1900 is 2.0, January 2, 1900 is 3.0, and so on. The value is passed in date.
         * </p>
         * <p>
         *  This is the same numbering system used by most spreadsheet programs, although some specify incorrectly that February 29, 1900
         *  existed, and thus set January 1, 1900 to 1.0. The date can be converted to and from an MS-DOS representation using
         *  VariantTimeToDosDateTime, which is discussed in Conversion and Manipulation Functions.
         * </p>
         */
        VT_DATE(7),
        /**
         * MSDN:
         * <p>
         * A string was passed; it is stored in bstrVal. This pointer must be obtained and freed by the BSTR functions, which are described
         * in Conversion and Manipulation Functions.
         * </p>
         */
        VT_BSTR(8),
        /**
         * MSDN:
         * <p>A pointer to an object was specified. This object is known only to implement IDispatch. The object can
         * be queried as to whether it supports any other desired interface by calling QueryInterface on the object. Objects that do not implement
         * IDispatch should be passed using VT_UNKNOWN. </p>
         */
        VT_DISPATCH(9),
        /**
         * MSDN:
         * <p>
         * An SCODE was specified. Generally, operations on error values should raise an exception or
         * propagate the error to the return value, as appropriate.
         * </p>
         */
        VT_ERROR(10),
        /**
         * MSDN:
         * <p>
         * A 16 bit Boolean (True/False) value was specified. A value of 0xFFFF (all bits 1) indicates True; a value of 0 (all bits 0) indicates
         * False. No other values are valid.
         * </p>
         */
        VT_BOOL(11),
        /**
         * MSDN:
         * <p>
         * Invalid. VARIANTARGs must be passed by reference.
         * </p>
         */
        VT_VARIANT(12),
        /**
         * MSDN:
         * <p>
         * A pointer to an object that implements the IUnknown interface is passed
         * </p>
         */
        VT_UNKNOWN(13),
        /**
         * MSDN:
         * <p>
         * Decimal variables are stored as 96-bit (12-byte) unsigned integers scaled by a variable power of 10. VT_DECIMAL uses the entire
         * 16 bytes of the Variant.
         * </p>
         */
        VT_DECIMAL(14),
        /**
         * MSDN:
         * <p>
         * A 1-byte character value is stored.
         * </p>
         */
        VT_I1(16),
        /**
         * MSDN:
         * <p>
         * An unsigned 1-byte character is stored.
         * </p>
         */
        VT_UI1(17),
        /**
         * MSDN:
         * <p>
         * An unsigned 2-byte integer value is stored.
         * </p>
         */
        VT_UI2(18),
        /**
         * MSDN:
         * <p>
         * An unsigned 4-byte integer value is stored.
         * </p>
         */
        VT_UI4(19),
        /**
         * MSDN:
         * <p>
         *  A 8-byte integer value is stored in llVal.
         *  VT_I8 is not available in Windows Millennium Edition and earlier versions, or Windows 2000 and earlier versions.
         * </p>
         */
        VT_I8(20),
        /**
         * MSDN:
         * <p>
         * An integer value is stored.
         * </p>
         */
        VT_INT(22),
        /**
         * MSDN:
         * <p>
         * An unsigned integer value is stored.
         * </p>
         */
        VT_UINT(23),
        /**
         * MSDN:
         * <p>
         * </p>
         */
        VT_RECORD(36),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_I2} was passed.
         * </p>
         */
        VT_ARRAY_I2(ARRAY|VT_I2.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_I4} was passed.
         * </p>
         */
        VT_ARRAY_I4(ARRAY|VT_I4.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_R4} was passed.
         * </p>
         */
        VT_ARRAY_R4(ARRAY|VT_R4.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_R8} was passed.
         * </p>
         */
        VT_ARRAY_R8(ARRAY|VT_R8.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_CY} was passed.
         * </p>
         */
        VT_ARRAY_CY(ARRAY|VT_CY.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_DATE} was passed.
         * </p>
         */
        VT_ARRAY_DATE(ARRAY|VT_DATE.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_BSTR} was passed.
         * </p>
         */
        VT_ARRAY_BSTR(ARRAY|VT_BSTR.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_BOOL} was passed.
         * </p>
         */
        VT_ARRAY_BOOL(ARRAY|VT_BOOL.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_VARIANT} was passed.
         * </p>
         */
        VT_ARRAY_VARIANT(ARRAY|VT_VARIANT.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_DECIMAL} was passed.
         * </p>
         */
        VT_ARRAY_DECIMAL(ARRAY|VT_DECIMAL.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_I1} was passed.
         * </p>
         */
        VT_ARRAY_I1(ARRAY|VT_I1.value),
        /**
         * MSDN:
         * <p>
         * </p>
         */
        VT_ARRAY_UI1(ARRAY|VT_UI1.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_UI1} was passed.
         * </p>
         */
        VT_ARRAY_UI2(ARRAY|VT_UI2.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_UI4} was passed.
         * </p>
         */
        VT_ARRAY_UI4(ARRAY|VT_UI4.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_INT} was passed.
         * </p>
         */
        VT_ARRAY_INT(ARRAY|VT_INT.value),
        /**
         * MSDN:
         * <p>
         * An array of data type {@link #VT_UINT} was passed.
         * </p>
         */
        VT_ARRAY_UINT(ARRAY|VT_UINT.value),
//        VT_BYREF
        ;

        private final int value;

        private Type( int value ) {
            this.value = value;
        }

        /**
         * @return The value of the COM enumeration
         */
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
        // The initial content of a buffer is, in general, undefined. See the documentation of java.nio.Buffer.
        byte[] b = new byte[16]; // this initializes the array with zeros
        image.put(b); // this prints the zeros to the buffer to guarantee, that the buffer is initialized with zeros.
        image.position(0);
    }

    /**
     * Creates an empty {@link Variant} with the given type.
     * @param type The type of the new Variant.
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
     * @param t The new type of the Variant
     */
    public void setType( Type t ) {
        image.putLong(0,t.comEnumValue());
    }

    /**
     * Gets the type of the variant.
     * @return The current type of the Variant
     */
    public Type getType() {
        int varType = image.getInt(0) & 0xFFFF;
        return EnumDictionary.get(Type.class).constant(varType);
    }


    /**
     * The native function that changes the type of the variant
     * @param type the new type
     * @param image the VARIANT as an ByteBuffer
     */
    private static native void changeType0( int type, ByteBuffer image );

    /**
     * Changes the variant type to the specified one.
     */
    private void changeType( Type t ) {
      if(getType() != t){
        changeType0( t.comEnumValue(), image );
      }
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_I1} and returns the byte represented by this Variant
     * @return The byte value of the Variant
     */
    public byte byteValue() {
      changeType(Type.VT_I1);
      byte[] b = new byte[1];
      image.get(b);
      return b[0];
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_I1} and sets its value to the given parameter.
     * @param i The new value.
     */
    public void set(byte i) {
        changeType(Type.VT_I1);
        image.putInt(8,i);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_I2} and returns the short represented by this Variant
     * @return the short value of this Variant.
     */
    public short shortValue() {
      changeType(Type.VT_I2);
      return image.getShort(8);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_I2} and sets its value to the given parameter.
     * @param i The new value.
     */
    public void set(short i) {
        changeType(Type.VT_I2);
        image.putInt(8,i);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_I4} and returns the int value represented
     * by this Variant
     * @return the int value of this Variant.
     */
    public int intValue() {
        changeType(Type.VT_I4);
        return image.getInt(8);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_I4} and sets its value to the given parameter.
     * @param i The new value.
     */
    public void set(int i) {
      // does also work and uses the native part: set0(i, image);
        changeType(Type.VT_I4);
        image.putInt(8,i);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_I8} and returns the long value
     * represented by this Variant
     * @return The long value of this Variant
     */
    public long longValue() {
      changeType(Type.VT_I8);
      return image.getLong(8);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_I8} and sets its value to the given parameter.
     * @param i The new value.
     */
    public void set(long i) {
        changeType(Type.VT_I8);
        image.putLong(8,i);
    }

    /**
     * Changes the Variant to represent a COM error with the given HRESULT
     * @param hresult The HRESULT of the error.
     */
    /*package*/ void makeError(int hresult) {
        clear();
        image.putShort(0,(short)Type.VT_ERROR.comEnumValue());
        image.putInt(8,hresult);
    }

    /**
     * Returns the HRESULT error code of this Variant
     * @return the HRESULT error code.
     */
    public int getError(){
      return image.getInt(8);
    }


    /**
     * Changes the type of the Variant to {@link Type#VT_R4} and returns the float represented by this Variant
     * @return the float value of this Variant.
     */
    public float floatValue() {
        changeType(Type.VT_R4);
       return image.getFloat(8);
    }


    /**
     * Changes the type of the Variant to {@link Type#VT_R4} and sets its value to the given parameter.
     * @param f The new value.
     */
    public void set(float f) {
        changeType(Type.VT_R4);
        image.putFloat(8,f);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_R8} and returns the double represented by this Variant
     * @return the double value of this Variant.
     */
    public double doubleValue() {
        changeType(Type.VT_R8);
        return image.getDouble(8);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_R8} and sets its value to the given parameter.
     * @param d The new value.
     */
    public void set(double d) {
        changeType(Type.VT_R8);
        image.putDouble(8,d);
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_BOOL} and sets its value to the given parameter.
     * @param b The new value.
     */
    public void set(boolean b){
      changeType(Type.VT_BOOL);
      image.putShort(8, (short) (b ? 0xffff : 0));
    }

    /**
     * Changes the type of the Variant to {@link Type#VT_BOOL} and returns the boolean represented by this Variant
     * @return the boolean value of this Variant.
     */
    public boolean booleanValue(){
      changeType(Type.VT_BOOL);
      return image.getShort(8) == 0xffff;
    }

    /**
     * Converts this Variant to a String and returns its value
     * @return the String representation of this Variant.
     */
    public String stringValue() {
        return convertTo(String.class);
    }

    /**
     * Sets the value of the {@link Variant} to the given {@link String}
     * @param value The new String value.
     */
    public void set(String value){
      set0(value, image);
    }

    /**
     * Retrieves the current contents of the {@link Variant} and returns an object. The type of the object depends on the type of the
     * {@link Variant}.
     * @see #set0(Object, ByteBuffer)
     * @return The current value.
     */
    public Object get(){
      return get0(image);
    }

// see set0(Object, ByteBuffer)
//    public void set(Object o){
//      set0(o, image);
//    }

    /**
     * <p>
     * This method is able to set a new value to the underlying VARIANT using native functions. It is able to determine the
     * type of the new value using IsAssignableForm and uses type converters on the native side.
     * </p>
     * <p>
     * This means we could use set0 to set all values. Instead of
     * <pre>
     * public void set(float f) {
     *   changeType(Type.VT_R4);
     *   image.putFloat(8,f);
     * }
     * </pre>
     * We could write
     * <pre>
     * public void set(float f) {
     *   set0(f, image);
     * }
     * </pre>
     * or even
     * <pre>
     * public void set(Object o) {
     *   set0(o, image);
     * }
     * </pre>
     * to cover all types at once! But this would annul the type checking.
     * </p>
     */
    private native void set0(Object value, ByteBuffer image);

    /**
     * This method is able to retrieve the value of the Variant and return an appropriate Java Object. The type of the object depends on the
     * type of the VARIANT. This means, that this method returns an {@link Integer} object, if the {@link Variant} is of type
     * {@link Type#VT_INT} and it returns an {@link String} object if the {@link Variant} is of the type {@link Type#VT_ARRAY_BSTR}.
     *
     * @param image The image of the VARIANT to retrieve the value.
     * @return The value of the Variant.
     */
    private native Object get0(ByteBuffer image);


    /**
     * Generates an String representation of this Variant that can be parsed.
     * @return A parsable String representation of this Variant
     */
    public String getParseableString(){
      // TODO expand this
      switch (this.getType()) {
        case VT_I1:
        case VT_I2:
        case VT_I4:
        case VT_INT:
          return Integer.toString(this.intValue());
        case VT_I8:
          return Long.toString(this.longValue());
        case VT_R4:
          return Float.toString(this.floatValue());
        case VT_R8:
          return Double.toString(this.doubleValue());
        case VT_BSTR:
          return this.stringValue();
        case VT_NULL:
          return "null";
        case VT_BOOL:
          return Boolean.toString(this.booleanValue());
        case VT_ERROR:
          return Integer.toHexString(this.getError());
      }
      System.err.println("Don't know how to print " + this.getType().name() + " as an Java literal");
      return null;
    }

    /**
     * Returns a Java literal as a String of this Variant
     * <p>
     * For example, if the Variant is of the type VT_R4 and its value is 4.6, then the
     * Java literal is "4.6f".
     * </p>
     * @return The Java literal.
     */
    public String getJavaCode(){
      // TODO expand this
      switch (this.getType()) {
        case VT_I1:
          return "(byte) " + Integer.toString(this.intValue());
        case VT_I2:
          return "(short) " + Integer.toString(this.intValue());
        case VT_I4:
        case VT_INT:
          return Integer.toString(this.intValue());
        case VT_I8:
          return Long.toString(this.longValue())+"L";
        case VT_R4:
          return Float.toString(this.floatValue()) + "f";
        case VT_R8:
          return Double.toString(this.doubleValue());
        case VT_BSTR:
          return "\"" + this.stringValue() + "\"";
        case VT_NULL:
          return "null";
        case VT_BOOL:
          return Boolean.toString(this.booleanValue());
        case VT_ERROR:
          if(isMissing()) {
            return "com4j.Variant.getMissing()";
          } // else: Does it make any sense to generate an other error value than missing?
          break;
      }
      System.err.println("Don't know how to print " + this.getType().name() + " as Java program code");
      return null;
    }

    /**
     * Reads this VARIANT as a COM interface pointer.
     * @param <T> The type of the return value
     * @param type The class object of the type
     * @return The {@link Com4jObject} of the IUnknown interface pointer that this Variant represents.
     */
    public <T extends Com4jObject> T object( final Class<T> type ) {
        // native method invocation changeType needs to happen in the COM thread, that is responsible for this variant
        // @see ComCollection#fetch
        ComThread t = thread != null ? thread : ComThread.get();
        return new Task<T>() {
            public T call() {
                Com4jObject wrapper = convertTo(Com4jObject.class);
                if(null == wrapper) {
                	return null;
                }
                
                T ret = wrapper.queryInterface(type);
                wrapper.dispose();
                return ret;
            }
        }.execute(t);
    }

    /**
     * Converts the variant to the given object type.
     * @param type The class object of the destination type
     * @param <T> The type of the return value.
     * @return An object of type &lt;T&gt;
     */
    public native <T> T convertTo( Class<T> type );

    /**
     * Represents the special variant instance used for
     * missing parameters.
     *
     * @deprecated
     *      This constant instance is mutable (both by Java methods and COM code where
     *      this gets passed into as a reference), so it's fundamentally unsafe.
     *      use {@link #getMissing()} instead.
     */
    public static final Variant MISSING = new Variant();

    /**
     * Generates a new Variant object, representing the VARIANT MISSING
     * @return A new instance of the MISSING variant.
     */
    public static Variant getMissing(){
      Variant v = new Variant();
      v.makeError(MISSING_ERROR_CODE);
      return v;
    }

    /**
     * Tests if this Variant represents the VARIANT MISSING
     * @return true if this Variant represents MISSING, false otherwise.
     */
    public boolean isMissing(){
      if(getType() != Type.VT_ERROR){
        return false;
      }
      return getError() == MISSING_ERROR_CODE;
    }

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
//        d += 0.5; // round
        int min = (int)d;
        ret.add(Calendar.MINUTE,min);

        d -= min;
        d *= 60;
        int secs = (int) d;
        ret.add(Calendar.SECOND, secs);

        return ret.getTime();
    }

    /**
     * Opposite of the {@link #toDate(double)} method.
     */
    static double fromDate(Date dt) {

        // the number of milliseconds since January 1, 1970, 00:00:00 GMT
        long t = dt.getTime();

        // the number of milliseconds since January 1, 1970, 00:00:00 Local Time
        Calendar c = new GregorianCalendar();
        c.setTime(dt);
        t += (c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)) ;

        // the number of milliseconds since December 30, 1899, 00:00:00 Local Time
        t += 2209161600000L;

        // DATE is an offset from "30 December 1899"
        if (t < 0) {
            // -0.3 -> -0.7
            long offset = -(t % MSPD);    // TODO: check
            t = t - MSPD + offset;
        }
        double d = ((double) t) / MSPD;
        return d;
    }

    /**
     * Returns a String representation of this Variant
     * <p>
     * A Variant consists of 16 bytes in memory. The String representation of a Variant is a sequence of
     * 16 bytes in hex
     * </p>
     * @return The hexadecimal value of the {@link ByteBuffer} of this Variant
     */
    public String toString(){
      byte[] b = new byte[16];
      image.position(0);
      image.get(b, 0, 16);
      StringBuilder sb = new StringBuilder();
      for(int i = 0; i < b.length; i++){
        sb.append(Integer.toHexString(b[i]));
      }
      return sb.toString();
    }

    /**
     * # of milliseconds per day.
     */
    private static final long MSPD = 24*60*60*1000;
}
