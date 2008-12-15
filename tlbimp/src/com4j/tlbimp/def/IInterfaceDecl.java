package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
@IID("{EE076FF5-2E16-4a23-AE24-5DF610F6006E}")
public interface IInterfaceDecl extends ITypeDecl, IInterface {
    /**
     * count the number of the base interfaces
     */
    @VTID(12)
    int countBaseInterfaces();

    /**
     * gets the base interface
     */
    @VTID(13)
    ITypeDecl getBaseInterface(int index);
}
