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
import java.util.List;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {
    public FileOption outDir = new FileOption("-o",new File("-"));
    public StringOption packageName = new StringOption("-p","");
    public BooleanOption debug = new BooleanOption("-debug");

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
        if(files.size()<1) {
            System.err.println(Messages.NO_FILE_NAME);
            usage();
            return -1;
        }

        CodeWriter cw;
        if(outDir.value.getPath().equals("-"))
            cw = new DumpCodeWriter();
        else
            cw = new FileCodeWriter(outDir.value);

        for( String file : files ) {
            File typeLibFileName = new File(file);
            System.err.println("Processing "+typeLibFileName);
            try {
                IWTypeLib tlb = COM4J.loadTypeLibrary(typeLibFileName).queryInterface(IWTypeLib.class);
                Generator.generate(tlb,cw,packageName.value);
                tlb.release();
            } catch( ComException e ) {
                return handleException(e);
            } catch( IOException e ) {
                return handleException(e);
            } catch( BindingException e ) {
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

}
