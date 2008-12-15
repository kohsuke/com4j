package com4j.tlbimp;

import java.io.FilterWriter;
import java.io.IOException;
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
 *  <li>Buffering the certain portion of the output and canceling it later.
 * </ol>
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class IndentingWriter extends PrintWriter {
    private int indent=0;
    private boolean newLine=true;

    public IndentingWriter(Writer out) {
        super(new CancellableWriter(out));
    }

    public IndentingWriter(Writer out, boolean autoFlush) {
        super(new CancellableWriter(out), autoFlush);
    }

    private CancellableWriter getOut() {
        return (CancellableWriter)out;
    }


//
//
// buffering, cancelling, and committing
//
//
    public void startBuffering() {
        try {
            getOut().mark();
        } catch (IOException e) {
        }
    }

    public void cancel() {
        getOut().cancel();
    }

    public void commit() {
        try {
            getOut().commit();
        } catch (IOException e) {
        }
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

    public void printJavadoc(String doc) {
        if(doc!=null) {
            println("/**");
            println(" * "+doc);
            println(" */");
        }
    }
}

class CancellableWriter extends FilterWriter {
    /**
     * Text that might be cancelled later will be buffered here.
     */
    private final StringBuffer buffer = new StringBuffer();

    private boolean marked;

    /**
     * Once called, successive writing will be buffered until
     * cancel() or commit() is called later.
     */
    public void mark() throws IOException {
        if(marked)  commit();
        marked = true;
    }

    /**
     * Cancel the data written since the last {@link #mark()} method.
     */
    public void cancel() {
        if(!marked)     throw new IllegalStateException();
        marked = false;
        buffer.setLength(0);
    }

    /**
     * Write the pending data.
     */
    public void commit() throws IOException {
        if(!marked)     throw new IllegalStateException();
        marked = false;
        super.append(buffer);
        buffer.setLength(0);
    }


    public CancellableWriter(Writer out) {
        super(out);
    }

    public void write(int c) throws IOException {
        if(marked)
            buffer.append( (char)c );
        else
            super.write(c);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        if(marked)
            buffer.append(cbuf,off,len);
        else
            super.write(cbuf, off, len);
    }

    public void write(String str, int off, int len) throws IOException {
        if(marked)
            buffer.append(str,off,len);
        else
            super.write(str, off, len);
    }

    public void flush() throws IOException {
        super.flush();
    }

    public void close() throws IOException {
        if(marked)
            commit();
        super.close();
    }
}