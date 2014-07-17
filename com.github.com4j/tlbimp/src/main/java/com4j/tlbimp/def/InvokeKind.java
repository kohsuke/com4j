package com4j.tlbimp.def;

import com4j.ComEnum;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public enum InvokeKind implements ComEnum {
    FUNC(1),
    PROPERTYGET(2),
    PROPERTYPUT(4),
    PROPERTYPUTREF(8);


    private final int value;

    InvokeKind(int value) {
        this.value=value;
    }

    public int comEnumValue() {
        return value;
    }
}
