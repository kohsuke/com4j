package com4j.tlbimp.def;

import com4j.ComEnum;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public enum VarType implements ComEnum {
    VT_I2(2),
    VT_I4(3),
    VT_R4(4),
    VT_R8(5),
    VT_CY(6),
    VT_DATE(7),
    VT_BSTR(8),
    VT_DISPATCH(9),
    VT_ERROR(10),
    VT_BOOL(11),
    VT_VARIANT(12),
    VT_UNKNOWN(13),
    VT_DECIMAL(14),
    VT_I1(16),
    VT_UI1(17),
    VT_UI2(18),
    VT_UI4(19),
    VT_I8(20),
    VT_UI8(21),
    VT_INT(22),
    VT_UINT(23),
    VT_VOID(24),
    VT_HRESULT(25),
    VT_PTR(26),
    VT_SAFEARRAY(27),
    VT_CARRAY(28),
    VT_USERDEFINED(29),
    VT_LPSTR(30),
    VT_LPWSTR(31);

    private final int value;

    VarType(int value) {
        this.value=value;
    }

    public int comEnumValue() {
        return value;
    }
}
