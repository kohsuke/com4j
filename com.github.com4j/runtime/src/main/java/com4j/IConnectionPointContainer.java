package com4j;

/**
 * @author Kohsuke Kawaguchi
 */
@IID("{B196B284-BAB4-101A-B69C-00AA00341D07}")
interface IConnectionPointContainer extends Com4jObject {
    // 3 is EnumConnectionPoints but we don't care.
    @VTID(4)
    Com4jObject /*IConnectionPoint*/ FindConnectionPoint(GUID iid);
}
