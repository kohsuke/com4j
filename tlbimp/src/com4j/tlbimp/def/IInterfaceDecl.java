package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;
import com4j.GUID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{EE076FF5-2E16-4a23-AE24-5DF610F6006E}")
public interface IInterfaceDecl extends ITypeDecl, IInterface {
    @VTID(6)
    GUID getGUID();

    @VTID(7)
    int countMethods();

    @VTID(8)
    IMethod getMethod(int idx);

    /**
     * count the number of the base interfaces
     */
    @VTID(9)
    int countBaseInterfaces();

    /**
     * gets the base interface
     */
    @VTID(10)
    ITypeDecl getBaseInterface(int index);
}
