package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;
import com4j.GUID;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{3BDCCFBF-B493-4d54-B1D0-4DE2FB1AFC78}")
public interface IWDispInterface extends IWType {
    @VTID(6)
    GUID getGUID();

    @VTID(7)
    int countMethods();

    @VTID(8)
    IWMethod getMethod(int idx);
}
