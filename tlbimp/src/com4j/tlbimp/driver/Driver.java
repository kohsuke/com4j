package com4j.tlbimp.driver;

import com4j.GUID;
import com4j.tlbimp.BindingException;
import com4j.tlbimp.CodeWriter;
import com4j.tlbimp.ErrorListener;
import com4j.tlbimp.Generator;
import com4j.tlbimp.ReferenceResolver;
import com4j.tlbimp.def.IWTypeLib;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
final class Driver {

    private final Map<GUID,Ref> refs = new HashMap<GUID,Ref>();

    private String packageName="";

    public void addRef( Ref r ) {
        refs.put(r.libid,r);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }


    public void run( final IWTypeLib mainLib, CodeWriter cw, final ErrorListener el ) throws BindingException, IOException {

        final Set<IWTypeLib> libsToGen = new HashSet<IWTypeLib>();

        ReferenceResolver resolver = new ReferenceResolver() {
            public String resolve(IWTypeLib lib) {
                if(lib.equals(mainLib))
                    return packageName;

                GUID libid = lib.getLibid();
                if( refs.containsKey(libid) )
                    return refs.get(libid).packageName;

                if( libid.equals(GUID_STDOLE))
                    return "";  // don't generate STDOLE. That's done by com4j runtime.

                if( libsToGen.add(lib) )
                    el.warning(Messages.REFERENCED_TYPELIB_GENERATED.format(lib.getName(),packageName));

                return packageName;
            }
        };

        Generator generator = new Generator(cw,resolver,el);

        generator.generate(mainLib);

        // repeatedly generate all the libraries that need to be generated
        Set<IWTypeLib> generatedLibs = new HashSet<IWTypeLib>();
        while(!generatedLibs.containsAll(libsToGen) ) {
            Set<IWTypeLib> s = new HashSet<IWTypeLib>(libsToGen);
            s.removeAll(generatedLibs);
            for( IWTypeLib lib : s ) {
                generator.generate(lib);
                generatedLibs.add(lib);
            }
        }

        // wrap up
        generator.finish();
    }

    private static final GUID GUID_STDOLE = new GUID("{00020430-0000-0000-C000-000000000046}");
}
