package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;
import com4j.Com4jObject;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{7BF089F3-5C19-45f8-B95A-90D762580914}")
public interface IWMethod extends Com4jObject {
    @VTID(3)
    String getName();
}
