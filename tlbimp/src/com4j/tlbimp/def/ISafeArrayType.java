package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{255C24C4-225E-4bd7-B699-A5B852C43919}")
public interface ISafeArrayType extends IType {
    @VTID(3)
    IType getComponentType();
}
