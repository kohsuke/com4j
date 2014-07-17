package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;
import com4j.Com4jObject;

/**
 * interface implemented by a co-class
 */
@IID("{9F6412CF-9B35-4B72-A8B9-AF83491E5B73}")
public interface IImplementedInterfaceDecl extends Com4jObject {
    /**
     * is this the default source/sink?
     */
    @VTID(3)
    boolean isDefault();

    /**
     * is this a sink?
     */
    @VTID(4)
    boolean isSource();

    /**
     * is restricted?
     */
    @VTID(5)
    boolean isRestricted();

    /**
     * gets the definition of the interface
     */
    @VTID(6)
    ITypeDecl getType();

}
