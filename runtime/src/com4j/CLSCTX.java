package com4j;

/**
 * CLSCTX constants.
 *
 * @author Kohsuke Kawaguchi
 */
public interface CLSCTX {
    /**
     * COM object will be in the same process.
     */
    public static final int INPROC_SERVER = 1;
    /**
     *
     */
    public static final int INPROC_HANDLER = 2;
    /**
     * COM object will be in another process on the same machine
     */
    public static final int LOCAL_SERVER = 4;
    /**
     * COM object will be in a remote machine.
     */
    public static final int REMOTE_SERVER = 16;
    /**
     * Use this when you don't care where the server is.
     */
    public static final int ALL = INPROC_HANDLER|INPROC_SERVER|LOCAL_SERVER|REMOTE_SERVER;
}
