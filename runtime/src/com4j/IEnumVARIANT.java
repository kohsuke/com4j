package com4j;

/**
 * @author Kohsuke Kawaguchi
 */
@IID("{00020404-0000-0000-C000-000000000046}")
interface IEnumVARIANT extends Com4jObject {
    @VTID(3)
    int next(int count,Variant item);

    @VTID(4)
    void skip(int count);

    @VTID(5)
    void reset();

    @VTID(6)
    IEnumVARIANT clone();
}
