package com4j.tlbimp;

import com4j.tlbimp.def.IWTypeLib;

/**
 * Receives errors found during the type generation.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ErrorListener {
    /**
     * Report a fatal error.
     */
    void error( BindingException e );

    /**
     * Report a non-fatal error.
     */
    void warning( String message );

    /**
     * Report that the {@link Generator} has started working on this.
     */
    void started( IWTypeLib lib );
}
