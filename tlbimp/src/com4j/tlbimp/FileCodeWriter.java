package com4j.tlbimp;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;

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
        file.mkdirs();
        // TODO: proper escaping
        return new IndentingWriter(new FileWriter(file));
    }
}
