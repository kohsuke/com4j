package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{BA8E1931-1249-4863-A436-332FA88F645B}")
public interface IPrimitiveType extends IType {
    @VTID(3)
    String getName();

    @VTID(4)
    VarType getVarType();
}
