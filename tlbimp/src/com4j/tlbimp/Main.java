package com4j.tlbimp;

import com4j.COM4J;
import com4j.ComException;
import com4j.tlbimp.def.IWTypeLib;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.opts.BooleanOption;
import org.kohsuke.args4j.opts.FileOption;
import org.kohsuke.args4j.opts.StringOption;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main implements ErrorListener {
    public FileOption outDir = new FileOption("-o",new File("-"));
    public StringOption packageName = new StringOption("-p","");
    public BooleanOption debug = new BooleanOption("-debug");
    public BooleanOption verbose = new BooleanOption("-v");

    public StringOption libid = new StringOption("-libid");
    public StringOption libVersion = new StringOption("-libver");

    public static void main(String[] args) {
        System.exit(new Main().doMain(args));
    }

    private void usage() {
        System.err.println(Messages.USAGE);
    }

    private int doMain(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        parser.addOptionClass(this);

        try {
            parser.parse(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            usage();
            return -1;
        }

        List<String> files = (List<String>)parser.getArguments();

        if(libid.value!=null) {
            if( !parser.getArguments().isEmpty() ) {
                System.err.println(Messages.CANT_SPECIFY_LIBID_AND_FILENAME);
                usage();
                return -1;
            }
            try {
                TypeLibInfo tli = TypeLibInfo.locate(libid.value,libVersion.value);
                if(verbose.isOn())
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
        if(outDir.value.getPath().equals("-")) {
            if(debug.isOn())
                cw = new DumpCodeWriter();
            else {
                System.err.println(Messages.NO_OUTPUT_DIR);
                usage();
                return -1;
            }
        } else
            cw = new FileCodeWriter(outDir.value);

        for( String file : files ) {
            File typeLibFileName = new File(file);
            System.err.println("Processing "+typeLibFileName);
            try {
                IWTypeLib tlb = COM4J.loadTypeLibrary(typeLibFileName).queryInterface(IWTypeLib.class);
                Generator.generate(tlb,cw,packageName.value,this);
                tlb.dispose();
            } catch( ComException e ) {
                return handleException(e);
            } catch( IOException e ) {
                return handleException(e);
            }
        }

        return 0;
    }

    private int handleException( Exception e) {
        if(debug.isOn()) {
            e.printStackTrace(System.err);
            return 1;
        } else {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    public void error(BindingException e) {
        handleException(e);
    }
}
