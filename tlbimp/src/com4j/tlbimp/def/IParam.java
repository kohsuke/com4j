package com4j.tlbimp.def;

import com4j.Com4jObject;
import com4j.IID;
import com4j.VTID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{8082BEBE-CC6C-44ee-BDF3-0A9BD5B2107B}")
public interface IParam extends Com4jObject {
    @VTID(3)
    String getName();

    @VTID(4)
    IType getType();

    @VTID(5)
    boolean isIn();

    @VTID(6)
    boolean isOut();

    @VTID(7)
    boolean isRetval();
}
