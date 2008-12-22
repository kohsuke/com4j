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
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
@SuppressWarnings("serial")
public final class Variant extends Number {
    /**
     * The memory image of the VARIANT.
     */
    final ByteBuffer image;

    private static final int ARRAY = 0x2000;

    private static final int MISSING_ERROR_CODE = 0x80020004;
    /**
     * VARIANT type.
     *
     * This enum only defines constants that are legal for VARIANTs.
     */
    public static enum Type implements ComEnum {
      /**
       * This is not a variant type. It is indicates, that the type is unknown. <br>
       * Its name is <code>NO_TYPE</code> instead of something like <code>UNKNOWN</code> to make it
       * more distinguishable from {@link Type#VT_UNKNOWN}, which indicates that the Variant points to an
       * IUnknown interface.
       */
        NO_TYPE(0),
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
        VT_I1(16),
        VT_UI1(17),
        VT_UI2(18),
        VT_UI4(19),
        VT_I8(20),
        VT_INT(22),
        VT_UINT(23),
        VT_RECORD(36),
        VT_ARRAY_I2(ARRAY|VT_I2.value),
        VT_ARRAY_I4(ARRAY|VT_I4.value),
        VT_ARRAY_R4(ARRAY|VT_R4.value),
        VT_ARRAY_R8(ARRAY|VT_R8.value),
        VT_ARRAY_CY(ARRAY|VT_CY.value),
        VT_ARRAY_DATE(ARRAY|VT_DATE.value),
        VT_ARRAY_BSTR(ARRAY|VT_BSTR.value),
        VT_ARRAY_BOOL(ARRAY|VT_BOOL.value),
        VT_ARRAY_VARIANT(ARRAY|VT_VARIANT.value),
        VT_ARRAY_DECIMAL(ARRAY|VT_DECIMAL.value),
        VT_ARRAY_I1(ARRAY|VT_I1.value),
        VT_ARRAY_UI1(ARRAY|VT_UI1.value),
        VT_ARRAY_UI2(ARRAY|VT_UI2.value),
        VT_ARRAY_UI4(ARRAY|VT_UI4.value),
        VT_ARRAY_INT(ARRAY|VT_INT.value),
        VT_ARRAY_UINT(ARRAY|VT_UINT.value),
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
        // The initial content of a buffer is, in general, undefined. See the documentation of java.nio.Buffer.
        byte[] b = new byte[16]; // this initializes the array with zeros
        image.put(b); // this prints the zeros to the buffer to guarantee, that the buffer is initialized with zeros.
        image.position(0);
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
//        byte[] b = new byte[15];
//        System.out.println("cap = "+image.capacity());
//        System.out.println("pos = "+image.position());
//
//        image.get(b);
//        for(int i = 0; i < b.length; i++){
//          System.out.println(b[i]);
//        }
        int varType = image.getInt(0) & 0xFFFF;
//        System.out.println("VarType = "+varType);
        return EnumDictionary.get(Type.class).constant(varType);
    }


    private static native void changeType0( int type, ByteBuffer image );

    /**
     * Changes the variant type to the specified one.
     */
    private void changeType( Type t ) {
      if(getType() != t){
        changeType0( t.comEnumValue(), image );
      }
    }

    public byte byteValue() {
      changeType(Type.VT_I1);
      byte[] b = new byte[1];
      image.get(b);
      return b[0];
    }

    public void set(byte i) {
        changeType(Type.VT_I1);
        image.putInt(8,i);
    }

    public short shortValue() {
      changeType(Type.VT_I2);
      return image.getShort(8);
    }

    public void set(short i) {
        changeType(Type.VT_I2);
        image.putInt(8,i);
    }

    public int intValue() {
        changeType(Type.VT_I4);
        return image.getInt(8);
    }

    public void set(int i) {
      // does also work and uses the native part: set0(i, image);
        changeType(Type.VT_I4);
        image.putInt(8,i);
    }

    public long longValue() {
      changeType(Type.VT_I8);
      return image.getLong(8);
    }

    public void set(long i) {
        changeType(Type.VT_I8);
        image.putLong(8,i);
    }

    /*package*/ void makeError(int hresult) {
        clear();
        image.putShort(0,(short)Type.VT_ERROR.comEnumValue());
        image.putInt(8,hresult);
    }

    public int getError(){
      return image.getInt(8);
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

    public void set(boolean b){
      changeType(Type.VT_BOOL);
      image.putShort(8, (short) (b ? 0xffff : 0));
    }
    public boolean booleanValue(){
      changeType(Type.VT_BOOL);
      return image.getShort(8) == 0xffff;
    }

    public String stringValue() {
        return convertTo(String.class);
    }

    public void set(String value){
      set0(value, image);
    }

    public Object get(){
      return get0(image);
    }

//    public void set(Object o){
//      set0(o, image);
//    }

    private native void set0(Object value, ByteBuffer image);
    private native Object get0(ByteBuffer image);


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
     */
    public <T extends Com4jObject> T object( final Class<T> type ) {
        // native method invocation like addRef and changeType needs to happen in the COM thread
        return ComThread.get().execute(new Task<T>() {
            public T call() {
                changeType(Type.VT_UNKNOWN);
                int ptr = image.getInt(8);
                if(ptr==0)  return null;
                Native.addRef(ptr);
                return Wrapper.create(type,ptr);
            }
        });
    }

    /**
     * Converts the variant to the given object type.
     */
    public native <T> T convertTo( Class<T> type );

    /**
     * Represents the special variant instance used for
     * missing parameters.
     */

    public static Variant getMissing(){
      Variant v = new Variant();
      v.makeError(MISSING_ERROR_CODE);
      return v;
    }

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
        t -= dt.getTimezoneOffset() * 60 * 1000;

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
