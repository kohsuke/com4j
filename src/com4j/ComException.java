package com4j;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ComException extends RuntimeException {
    private final int hresult;

    public ComException( String msg, int hresult ) {
        super(msg);
        this.hresult = hresult;
    }
}
