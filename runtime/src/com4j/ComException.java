package com4j;

/**
 * Signals a failure in the COM method invocation.
 *
 * <p>
 * Calling a wrapped COM method throws this exception
 * when the underlying COM method returns a failure HRESULT code.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ComException extends RuntimeException {
    private final int hresult;

    public ComException( String msg, int hresult ) {
        super(Integer.toHexString(hresult)+' '+cutEOL(msg));
        this.hresult = hresult;
    }

    public ComException( String msg ) {
        this(msg,-1);
    }

    /**
     * Gets the HRESULT code of this error.`
     */
    public int getHRESULT() {
        return hresult;
    }

    private static String cutEOL( String s ) {
        if(s==null)
            return "(Unknown error)";
        if(s.endsWith("\r\n"))
            return s.substring(0,s.length()-2);
        else
            return s;
    }
}
