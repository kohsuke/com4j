package com4j.tlbimp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Escapes the Java reserved words.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class Escape {
    /**
     * Set of all the Java keywords.
     */
    private static final Set<String> reservedWords;

    /**
     * Escapes the identifier if necessary.
     */
    public static String escape( String identifier ) {
        if(reservedWords.contains(identifier))
            return '_'+identifier;
        else
            return identifier;
    }

    static {
        // see http://java.sun.com/docs/books/jls/second_edition/html/lexical.doc.html#229308
        String[] words = new String[] {
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while"
        };
        HashSet<String> s = new HashSet<String>();
        for( String w : words )
            s.add(w);
        reservedWords = Collections.unmodifiableSet(s);
    }
}
