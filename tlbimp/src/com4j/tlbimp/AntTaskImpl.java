package com4j.tlbimp;

import com4j.COM4J;
import com4j.ComException;
import com4j.tlbimp.def.IWTypeLib;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;

import java.io.File;
import java.io.IOException;

/**
 * tlbimp implemented as an Ant task.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AntTaskImpl extends Task {

    private File destDir;
    private String packageName="";
    private File source;

    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    public void setSource(File source) {
        this.source = source;
    }

    public void execute() throws BuildException {
        if( source==null )
            throw new BuildException("@source is missing");
        if( destDir==null )
            throw new BuildException("@destDir is missing");

        try {
            log("Generating definitions from "+source, Project.MSG_INFO);
            IWTypeLib tlb = COM4J.loadTypeLibrary(source).queryInterface(IWTypeLib.class);
            CodeWriter cw = new FileCodeWriter(destDir);
            Generator.generate(tlb,cw,packageName);
            tlb.release();
        } catch( ComException e ) {
            throw new BuildException(e);
        } catch( IOException e ) {
            throw new BuildException(e);
        } catch( BindingException e ) {
            throw new BuildException(e);
        }
    }
}
