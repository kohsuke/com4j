package com4j.tlbimp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Stack;

/**
 * {@link PrintWriter} with a little additional capability.
 *
 * <p>
 * Specifically,
 * <ol>
 *  <li>Indentation.
 *  <li>Printing comma-separated tokens.
 * </ol>
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class IndentingWriter extends PrintWriter {
    private int indent=0;
    private boolean newLine=true;

    public IndentingWriter(Writer out) {
        super(out);
    }

    public IndentingWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public IndentingWriter(OutputStream out) {
        super(out);
    }

    public IndentingWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }



//
//
// indentation
//
//
    /**
     * Increases the indentation level.
     */
    public void in() {
        indent++;
    }

    /**
     * Decreases the indentation level.
     */
    public void out() {
        indent--;
    }

    private void printIndent() {
        try {
            for( int i=0; i<indent; i++ )
                out.write("    ");
        } catch( IOException e ) {
        }
    }

    private void checkIndent() {
        if(newLine)
            printIndent();
        newLine = false;
    }


//
//
// comma-separated tokens
//
//
    /**
     * If true, we need to print ',' in the next {@link #comma()}.
     */
    private boolean needsComma;

    private final Stack<Boolean> commaStack = new Stack<Boolean>();

    /**
     * Starts the comma-separated token mode.
     */
    public void beginCommaMode() {
        commaStack.push(needsComma);
        needsComma = false;
    }

    /**
     * Ends the comma-separated token mode.
     */
    public void endCommaMode() {
        needsComma |= (boolean)commaStack.pop();
    }

    /**
     * Prints out ',' if something was printed since
     * the last invocation of {@link #comma()} or {@link #beginCommaMode()}.
     */
    public void comma() {
        if(needsComma) {
            print(',');
            needsComma = false;
        }
    }



//
//
// overriding the base class methods
//
//
    public void println() {
        super.println();
        newLine = true;
    }

    public void write(int c) {
        checkIndent();
        needsComma = true;
        super.write(c);
    }

    public void write(char buf[], int off, int len) {
        checkIndent();
        needsComma = true;
        super.write(buf, off, len);
    }

    public void write(String s, int off, int len) {
        checkIndent();
        needsComma = true;
        super.write(s, off, len);
    }
}
