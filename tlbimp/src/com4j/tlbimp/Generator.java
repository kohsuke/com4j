package com4j.tlbimp;

import com4j.tlbimp.def.IWDispInterface;
import com4j.tlbimp.def.IWMethod;
import com4j.tlbimp.def.IWType;
import com4j.tlbimp.def.IWTypeLib;
import com4j.tlbimp.def.IType;
import com4j.tlbimp.def.IPtrType;
import com4j.tlbimp.def.IPrimitiveType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Generator {
    /**
     * Root of the output directory.
     */
    private final File outDir;

    /**
     * Package to produce the output.
     * Can be empty, but never be null.
     */
    private String packageName = "";


    public Generator(File outDir) {
        this.outDir = outDir;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    private File getPackageDir() {
        File f = new File(outDir,packageName);
        f.mkdirs();
        return f;
    }

    private PrintWriter createWriter( File f ) throws IOException {
//        // TODO: handle encoding better
//        return new PrintWriter(new FileWriter(f));
        return new PrintWriter(System.out);
    }

    public void generate( IWTypeLib lib ) throws IOException {
        generatePackageHtml(lib);

        int len = lib.count();
        for( int i=0; i<len; i++ ) {
            IWType t = lib.getType(i);
            switch(t.getKind()) {
            case DISPATCH:
                generate( t.queryInterface(IWDispInterface.class) );
                break;
            }
            t.release();
        }
    }

    private void generatePackageHtml(IWTypeLib lib) throws IOException {
        PrintWriter o = createWriter( new File(getPackageDir(),"package.html" ) );
        o.println("<html><body>");
        o.printf("<h2>%1s</h2>",lib.getName());
        o.printf("<p>%1s</p>",lib.getHelpString());
        o.println("</html></body>");
        o.flush();
//        o.close();
    }

    private void generate( IWDispInterface t ) throws IOException {
        String typeName = t.getName();
        PrintWriter o = createWriter( new File(getPackageDir(),typeName ) );
        o.println("// GENERATED. DO NOT MODIFY");
        if(packageName.length()!=0) {
            o.printf("package %1s",packageName);
            o.println();
            o.println();
        }

        o.println("import com4j.*;");
        o.println();


        o.printf("@IID(\"%1s\")",t.getGUID());
        o.println();
        o.printf("interface %1s {",typeName);
        o.println();

        for( int j=0; j<t.countMethods(); j++ ) {
            IWMethod m = t.getMethod(j);
            generate(m,o);
            m.release();
        }

        o.println("}");

        o.flush();
    }

    private void generate(IWMethod m, PrintWriter o) {
        String doc = m.getHelpString();
        if(doc!=null) {
            o.println("\t/**");
            o.println("\t * "+doc);
            o.println("\t */");
        }
        o.println("\t"+m.getKind());
        o.println("\t"+getTypeString(m.getReturnType())+' '+m.getName());
        o.println();
        o.flush();
    }

    private String getTypeString(IType t) {
        if(t==null)
            return "null";

        IPtrType pt = t.queryInterface(IPtrType.class);
        if(pt!=null)
            return getTypeString(pt.getPointedAtType())+"*";

        IPrimitiveType prim = t.queryInterface(IPrimitiveType.class);
        if(prim!=null)
            return prim.getName();

        IWType decl = t.queryInterface(IWType.class);
        if(decl!=null)
            return decl.getName();
        
        return "N/A";
    }
}
