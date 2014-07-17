package com4j;



/**
 * Immutable representation of 128-bit COM GUID.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class GUID {
    /**
     * Array of 2 long value to represent the bit image
     */
    final long[] v;

    /**
     * Used internally when GUID is created as a return value from
     * a native method invocation.
     */
    GUID( long[] values ) {
        this.v = values;
    }

    /**
     * Parses the string representation "<tt>{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</tt>"
     * or "<tt>xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</tt>".
     * @param str the String to be parsed
     */
    public GUID( String str ) {
        if(str.length()==32+4)
            str = '{'+str+'}';

        if(str.length()!=32+6)
            throw new IllegalArgumentException("not a GUID: "+str);

        v = new long[2];

        // {xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}
        //  (1)      (10) (15) (20) (25)
        v[0] = parse(str,1,8)|(parse(str,10,4)<<32)|(parse(str,15,4)<<48);
        v[1] = parse(str,20,2)|
            (parse(str,22,2)<< 8)|
            (parse(str,25,2)<<16)|
            (parse(str,27,2)<<24)|
            (parse(str,29,2)<<32)|
            (parse(str,31,2)<<40)|
            (parse(str,33,2)<<48)|
            (parse(str,35,2)<<56);
    }

    private long parse( String s, int idx, int len ) {
        return Long.parseLong(s.substring(idx,idx+len),16);
    }

    /**
     * Returns true if the given object is a {@link GUID} object and has the same bit representation.
     * @param o the second object
     * @return true, if the given GUID object and this object have the same bit representation.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GUID)) return false;

        final GUID guid = (GUID) o;

        if (v[0] != guid.v[0]) return false;
        if (v[1] != guid.v[1]) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (v[0] ^ (v[0] >>> 32));
        result = 29 * result + (int) (v[1] ^ (v[1] >>> 32));
        return result;
    }

    /**
     * Returns the GUID in the "<tt>{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</tt>" format.
     * @return the String representation of this GUID object.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(38);
        buf.append('{');
        toHex(buf,(v[0]&0x00000000FFFFFFFFL)    ,8);
        buf.append('-');
        toHex(buf,(v[0]&0x0000FFFF00000000L)>>32,4);
        buf.append('-');
        toHex(buf,(v[0]&0xFFFF000000000000L)>>48,4);
        buf.append('-');
        toHex(buf,(v[1]&0x00000000000000FFL)    ,2);
        toHex(buf,(v[1]&0x000000000000FF00L)>> 8,2);
        buf.append('-');
        toHex(buf,(v[1]&0x0000000000FF0000L)>>16,2);
        toHex(buf,(v[1]&0x00000000FF000000L)>>24,2);
        toHex(buf,(v[1]&0x000000FF00000000L)>>32,2);
        toHex(buf,(v[1]&0x0000FF0000000000L)>>40,2);
        toHex(buf,(v[1]&0x00FF000000000000L)>>48,2);
        toHex(buf,(v[1]&0xFF00000000000000L)>>56,2);
        buf.append('}');
        return buf.toString();
    }

    private static final char[] digits = "0123456789ABCDEF".toCharArray();

    private void toHex( StringBuffer buf, long n, int len ) {
        for( int i=len-1; i>=0; i-- ) {
            buf.append( digits[((int)(n>>(i*4)))&0xF] );
        }
    }

    /** The NULL GUID */
    public static final GUID GUID_NULL = new GUID("{00000000-0000-0000-0000-000000000000}");

    /** The GUID of STDOLE */
    public static final GUID GUID_STDOLE = new GUID("{00020430-0000-0000-C000-000000000046}");

}
