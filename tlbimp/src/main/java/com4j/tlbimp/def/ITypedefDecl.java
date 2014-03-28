package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{1FA456D6-6E48-4ff0-9BF8-300937470A02}")
public interface ITypedefDecl extends ITypeDecl {
    @VTID(7)
    IType getDefinition();
}
