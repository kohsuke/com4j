package com4j;



/**
 * Wraps COM VARIANT data structure.
 *
 * This class allows you to deal with the raw VARIANT type in case you need it,
 * but in general you should bind <tt>VARIANT*</tt> to {@link Object} or
 * {@link Holder<Object>} for more natural Java binding.
 *
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
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Variant extends Number {
    /**
     * The memory image of the VARIANT.
     */
    private final long[] image = new long[2];

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
//        VT_ARRAY(),
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
        if(image[0]!=0) {
            clear0(image[0],image[1]);
            image[0] = image[1] = 0;
        }
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
    private static native void clear0( long image0, long image1 );

    /**
     * Sets the type of the variant.
     */
    public void setType( Type t ) {
        image[0] = t.comEnumValue();
    }

    /**
     * Gets the type of the variant.
     */
    public Type getType() {
        return EnumDictionary.get(Type.class).constant((int)image[0]);
    }


    private static native void changeType0( int type, long[] image );

    /**
     * Changes the variant type to the specified one.
     */
    private void changeType( Type t ) {
        changeType0( t.comEnumValue(), image );
    }

    public int intValue() {
        changeType(Type.VT_I4);
        return (int)(image[1] & 0xFFFFFFFF);
    }

    public long longValue() {
        // VARIANT doesn't seem to support 64bit int
        return intValue();
    }

    public float floatValue() {
        return castToFloat0(image);
    }

    public double doubleValue() {
        return castToDouble0(image);
    }

    /**
     * Changes the variant type to {@link Type.VT_R4} and
     * returns its float value.
     */
    private static native float castToFloat0( long[] image );

    /**
     * Changes the variant type to {@link Type.VT_R8} and
     * returns its double value.
     */
    private static native double castToDouble0( long[] image );

}
