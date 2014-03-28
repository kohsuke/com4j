package com4j.tlbimp;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com4j.GUID;
import com4j.tlbimp.def.IInterface;
import com4j.tlbimp.def.IMethod;
import com4j.tlbimp.def.IProperty;
import com4j.tlbimp.def.InvokeKind;

/**
 * Generates an interface definition.
 *
 * <p>
 * This is the common code between {@link InvocableInterfaceGenerator}
 * and {@link EventInterfaceGenerator}.
 *
 * @author Kohsuke Kawaguchi
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
abstract class InterfaceGenerator<T extends IInterface> {

    /**
     * Interface definition.
     */
    protected final T t;

    /**
     * The root of the generators.
     */
    protected final Generator g;

    protected final Generator.LibBinder lib;

    /**
     * Simple (IOW not fully qualified) name of the type,
     * like "IFoo".
     */
    protected final String simpleName;

    protected InterfaceGenerator(Generator.LibBinder lib, T t) {
        this.t = t;
        this.lib = lib;
        this.g = lib.parent();
        simpleName = lib.getSimpleTypeName(t);
    }

    /**
     * Returns non-null string to generate the type into a subpackage.
     *
     * @return
     *      a string like "foo" or null.
     */
    protected String getSubPackageName() {
        return null;
    }

    protected final void generate() throws IOException {
        String pkg = getSubPackageName();

        IndentingWriter o = lib.createWriter((pkg!=null?pkg+'/':"")+simpleName+".java");
        lib.generateHeader(o,pkg);

        o.printJavadoc(t.getHelpString());

        o.printf("@IID(\"%1s\")",getIID());
        o.println();
        o.printf("%1s %2s",getClassDecl(),simpleName);

        generateExtends(o);

        o.println(" {");
        o.in();
        o.println("// Methods:");

        // see issue 15.
        // to avoid binding both propput and propputref,
        // we'll use this to keep track of what we generated.
        // TODO: what was the difference between propput and propputref?
        Set<String> putMethods = new HashSet<String>();

        for( int j=0; j<t.countMethods(); j++ ) {
            IMethod m = t.getMethod(j);
            InvokeKind kind = m.getKind();
            if(kind== InvokeKind.PROPERTYPUT || kind== InvokeKind.PROPERTYPUTREF) {
                if(!putMethods.add(m.getName()))
                    continue;   // already added
            }
            try {
                o.startBuffering();
                generateMethod(m,o);
                o.commit();
            } catch( BindingException e ) {
                o.cancel();
                e.addContext("interface "+t.getName());
                g.el.error(e);
            }
            m.dispose();
        }
				// Generating getter and setter for the COM IDispatch Properties
        o.println("// Properties:");
        for(int i = 0; i < t.countProperties(); i++){
          try {
            o.startBuffering();
            generateProperty(t.getProperty(i), o);
            o.commit();
          } catch( BindingException e ) {
            o.cancel();
            e.addContext("interface "+t.getName());
            g.el.error(e);
          }
        }

        o.out();
        o.println("}");

        o.close();
    }

    /**
     * Chance to generate "extends ...." portion.
     */
    protected void generateExtends(IndentingWriter o) {
    }

    /**
     * Returns the access modifier + type name,
     * such as "public interface", etc.
     */
    protected abstract String getClassDecl();

    /**
     * Lists up the type names that this object should derive from.
     *
     * @return
     *      Must not be null, but can be empty.
     *      All interfaces are implicitly derived from {@link Com4jObject},
     *      so the returned list doesn't have to have it explicitly.
     */
    protected abstract List<String> getBaseTypes();

    protected abstract GUID getIID();

    protected abstract void generateMethod(IMethod m, IndentingWriter o) throws BindingException;

    protected abstract void generateProperty(IProperty p, IndentingWriter o) throws BindingException;
}
