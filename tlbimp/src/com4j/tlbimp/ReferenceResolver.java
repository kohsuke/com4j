package com4j.tlbimp;

import com4j.tlbimp.def.IWTypeLib;

/**
 * Resolves a reference to another type library.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ReferenceResolver {
    String resolve(IWTypeLib lib) throws BindingException;
}
