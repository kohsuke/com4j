package com4j;

import static com4j.Const.*;

/**
 * Native method type.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public enum NativeType {
    /**
     * <tt>BSTR</tt>.
     *
     * TODO: support CharSequence
     * <p>
     * Expected Java type:
     *      String
     */
    BSTR(1),

    /**
     * <tt>BSTR*</tt>.
     *
     * TODO: support StringBuffer
     * <p>
     * Expected Java type:
     *      {@link Holder}<String>
     */
    BSTR_ByRef(1|BYREF),


    /**
     * String will be marshalled as "wchar_t*".
     *
     * More concretely, it becomes a L'\0'-terminated
     * UTF-16LE format.
     */
    Unicode(2),
    /**
     * String will be marshalled as "char*".
     *
     * <p>
     * More concretely, it becomes a '\0'-terminated
     * multi-byte string where characters are encoded
     * according to the platform default encoding.
     *
     * <p>
     * For example, on a typical English Windows system
     * this is just an ASCII string (more or less). On
     * a typical Japanese Windows system, this is
     * Shift-JIS.
     */
    CSTR(3),

    Int8(100),
    Int16(101),
    Int32(102),

    /**
     * Used only with {@link ReturnValue} for returning
     * HRESULT of the method invocation as "int".
     */
    HRESULT(200),

    ;

    /**
     * Passed to the native code.
     */
    final int code;

    NativeType( int code ) {
        this.code = code;
    }
}
