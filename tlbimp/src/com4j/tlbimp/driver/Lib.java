package com4j.tlbimp.driver;

import java.io.File;

import com4j.COM4J;
import com4j.GUID;
import com4j.tlbimp.BindingException;
import com4j.tlbimp.TypeLibInfo;
import com4j.tlbimp.def.IWTypeLib;

/**
 * Reference to another type library and which package it is in.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
public final class Lib {
    /**
     * Library GUID.
     */
    private GUID libid;

    /**
     * Optional library version.
     * Null to designate the latest package.
     */
    private String libver;

    /**
     * Java package name to put the generated files into.
     */
    private String packageName = null;

    /**
     * The file that contains a type library.
     */
    private File file;

    /**
     * True to avoid generating source code for this type library.
     * (It's assumed to be present somewhere else already.)
     */
    private boolean suppress = false;

    public void setLibid(String libid) {
        this.libid = new GUID(libid);
    }

    public void setLibid(GUID libid) {
        this.libid = libid;
    }

    public void setLibver(String ver) {
        this.libver = ver;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    public void setSuppress(boolean b) {
        this.suppress = b;
    }

    public void setFile(File file) {
        this.file = file;

        if(file!=null && !file.exists())
            throw new IllegalArgumentException(Messages.NO_SUCH_FILE.format(file));
    }

    public File getFile() throws BindingException {
        if(file==null) {
            TypeLibInfo tli = TypeLibInfo.locate(libid, libver);
            file = tli.typeLibrary;
        }
        return file;
    }

    public String getPackage() {
        return packageName;
    }

    public boolean suppress() {
        return suppress;
    }

    public GUID getLibid() {
        if(libid==null) {
            IWTypeLib tlb = COM4J.loadTypeLibrary(file).queryInterface(IWTypeLib.class);
            libid = tlb.getLibid();
            tlb.dispose();
        }
        return libid;
    }

    /**
     * Makes sure if the class is properly configured.
     */
    void validate() {
        if(libid==null && file==null)
            throw new IllegalArgumentException("either libid or file must be set");
    }
}
