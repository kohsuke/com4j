package com4j.tlbimp.def;

import com4j.GUID;
import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{3BDCCFBF-B493-4d54-B1D0-4DE2FB1AFC78}")
public interface IDispInterfaceDecl extends ITypeDecl, IInterface {
    @VTID(6)
    GUID getGUID();

    @VTID(7)
    int countMethods();

    @VTID(8)
    IMethod getMethod(int idx);

    /**
     * get the vtable interface of this dispatch interface. Works only when this is a dual interface.
     */
    @VTID(9)
    IInterfaceDecl getVtblInterface();
}
