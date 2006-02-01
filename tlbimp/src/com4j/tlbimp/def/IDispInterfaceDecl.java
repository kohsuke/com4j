package com4j.tlbimp.def;

import com4j.GUID;
import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{3BDCCFBF-B493-4d54-B1D0-4DE2FB1AFC78}")
public interface IDispInterfaceDecl extends ITypeDecl, IInterface {
    @VTID(7)
    GUID getGUID();

    @VTID(8)
    int countMethods();

    @VTID(9)
    IMethod getMethod(int idx);

    /**
     * return true if this interface is a dual interface
     */
    @VTID(10)
    boolean isDual();

    /**
     * get the vtable interface of this dispatch interface. Works only when this is a dual interface.
     *
     * @see #isDual()
     */
    @VTID(11)
    IInterfaceDecl getVtblInterface();
}
