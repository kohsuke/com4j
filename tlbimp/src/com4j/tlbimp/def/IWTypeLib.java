package com4j.tlbimp.def;

import com4j.Com4jObject;
import com4j.IID;
import com4j.VTID;
import com4j.GUID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{2CC69AC1-D228-401b-83D9-7A7C42E5DBD9}")
public interface IWTypeLib extends Com4jObject {
    @VTID(3)
    int count();

    @VTID(4)
    GUID getLibid();

    @VTID(5)
    String getName();

    @VTID(6)
    String getHelpString();

    @VTID(7)
    ITypeDecl getType( int index );
}
