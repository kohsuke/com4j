package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{C99EBD48-0DF6-453d-94A8-BC004F69330F}")
public interface IEnumDecl extends ITypeDecl {
    @VTID(3)
    int countConstants();

    @VTID(4)
    IConstant getConstant(int index);
}
