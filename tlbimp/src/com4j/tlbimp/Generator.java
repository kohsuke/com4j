package com4j.tlbimp;

import com4j.tlbimp.def.IWTypeLib;
import com4j.tlbimp.def.IWType;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

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
            generate(t);
            t.release();
        }
    }

    private void generatePackageHtml(IWTypeLib lib) throws IOException {
        PrintWriter o = createWriter( new File(getPackageDir(),"package.html" ) );
        o.println("<html><body>");
        o.printf("<h2>{0}</h2>",lib.getName());
        o.printf("<p>{0}</p>",lib.getHelpString());
        o.println("</html></body>");
        o.close();
    }

    private void generate( IWType t ) throws IOException {
        String typeName = t.getName();
        PrintWriter o = createWriter( new File(getPackageDir(),typeName ) );
        o.println("// GENERATED. DO NOT MODIFY");
        if(packageName.length()!=0) {
            o.printf("package {0}",packageName);
            o.println();
            o.println();
        }

        o.println("import com4j.*;");
        o.println();


        o.printf("@IID(\"{0}\")",t.getGUID());
        o.println();
        o.printf("interface {0} '{'",typeName);
        o.println();
    }
}
