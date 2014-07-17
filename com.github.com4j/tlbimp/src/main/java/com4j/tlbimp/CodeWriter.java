package com4j.tlbimp;

import java.io.File;
import java.io.IOException;

/**
 * {@link Generator} uses this interface to create output.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface CodeWriter {
    /**
     * Creates a new {@link IndentingWriter} used to write the given
     * source code / HTML file, etc.
     *
     * @param file
     *      A relative File like "org/acme/Foo.java". The callee is
     *      expected to absolutize.
     */
    IndentingWriter create( File file ) throws IOException;
}
