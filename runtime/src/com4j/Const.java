package com4j;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class Const {
    /**
     * This constant is used to pass a value "by reference".
     *
     * It means that the corresponding Java method takes {@link Holder},
     * and its value may be updated as a result of the method execution. 
     */
    static final int BYREF=0x8000;
}
