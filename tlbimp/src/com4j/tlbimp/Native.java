package com4j.tlbimp;

/**
 * Registry access helper methods.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class Native {
    static native String readRegKey( String name );
    static native String[] enumRegKeys( String name );
}
