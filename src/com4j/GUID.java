package com4j;



/**
 * GUID.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class GUID {
    /**
     * The first 64 bits of the GUID.
     */
    public final long l1;
    /**
     * The second 64 bits of the GUID.
     */
    public final long l2;

    /**
     * Parses the string representation "{xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}".
     */
    public GUID( String str ) {
        if(str.length()!=32+6)
            throw new IllegalArgumentException("not a GUID: "+str);

        // {xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}
        //  (1)      (10) (15) (20) (25)
        l1 = parse(str,1,8)|(parse(str,10,4)<<32)|(parse(str,15,4)<<48);
        l2 = parse(str,20,2)|
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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GUID)) return false;

        final GUID guid = (GUID) o;

        if (l1 != guid.l1) return false;
        if (l2 != guid.l2) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (l1 ^ (l1 >>> 32));
        result = 29 * result + (int) (l2 ^ (l2 >>> 32));
        return result;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(38);
        buf.append('{');
        toHex(buf,(l1&0x00000000FFFFFFFFL)    ,8);
        buf.append('-');
        toHex(buf,(l1&0x0000FFFF00000000L)>>32,4);
        buf.append('-');
        toHex(buf,(l1&0xFFFF000000000000L)>>48,4);
        buf.append('-');
        toHex(buf,(l2&0x00000000000000FFL)    ,2);
        toHex(buf,(l2&0x000000000000FF00L)>> 8,2);
        buf.append('-');
        toHex(buf,(l2&0x0000000000FF0000L)>>16,2);
        toHex(buf,(l2&0x00000000FF000000L)>>24,2);
        toHex(buf,(l2&0x000000FF00000000L)>>32,2);
        toHex(buf,(l2&0x0000FF0000000000L)>>40,2);
        toHex(buf,(l2&0x00FF000000000000L)>>48,2);
        toHex(buf,(l2&0xFF00000000000000L)>>56,2);
        buf.append('}');
        return buf.toString();
    }

    private static final char[] digits = "0123456789ABCDEF".toCharArray();

    private void toHex( StringBuffer buf, long n, int len ) {
        for( int i=len-1; i>=0; i-- ) {
            buf.append( digits[((int)(n>>(i*4)))&0xF] );
        }
    }
}
