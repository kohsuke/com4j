package com4j.tlbimp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class FileCodeWriter implements CodeWriter {
    private final File outDir;

    public FileCodeWriter(File outDir) {
        this.outDir = outDir;
    }

    public IndentingWriter create(File file) throws IOException {
        file = new File(outDir,file.getPath());
        File dir = file.getParentFile();
        boolean newCreated = dir.mkdirs();
        boolean exists = dir.exists();
        if(!exists){
          throw new IOException("Could not create the directory "+ file.getParentFile().getAbsolutePath());
        }
        // TODO: proper escaping
        return new IndentingWriter(new FileWriter(file));
    }
}
