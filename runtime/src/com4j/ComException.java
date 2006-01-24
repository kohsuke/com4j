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

    private final String fileName;
    private final int line;

    private ErrorInfo errorInfo;

    public ComException( String msg, int hresult, String fileName, int line ) {
        super(Integer.toHexString(hresult)+' '+cutEOL(msg));
        this.hresult = hresult;
        this.fileName = fileName;
        this.line = line;
    }

    public ComException( String msg, String fileName, int line ) {
        super(msg);
        this.hresult = -1;
        this.fileName = fileName;
        this.line = line;
    }

    /*package*/ void setErrorInfo(ErrorInfo errorInfo) {
        this.errorInfo = errorInfo;
    }

    /**
     * Gets the {@link ErrorInfo} object associated with this error.
     *
     * <p>
     * Some COM objects can report additional error information beyond
     * simple HRESULT value. If an error came from such an COM object,
     * this method returns a non-null value, and you can query the returned
     * {@link ErrorInfo} object for more information about the error.
     *
     * @return
     *      null if this exception doesn't have such detailed information.
     */
    public ErrorInfo getErrorInfo() {
        return errorInfo;
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

    @Override
    public String getMessage() {
        if(errorInfo!=null && errorInfo.getDescription()!=null) {
            return super.getMessage()+" : "+errorInfo.getDescription();
        }
        String s = Native.getErrorMessage(hresult);
        if(s!=null) {
            return super.getMessage()+" : "+s;
        }
        return super.getMessage();
    }

    @Override
    public String toString() {
        String s = super.toString();
        if(fileName!=null) {
            s += " : "+fileName+':'+line;
        }
        return s;
    }
}
