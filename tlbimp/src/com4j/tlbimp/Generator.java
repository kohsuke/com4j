package com4j.tlbimp;

import com4j.NativeType;
import com4j.Variant;
import com4j.Com4jObject;
import com4j.tlbimp.def.IConstant;
import com4j.tlbimp.def.IDispInterfaceDecl;
import com4j.tlbimp.def.IEnumDecl;
import com4j.tlbimp.def.IInterfaceDecl;
import com4j.tlbimp.def.IMethod;
import com4j.tlbimp.def.IParam;
import com4j.tlbimp.def.IPrimitiveType;
import com4j.tlbimp.def.IPtrType;
import com4j.tlbimp.def.IType;
import com4j.tlbimp.def.ITypeDecl;
import com4j.tlbimp.def.ITypedefDecl;
import com4j.tlbimp.def.IWTypeLib;
import com4j.tlbimp.def.TypeKind;
import com4j.tlbimp.def.VarType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Generator {
    /**
     * Root of the output directory.
     */
    private final File outDir;

    /**
     * Type library.
     */
    private final IWTypeLib lib;

    /**
     * Package to produce the output.
     * Can be empty, but never be null.
     */
    private final String packageName;

    private final Map<ITypeDecl,String> aliases
        = new HashMap<ITypeDecl,String>();

    /**
     *
     * @param packageName
     *      Package to produce the output. Can be empty, but never be null.
     */
    public static void generate( IWTypeLib lib, File outDir, String packageName ) throws IOException, BindingException {
        new Generator(lib,outDir,packageName)._generate();
    }


    private Generator( IWTypeLib lib, File outDir, String packageName ) {
        this.lib = lib;
        this.outDir = outDir;
        this.packageName = packageName;

        buildSimpleAliasTable();
    }

    /**
     * MIDL often produces typedefs of the form "typedef A B" where
     * B is an enum declaration, and A is the name given by the user in IDL.
     *
     * <p>
     * I don't know why MIDL behaves in this way, but in this case
     * it's usually desirable as a binding if we use A everywhere in place of B.
     *
     * <p>
     * We build this map B -> A to simply this.
     */
    private void buildSimpleAliasTable() {
        int len = lib.count();
        for( int i=0; i<len; i++ ) {
            ITypeDecl t = lib.getType(i);
            if(t.getKind()==TypeKind.ALIAS) {
                ITypedefDecl typedef = t.queryInterface(ITypedefDecl.class);
                ITypeDecl def = typedef.getDefinition().queryInterface(ITypeDecl.class);
                if(def!=null) {
//                    System.out.println(def.getName()+" -> "+typedef.getName());
                    aliases.put( def, typedef.getName() );
                }
            }
            t.release();
        }
    }

    /**
     * Gets the type name for the given declaration.
     * <p>
     * This takes the aliases into account.
     */
    private String getTypeName(ITypeDecl decl) {
        if(aliases.containsKey(decl))
            return aliases.get(decl);
        else
            return decl.getName();
    }


    private File getPackageDir() {
        File f = new File(outDir,packageName);
        f.mkdirs();
        return f;
    }

    private IndentingWriter createWriter( File f ) throws IOException {
//        // TODO: handle encoding better
//        return new PrintWriter(new FileWriter(f));
        return new IndentingWriter(System.out,true);
    }

    private void _generate() throws IOException, BindingException {
        generatePackageHtml(lib);

        int len = lib.count();
        for( int i=0; i<len; i++ ) {
            ITypeDecl t = lib.getType(i);
            switch(t.getKind()) {
            case DISPATCH:
                generate( t.queryInterface(IDispInterfaceDecl.class).getVtblInterface() );
                break;
            case INTERFACE:
                generate( t.queryInterface(IInterfaceDecl.class) );
                break;
            case ENUM:
                generate( t.queryInterface(IEnumDecl.class) );
                break;
            case ALIAS:
                {
                    ITypedefDecl alias = t.queryInterface(ITypedefDecl.class);
                    System.out.printf("typedef %1s %2s", alias.getName(),
                        getTypeString(alias.getDefinition()));
                    System.out.println();
                    break;
                }
            default:
                System.out.println( t.getKind() );
                break;
            }
            t.release();
        }
    }

    private void generatePackageHtml(IWTypeLib lib) throws IOException {
        PrintWriter o = createWriter( new File(getPackageDir(),"package.html" ) );
        o.println("<html><body>");
        o.printf("<h2>%1s</h2>",lib.getName());
        o.printf("<p>%1s</p>",lib.getHelpString());
        o.println("</html></body>");
//        o.close();    // TODO: close
    }


    private void generate( IEnumDecl t ) throws IOException {

        // load all the constants first
        int len = t.countConstants();
        IConstant[] cons = new IConstant[len];

        for( int i=0; i<len; i++ )
            cons[i] = t.getConstant(i);

        // check if we need to use ComEnum
        boolean needComEnum = false;
        for( int i=0; i<len; i++ ) {
            if( cons[i].getValue()!=i ) {
                needComEnum = true;
                break;
            }
        }

        // generate the prolog
        String typeName = getTypeName(t);
        IndentingWriter o = createWriter( new File(getPackageDir(),typeName ) );
        generateHeader(o);

        printJavadoc(t.getHelpString(), o);

        o.printf("enum %1s ",typeName);
        if(needComEnum)
            o.print("implements ComEnum ");
        o.println("{");
        o.in();

        // generate constants
        for( IConstant con : cons ) {
            printJavadoc(con.getHelpString(),o);
            o.print(con.getName());
            if(needComEnum) {
                o.printf("(%1d),",con.getValue());
            } else {
                o.print(", // ");
                o.print(con.getValue());
            }
            o.println();
        }

        if(needComEnum) {
            // the rest of the boiler-plate code
            o.println(";");
            o.println();
            o.println("private final int value;");
            o.println(typeName+"(int value) { this.value=value; }");
            o.println("public int comEnumValue() { return value; }");
        }

        o.out();
        o.println("}");

        // clean up
        for( IConstant con : cons)
            con.release();

//        o.close();    // TODO: close
    }

    private void generate( IInterfaceDecl t ) throws IOException, BindingException {
        try {
            String typeName = getTypeName(t);
            IndentingWriter o = createWriter( new File(getPackageDir(),typeName ) );
            generateHeader(o);

            printJavadoc(t.getHelpString(), o);

            o.printf("@IID(\"%1s\")",t.getGUID());
            o.println();
            o.printf("interface %1s",typeName);
            if(t.countBaseInterfaces()!=0) {
                o.print(" extends ");
                o.beginCommaMode();
                for( int i=0; i<t.countBaseInterfaces(); i++ ) {
                    o.comma();
                    String baseName = getTypeName(t.getBaseInterface(i));
                    if(baseName.equals("IUnknown") || baseName.equals("IDispatch"))
                        baseName = "Com4jObject";
                    o.print(baseName);
                }
                o.endCommaMode();
            }
            o.println(" {");
            o.in();

            for( int j=0; j<t.countMethods(); j++ ) {
                IMethod m = t.getMethod(j);
                generate(m,o);
                m.release();
            }

            o.out();
            o.println("}");

            // o.close(); // TODO:
        } catch( BindingException e ) {
            throw new BindingException(
                Messages.FAILED_TO_BIND.format(t.getName()),
                e );
        }
    }

    private void generateHeader(IndentingWriter o) {
        o.println("// GENERATED. DO NOT MODIFY");
        if(packageName.length()!=0) {
            o.printf("package %1s",packageName);
            o.println();
            o.println();
        }

        o.println("import com4j.*;");
        o.println();
    }

    /**
     * Binds a native method to a Java method.
     */
    private class MethodBinder {
        private final IMethod method;
        private final IParam[] params;

        public MethodBinder(IMethod method) {
            this.method = method;

            int len = method.getParamCount();
            params = new IParam[len];
            for( int i=0; i<len; i++ )
                params[i] = method.getParam(i);
        }

        /**
         * Returns the index of the return value parameter,
         * or -1 if none.
         */
        private int getReturnParam() {
             for( int i=0; i<params.length; i++ ) {
                if(params[i].isRetval())
                    return i;
            }
            return -1;
        }

        public void declare( IndentingWriter o ) throws BindingException {
            printJavadoc(method.getHelpString(), o);
//            o.println("// "+method.getKind());
            o.printf("@VTID(%1d)",
                method.getVtableIndex());
            o.println();

            declareReturnType(o);

            o.print(camelize(method.getName()));
            o.print('(');
            o.in();

            boolean first = true;
            // declare parameters
            for( IParam p : params ) {
                if( p.isRetval() && !p.isIn() )
                    continue;   // skip, cause it's showing up as the return value
                if(!first)
                    o.println(',');
                else
                    o.println();
                first = false;
                declare(p,o);
            }
            o.out();
            o.println(");");
        }

        /**
         * Declares a parameter.
         */
        private void declare( IParam p, IndentingWriter o ) throws BindingException {
            VariableBinding vb = bind(p.getType());
            if(!vb.isDefault) {
                o.printf("@MarshalAs(NativeType.%1s) ",vb.nativeType.name());
            }
            o.print(vb.javaType);
            o.print(' ');
            String name = p.getName();
            if(name==null)  name="rhs";
            o.print(camelize(name));
        }

        /**
         * Declares the return type.
         */
        private void declareReturnType(IndentingWriter o) throws BindingException {
            int r = getReturnParam();
            if(r==-1) {
                o.print("void ");
            } else {
                // we assume that the [retval] param to be passed by reference
                IPtrType pt = params[r].getType().queryInterface(IPtrType.class);
                if(pt==null)
                    throw new BindingException(Messages.RETVAL_MUST_BY_REFERENCE.format());
                VariableBinding vb = bind(pt.getPointedAtType());

                // add @ReturnType if necessary
                if(!vb.isDefault || params[r].isIn() || r!=params.length-1 ) {
                    o.print("@ReturnType(");
                    o.beginCommaMode();
                    if(!vb.isDefault) {
                        o.comma();
                        o.print("type=NativeType."+vb.nativeType.name());
                    }
                    if(params[r].isIn()) {
                        o.comma();
                        o.print("inout=true");
                    }
                    if(r!=params.length-1) {
                        o.comma();;
                        o.print("index="+r);
                    }
                    o.endCommaMode();
                    o.println(")");
                }

                o.print(vb.javaType);
                o.print(' ');
            }
        }
    }

    private static class VariableBinding {
        public final String javaType;
        public final NativeType nativeType;
        /**
         * True if the {@link #nativeType} is the default native type
         * for the {@link #javaType}.
         */
        public final boolean isDefault;

        public VariableBinding(Class javaType, NativeType nativeType, boolean isDefault) {
            this(javaType.getName(),nativeType,isDefault);
        }

        public VariableBinding(String javaType, NativeType nativeType, boolean isDefault) {
            this.javaType = javaType;
            this.nativeType = nativeType;
            this.isDefault = isDefault;
        }
    }

    /**
     * Defines the type bindings for the primitive types.
     */
    private static final Map<VarType,VariableBinding> primitiveTypeBindings
        = new EnumMap<VarType,VariableBinding>(VarType.class);

    static {
        // initialize the primitive binding
        pbind( VarType.VT_I2, Short.TYPE, NativeType.Int16, true );
        pbind( VarType.VT_I4, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_BSTR, String.class, NativeType.BSTR, true );
        // TODO: is it OK to map UI2 to short?
        pbind( VarType.VT_UI2, Short.TYPE, NativeType.Int16, true );
        pbind( VarType.VT_UI4, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_INT, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_UINT, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_BOOL, Boolean.TYPE, NativeType.VariantBool, true );
        pbind( VarType.VT_VARIANT, Object.class, NativeType.VARIANT_ByRef, true );
        pbind( VarType.VT_DISPATCH, Com4jObject.class, NativeType.Dispatch, false );
        pbind( VarType.VT_UNKNOWN, Com4jObject.class, NativeType.ComObject, true );
        pbind( VarType.VT_DATE, Date.class, NativeType.Date, true );
        // TODO
//        pbind( VarType.VT_R4, Float.TYPE, NativeType.Float );
//        pbind( VarType.VT_R8, Double.TYPE, NativeType.Double );
    }

    private static void pbind( VarType vt, Class c, NativeType n, boolean isDefault ) {
        primitiveTypeBindings.put(vt,new VariableBinding(c,n,isDefault));
    }

    /**
     * Binds the native type to a Java type and its conversion.
     */
    private VariableBinding bind( IType t ) throws BindingException {
        IPrimitiveType pt = t.queryInterface(IPrimitiveType.class);
        if(pt!=null) {
            // primitive
            VariableBinding r = primitiveTypeBindings.get(pt.getVarType());
            if(r!=null)     return r;

            throw new BindingException(Messages.UNSUPPORTED_VARTYPE.format(pt.getVarType()));
        }

        IPtrType ptrt = t.queryInterface(IPtrType.class);
        if(ptrt!=null) {
            // pointer type
            IType comp = ptrt.getPointedAtType();
            ITypeDecl compDecl = comp.queryInterface(ITypeDecl.class);
            if( compDecl!=null )
                // t = T* where T is a declared interface
                return new VariableBinding( getTypeName(compDecl), NativeType.ComObject, true );

            IPrimitiveType compPrim = comp.queryInterface(IPrimitiveType.class);
            if( compPrim!=null ) {
                if( compPrim.getVarType()==VarType.VT_VARIANT ) {
                    // T = VARIANT*
                    return new VariableBinding(Object.class, NativeType.VARIANT_ByRef, true);
                }
            }
            // TODO
            throw new UnsupportedOperationException(getTypeString(t));
        }

        // T = typedef
        ITypedefDecl typedef = t.queryInterface(ITypedefDecl.class);
        if(typedef!=null) {
            return bind(typedef.getDefinition());
        }

        // T = enum
        IEnumDecl enumdef = t.queryInterface(IEnumDecl.class);
        if(enumdef!=null) {
            // TODO: we should probably check the size of the enum.
            // there's no guarantee it's 32 bit.
            return new VariableBinding( getTypeName(enumdef), NativeType.Int32, true );
        }

        ITypeDecl declt = t.queryInterface(ITypeDecl.class);
        if(declt!=null) {
            // TODO: not clear how we should handle this
            String name = declt.getName();
            if(name.equals("GUID")) {
                return new VariableBinding( "GUID", NativeType.GUID, true );
            }
            throw new UnsupportedOperationException("other decl "+declt.getKind());
        }

        throw new BindingException(Messages.UNSUPPORTED_TYPE.format());
    }


    private void generate(IMethod m, IndentingWriter o) throws BindingException {
        try {
            new MethodBinder(m).declare(o);
            o.println();
        } catch (BindingException e) {
            throw new BindingException(
                Messages.FAILED_TO_BIND.format(m.getName()),
                e );
        }
    }

    private void printJavadoc(String doc, PrintWriter o) {
        if(doc!=null) {
            o.println("/**");
            o.println(" * "+doc);
            o.println(" */");
        }
    }

    private String getTypeString(IType t) {
        if(t==null)
            return "null";

        IPtrType pt = t.queryInterface(IPtrType.class);
        if(pt!=null)
            return getTypeString(pt.getPointedAtType())+"*";

        IPrimitiveType prim = t.queryInterface(IPrimitiveType.class);
        if(prim!=null)
            return prim.getName();

        ITypeDecl decl = t.queryInterface(ITypeDecl.class);
        if(decl!=null)
            return getTypeName(decl);

        return "N/A";
    }

    private static String camelize(String s) {
        if(Character.isUpperCase(s.charAt(0))) {
            // change "ThisKindOfName" to "thisKindOfName"
            return Character.toLowerCase(s.charAt(0))+s.substring(1);
        } else {
            return s;
        }
    }
}
