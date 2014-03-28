package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;
import com4j.GUID;

/**
 * CoClass declaration
 */
@IID("{A2F511E4-CC26-4337-A4F4-EA992190D082}")
public interface ICoClassDecl extends ITypeDecl {
    /**
     * count the number of the interfaces implemented by this co-class
     */
    @VTID(7)
    int countImplementedInterfaces();

    /**
     * gets an interface implemented by this co-class
     */
    @VTID(8)
    IImplementedInterfaceDecl getImplementedInterface(
        int index);

    /**
     * checks if this co-class is creatable
     */
    @VTID(9)
    boolean isCreatable();

    @VTID(10)
    GUID getGUID();
}
