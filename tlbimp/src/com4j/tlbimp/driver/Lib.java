package com4j.tlbimp.driver;

import com4j.GUID;
import com4j.COM4J;
import com4j.tlbimp.TypeLibInfo;
import com4j.tlbimp.BindingException;
import com4j.tlbimp.def.IWTypeLib;

import java.io.File;

import org.apache.tools.ant.BuildException;

/**
 * Reference to another type library and which package it is in.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
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
     * <p>
     * This could be a special token {@link #NONE} to
     * indicate that this type library should not be generated.
     */
    private String packageName;

    /**
     * The file that contains a type library.
     */
    private File file;

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
        if(b)
            this.packageName = NONE;
    }

    public void setFile(File file) {
        this.file = file;

        if(!file.exists())
            throw new BuildException(Messages.NO_SUCH_FILE.format(file));
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

    public static final String NONE = "";
}
