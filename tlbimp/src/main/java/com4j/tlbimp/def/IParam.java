package com4j.tlbimp.def;

import com4j.Com4jObject;
import com4j.IID;
import com4j.NativeType;
import com4j.ReturnValue;
import com4j.VTID;
import com4j.Variant;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
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

    @VTID(8)
    boolean isOptional();

    @VTID(9)
    @ReturnValue(type=NativeType.VARIANT)
    Variant getDefaultValue();

    @VTID(10)
    boolean isLCID();

}
