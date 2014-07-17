package com4j.tlbimp.def;

import com4j.Com4jObject;
import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{579779F8-E889-43cc-9C07-F412A5374970}")
public interface IConstant extends Com4jObject {
    @VTID(3)
    String getName();

    @VTID(4)
    IType getType();

    @VTID(5)
    int getValue();

    @VTID(6)
    String getHelpString();
}
