package com4j.tlbimp.def;

import com4j.Com4jObject;
import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{AF7CC95D-EC5C-4904-B2ED-E1E8838A4377}")
public interface ITypeDecl extends Com4jObject, IType {
    @VTID(3)
    String getName();

    @VTID(4)
    String getHelpString();

    @VTID(5)
    TypeKind getKind();

    @VTID(6)
    IWTypeLib getParent();
}
