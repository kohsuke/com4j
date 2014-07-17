package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
@IID("{3BDCCFBF-B493-4d54-B1D0-4DE2FB1AFC78}")
public interface IDispInterfaceDecl extends ITypeDecl, IInterface {

    /**
     * get the vtable interface of this dispatch interface. Works only when this is a dual interface.
     *
     * @see #isDual()
     */
    @VTID(13)
    IInterfaceDecl getVtblInterface();
}
