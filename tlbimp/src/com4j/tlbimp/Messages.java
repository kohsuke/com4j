package com4j.tlbimp;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Localization of the messages.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
enum Messages {

    RETVAL_MUST_BY_REFERENCE,
    UNSUPPORTED_VARTYPE,
    UNSUPPORTED_TYPE,
    FAILED_TO_BIND,
    USAGE,
    NO_FILE_NAME,
    NO_OUTPUT_DIR,
    CANT_SPECIFY_LIBID_AND_FILENAME,
    INVALID_LIBID,
    NO_VERSION_AVAILABLE,
    INVALID_VERSION,
    NO_WIN32_TYPELIB
    ;

    private static final ResourceBundle rb = ResourceBundle.getBundle(Messages.class.getName());

    String format( Object... args ) {
        return MessageFormat.format( rb.getString(name()), args );
    }

    public String toString() {
        return format();
    }
}
