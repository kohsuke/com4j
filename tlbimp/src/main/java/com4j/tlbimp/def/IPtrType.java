package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{EB8F889F-8944-4faf-80F2-6C2457C224C4}")
public interface IPtrType extends IType {
    @VTID(3)
    IType getPointedAtType();
}
