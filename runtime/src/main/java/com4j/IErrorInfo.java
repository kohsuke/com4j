package com4j;

/**
 * Represents <tt>IErrorInfo</tt> object.
 *
 * @author Kohsuke Kawaguchi
 */
@IID("{1CF2B120-547D-101B-8E65-08002B2BD119}")
interface IErrorInfo extends Com4jObject {
    @VTID(3)
    GUID guid();

    @VTID(4)
    String source();

    @VTID(5)
    String description();

    @VTID(6)
    String helpFile();

    @VTID(7)
    int helpContext();
}
