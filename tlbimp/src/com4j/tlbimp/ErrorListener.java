package com4j.tlbimp;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ErrorListener {
    void error( BindingException e );
}
