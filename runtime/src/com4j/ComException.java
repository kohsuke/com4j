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

@SuppressWarnings("serial")
public class ComException extends RuntimeException {

    private final int hresult;

    private final String fileName;
    private final int line;

    private ErrorInfo errorInfo;

    /**
     * Constructs a new ComException with the given values
     * @param msg the message text of the Exception
     * @param hresult the HRESULT value of the COM error
     * @param fileName the file name of the source where the error occurred
     * @param line the line in the file where the error occurred
     */
    public ComException( String msg, int hresult, String fileName, int line ) {
        super(Integer.toHexString(hresult)+' '+cutEOL(msg));
        this.hresult = hresult;
        this.fileName = fileName;
        this.line = line;
    }

    /**
     * Constructs a new ComException with the given values.
     * @param msg the message text of the Exception
     * @param fileName the file name of the source where the error occurred
     * @param line the line in the file where the error occurred
     */
    public ComException( String msg, String fileName, int line ) {
        super(msg);
        this.hresult = -1;
        this.fileName = fileName;
        this.line = line;
    }

    /**
     * Constructs a new ComException with the given values
     * @param msg the message text of the Exception
     * @param hresult the HRESULT value of the COM error
     */
    public ComException(String msg, int hresult) {
        this(msg,hresult,null,-1);
    }

    /**
     * Constructs a new ComException with the given ComExcepton as cause.
     * @param cause the line in the file where the error occurred
     */
    public ComException(ComException cause) {
        super(cause.getDetailMessage(),cause);
        this.hresult = cause.hresult;
        this.fileName = cause.fileName;
        this.line = cause.line;
        this.errorInfo = cause.errorInfo;
    }

    /**
     * Sets the ErrorInfo of this ComException
     * @param errorInfo the new ErrorInfo
     */
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
     * Returns the HRESULT value of this error.
     * @return the HRESULT value of this error
     */
    public int getHRESULT() {
        return hresult;
    }

    /**
     * Cuts off the end of line characters.
     * @param s the original String
     * @return
     */
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

    /**
     * Returns the message of the superclass
     * @return the message of the superclass
     */
    public String getDetailMessage() {
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
