package com4j.tlbimp.driver;

import com4j.GUID;

/**
 * Reference to another type library and which package it is in.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
final class Ref {
    GUID libid;
    String packageName;

    public void setLibid(String libid) {
        this.libid = new GUID(libid);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Makes sure if the class is properly configured.
     */
    void validate() {
        if(libid==null)
            throw new IllegalArgumentException("libid is not set");

        if(packageName==null)
            throw new IllegalArgumentException("package name is not set");
    }
}
