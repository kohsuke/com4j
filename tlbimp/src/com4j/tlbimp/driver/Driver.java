package com4j.tlbimp.driver;

import com4j.GUID;
import com4j.COM4J;
import com4j.tlbimp.BindingException;
import com4j.tlbimp.CodeWriter;
import com4j.tlbimp.ErrorListener;
import com4j.tlbimp.Generator;
import com4j.tlbimp.ReferenceResolver;
import com4j.tlbimp.TypeLibInfo;
import com4j.tlbimp.def.IWTypeLib;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
final class Driver {

    private final Map<GUID,Lib> libs = new HashMap<GUID,Lib>();

    private String packageName="";

    private Locale locale = Locale.getDefault();

    boolean renameGetterAndSetters = false;
    boolean alwaysUseComEnums = false;
    boolean generateDefaultMethodOverloads = false;


    public void addLib( Lib r ) {
        libs.put(r.getLibid(),r);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setLocale(String locale) {
        String[] tokens = locale.split("_");
        this.locale = new Locale(
            tokens.length>0 ? tokens[0] : "",
            tokens.length>1 ? tokens[1] : "",
            tokens.length>2 ? tokens[2] : ""
        );
    }


    public void run( CodeWriter cw, final ErrorListener el ) throws BindingException, IOException {

        final Set<IWTypeLib> libsToGen = new HashSet<IWTypeLib>();
        for (Lib lib : libs.values()) {
            libsToGen.add(COM4J.loadTypeLibrary(lib.getFile()).queryInterface(IWTypeLib.class));
        }

        ReferenceResolver resolver = new ReferenceResolver() {
            public String resolve(IWTypeLib lib) {
                GUID libid = lib.getLibid();
                if( libs.containsKey(libid) ) {
                    String pkg = libs.get(libid).getPackage();
                    if(pkg!=null)
                        return pkg;
                }

                // TODO: move this to a filter
                if( libid.equals(GUID.GUID_STDOLE))
                    return "";  // don't generate STDOLE. That's replaced by com4j runtime.

                if( libsToGen.add(lib) )
                    el.warning(Messages.REFERENCED_TYPELIB_GENERATED.format(lib.getName(),packageName));

                return packageName;
            }

            public boolean suppress(IWTypeLib lib) {
                GUID libid = lib.getLibid();

                if( libid.equals(GUID.GUID_STDOLE))
                    return true;

                Lib r = libs.get(libid);
                if(r!=null)
                    return r.suppress();
                else
                    return false;
            }
        };

        Generator generator = new Generator(cw,resolver,el,locale);
        generator.setAlwaysUseComEnums(alwaysUseComEnums);
        generator.setRenameGetterAndSetters(renameGetterAndSetters);
        generator.setGenerateDefaultMethodOverloads(generateDefaultMethodOverloads);

        // repeatedly generate all the libraries that need to be generated
        Set<IWTypeLib> generatedLibs = new HashSet<IWTypeLib>();
        while(!generatedLibs.containsAll(libsToGen) ) {
            Set<IWTypeLib> s = new HashSet<IWTypeLib>(libsToGen);
            s.removeAll(generatedLibs);
            for( IWTypeLib lib : s ) {
                el.started(lib);
                generator.generate(lib);
                generatedLibs.add(lib);
            }
        }

        // wrap up
        generator.finish();
    }

}
