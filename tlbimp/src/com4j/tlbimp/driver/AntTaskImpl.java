package com4j.tlbimp.driver;

import com4j.COM4J;
import com4j.ComException;
import com4j.tlbimp.BindingException;
import com4j.tlbimp.ErrorListener;
import com4j.tlbimp.FileCodeWriter;
import com4j.tlbimp.TypeLibInfo;
import com4j.tlbimp.def.IWTypeLib;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.IOException;

/**
 * tlbimp implemented as an Ant task.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AntTaskImpl extends Task implements ErrorListener {

    private File destDir;
    private File source;

    private String libid;
    private String libver;

    private Driver driver = new Driver();


    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    public void setPackage(String packageName) {
        driver.setPackageName(packageName);
    }

    public void setFile(File source) {
        this.source = source;
    }

    public void setLibid(String libid) {
        this.libid = libid;
    }

    public void setLibver(String libver) {
        this.libver = libver;
    }

    public void addConfiguredRef( Ref r ) {
        r.validate();
        driver.addRef(r);
    }

    public void execute() throws BuildException {
        if( destDir==null )
            throw new BuildException("@destDir is missing");
        if( source==null ) {
            if( libid==null )
                throw new BuildException("both @file and @libid is missing");
            try {
                TypeLibInfo tli = TypeLibInfo.locate(libid,libver);
                log("The type library is "+tli.libName+" <"+tli.version+">", Project.MSG_INFO);
                source = tli.typeLibrary;
            } catch (BindingException e) {
                error(e);
                return;
            }
        }

        try {
            if(!source.exists())
                throw new BuildException(Messages.NO_SUCH_FILE.format(source));

            log("Generating definitions from "+source, Project.MSG_INFO);
            driver.run(
                COM4J.loadTypeLibrary(source).queryInterface(IWTypeLib.class),
                new FileCodeWriter(destDir),
                this);
        } catch( ComException e ) {
            throw new BuildException(e);
        } catch( IOException e ) {
            throw new BuildException(e);
        } catch( BindingException e ) {
            throw new BuildException(e);
        }
    }

    public void error(BindingException e) {
        log(e.getMessage(),Project.MSG_ERR);
    }

    public void warning(String message) {
        log(message,Project.MSG_WARN);
    }
}
