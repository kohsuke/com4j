package com4j.tlbimp;

import java.util.List;
import java.util.ArrayList;

/**
 * Signals a failure in the binding process.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */

@SuppressWarnings("serial")
public class BindingException extends Exception {

    public BindingException(String message) {
        super(message);
    }

    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public BindingException(Throwable cause) {
        super(cause);
    }

    private final List<String> contexts = new ArrayList<String>();

    void addContext( String ctxt ) {
        contexts.add(ctxt);
    }

    public String getMessage() {
        StringBuilder buf = new StringBuilder();
        buf.append(super.getMessage());
        for( String s : contexts )
            buf.append("\n  ").append(s);
        return buf.toString();
    }
}
