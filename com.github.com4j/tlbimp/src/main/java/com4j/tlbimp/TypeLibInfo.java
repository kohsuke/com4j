package com4j.tlbimp;

import com4j.COM4J;
import com4j.ComException;
import com4j.GUID;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Locates
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class TypeLibInfo {

    /**
     * Human readable library name.
     */
    public final String libName;

    /**
     * Type library file.
     */
    public final File typeLibrary;

    /**
     * Type library version.
     */
    public final Version version;

    /**
     * Locale ID.
     */
    public final int lcid;

    public TypeLibInfo(String libName, File typeLibrary, Version version, int lcid) {
        this.libName = libName;
        this.typeLibrary = typeLibrary;
        this.version = version;
        this.lcid = lcid;
    }

    /**
     * Locates the type library file from the LIBID (a GUID) and an optional version number.
     *
     * @param libid
     *      String of the form "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
     * @param version
     *      Optional version number. If null, the function searches for the latest version.
     *
     * @throws ComException
     *      If it fails to find the type library.
     */
    public static TypeLibInfo locate( GUID libid, String version ) throws BindingException {
        // make sure to load the com4j.dll
        COM4J.IID_IUnknown.toString();

        // check if libid is correct
        if(libid==null)     throw new IllegalArgumentException();
        String libKey = "TypeLib\\"+libid;
        try {
            Native.readRegKey(libKey);
        } catch( ComException e ) {
            throw new BindingException(Messages.INVALID_LIBID.format(libid),e);
        }


        if(version==null) {
            // find the latest version
            List<Version> versions = new ArrayList<Version>();
            for( String v : Native.enumRegKeys(libKey) ) {
                versions.add(new Version(v));
            }
            Collections.sort(versions);

            if( versions.size()==0 )
                throw new BindingException(Messages.NO_VERSION_AVAILABLE.format());

            version = versions.get(versions.size()-1).toString();
        }

        String verKey = "TypeLib\\"+libid+"\\"+version;
        String libName;
        try {
            libName = Native.readRegKey(verKey);
        } catch( ComException e ) {
            throw new BindingException(Messages.INVALID_VERSION.format(version),e);
        }

        Set<Integer> lcids = new HashSet<Integer>();

        for( String lcid : Native.enumRegKeys(verKey) ) {
            try {
                lcids.add(Integer.valueOf(lcid));
            } catch( NumberFormatException e ) {
                ; // ignore "FLAGS" and "HELPDIR"
            }
        }

        int lcid;
        if( lcids.contains(0) )     lcid=0;
        else                        lcid=lcids.iterator().next();

        String fileName;
        try {
            fileName = Native.readRegKey(verKey+"\\"+lcid+"\\win32");
        } catch( ComException e ) {
            throw new BindingException(Messages.NO_WIN32_TYPELIB.format(libid,version),e);
        }

        return new TypeLibInfo( libName, new File(fileName), new Version(version), lcid );
    }

    /**
     * Represents the version number of the form "x.y"
     */
    public static final class Version implements Comparable<Version> {
        public final int major;
        public final int minor;
        public final String version;

        public Version(String name) {
            int idx = name.indexOf('.');
            major = Integer.valueOf(name.substring(0,idx),16);
            minor = Integer.valueOf(name.substring(idx+1),16);
            version = name;
        }

        public int compareTo(Version rhs) {
            if(this.major!=rhs.major)
                return this.major-rhs.major;
            else
                return this.minor-rhs.minor;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Version)) return false;

            final Version version = (Version) o;

            if (major != version.major) return false;
            if (minor != version.minor) return false;

            return true;
        }

        public int hashCode() {
            return 29 * major + minor;
        }

        public String toString() {
            return version;
        }
    }
}
