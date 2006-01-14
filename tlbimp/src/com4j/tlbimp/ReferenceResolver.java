package com4j.tlbimp;

import com4j.tlbimp.def.IWTypeLib;

/**
 * Resolves a reference to another type library.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ReferenceResolver {
    /**
     * @return
     *      the package name in which this given type library is
     *      generated.
     */
    String resolve(IWTypeLib lib) throws BindingException;

    /**
     * @return
     *      true to avoid generating code for this library.
     */
    boolean suppress(IWTypeLib lib);
}
