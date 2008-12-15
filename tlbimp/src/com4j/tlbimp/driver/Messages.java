package com4j.tlbimp.driver;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Localization of the messages.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
enum Messages {

    USAGE,
    NO_FILE_NAME,
    NO_OUTPUT_DIR,
    NO_SUCH_FILE,
    CANT_SPECIFY_LIBID_AND_FILENAME,
    REFERENCED_TYPELIB_GENERATED,
    COULDNT_LOCATE_REFERENCED_TYPELIB,
    ;

    private static final ResourceBundle rb = ResourceBundle.getBundle(Messages.class.getName());

    String format( Object... args ) {
        return MessageFormat.format( rb.getString(name()), args );
    }

    public String toString() {
        return format();
    }
}
