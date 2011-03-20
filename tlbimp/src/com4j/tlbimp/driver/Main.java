package com4j.tlbimp.driver;

import com4j.ComException;
import com4j.GUID;
import com4j.tlbimp.BindingException;
import com4j.tlbimp.CodeWriter;
import com4j.tlbimp.DumpCodeWriter;
import com4j.tlbimp.ErrorListener;
import com4j.tlbimp.FileCodeWriter;
import com4j.tlbimp.TypeLibInfo;
import com4j.tlbimp.def.IWTypeLib;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Type library importer.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main implements ErrorListener {
    @Option(name="-o")
    public File outDir = new File("-");
    @Option(name="-p")
    public String packageName = "";
    @Option(name="-debug")
    public boolean debug;
    @Option(name="-v")
    public boolean verbose;

    @Option(name="-libid")
    public String libid = null;
    @Option(name="-libver")
    public String libVersion = null;

    @Option(name="-locale")
    public String locale = null;

    @Option(name="-javaGetterSetterName",usage="Generate getters/setters in the Java naming convention")
    public boolean javaGetterSetterName = false;

    @Option(name="-alwaysUseComEnums",usage="Always use ComEnum for generating enums")
    public boolean alwaysUseComEnums = false;

    @Argument
    private List<String> files = new ArrayList<String>();

    public static void main(String[] args) {
        System.exit(new Main().doMain(args));
    }

    private void usage() {
        System.err.println(Messages.USAGE);
    }

    private int doMain(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            usage();
            return -1;
        }

        if(libid!=null) {
            if( !files.isEmpty() ) {
                System.err.println(Messages.CANT_SPECIFY_LIBID_AND_FILENAME);
                usage();
                return -1;
            }
            try {
                TypeLibInfo tli = TypeLibInfo.locate(new GUID(libid),libVersion);
                if(verbose)
                    System.err.printf("Found %1s <%2s>\n",tli.libName,tli.version);

                files = Arrays.asList(tli.typeLibrary.toString());
            } catch( BindingException e ) {
                error(e);
                return -1;
            }
        } else {
            // expect type library file names in the command line.
            if(files.size()<1) {
                System.err.println(Messages.NO_FILE_NAME);
                usage();
                return -1;
            }
        }

        CodeWriter cw;
        if(outDir.getPath().equals("-")) {
            if(debug)
                cw = new DumpCodeWriter();
            else {
                System.err.println(Messages.NO_OUTPUT_DIR);
                usage();
                return -1;
            }
        } else
            cw = new FileCodeWriter(outDir);

        Driver driver = new Driver();
        driver.setPackageName(packageName);

        for( String file : files ) {
            File typeLibFileName = new File(file);
            if(!typeLibFileName.exists()) {
                System.err.println(Messages.NO_SUCH_FILE.format(typeLibFileName));
                return -1;
            }

            Lib lib = new Lib();
            lib.setFile(typeLibFileName);
            driver.addLib(lib);
        }

        driver.alwaysUseComEnums = alwaysUseComEnums;
        driver.renameGetterAndSetters = javaGetterSetterName;

        try {
            if(locale!=null)
                driver.setLocale(locale);
            driver.run(cw,this);
        } catch( ComException e ) {
            return handleException(e);
        } catch( IOException e ) {
            return handleException(e);
        } catch( BindingException e ) {
            return handleException(e);
        }

        return 0;
    }

    private int handleException( Exception e) {
        if(debug) {
            e.printStackTrace(System.err);
            return 1;
        } else {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    public void started(IWTypeLib lib) {
        System.err.println("Generating definitions from "+lib.getName());
    }

    public void error(BindingException e) {
        handleException(e);
    }

    public void warning(String message) {
        System.err.println(message);
    }
}
