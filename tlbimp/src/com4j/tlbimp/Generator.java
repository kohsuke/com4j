package com4j.tlbimp;

import com4j.Com4jObject;
import com4j.NativeType;
import com4j.tlbimp.def.ICoClassDecl;
import com4j.tlbimp.def.IConstant;
import com4j.tlbimp.def.IDispInterfaceDecl;
import com4j.tlbimp.def.IEnumDecl;
import com4j.tlbimp.def.IImplementedInterfaceDecl;
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
import com4j.tlbimp.def.ISafeArrayType;
import com4j.tlbimp.def.InvokeKind;
import com4j.tlbimp.def.IInterface;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.Buffer;

/**
 * Type library importer.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Generator {
    private final CodeWriter writer;

    private final ReferenceResolver referenceResolver;

    private final ErrorListener el;

    private final DefaultMethodFinder dmf = new DefaultMethodFinder();

    /**
     * {@link IWTypeLib}s specified to the {@link #generate(IWTypeLib)} method.
     */
    private final Set<TypeLibInfo> generatedTypeLibs = new HashSet<TypeLibInfo>();

    public Generator( CodeWriter writer, ReferenceResolver resolver, ErrorListener el ) {
        this.el = el;
        this.writer = writer;
        this.referenceResolver = resolver;
    }

    /**
     * Call this method repeatedly to generate classes from each type library.
     */
    public void generate( IWTypeLib lib ) throws BindingException, IOException {
        TypeLibInfo tli = getTypeLibInfo(lib);
        if(referenceResolver.suppress(lib))
            return; // skip code generation
        if(generatedTypeLibs.add(tli))
            new PackageBinder(tli).generate();
    }

    /**
     * Finally call this method to wrap things up.
     *
     * <p>
     * In particular this generates the ClassFactory class.
     */
    public void finish() throws IOException {
        //Map<String,Set<TypeLibInfo>> byPackage = new HashMap<String,Set<TypeLibInfo>>();
        //for( TypeLibInfo tli : generatedTypeLibs ) {
        //    Set<TypeLibInfo> s = byPackage.get(tli.packageName);
        //    if(s==null)
        //        byPackage.put(tli.packageName,s=new HashSet<TypeLibInfo>());
        //    s.add(tli);
        //}

        // for( Map.Entry<String,Set<TypeLibInfo>> e : byPackage.entrySet() ) {
        for( Package pkg : packages.values() ) {
            TypeLibInfo lib1 = pkg.typeLibs.iterator().next();
            PackageBinder pb = new PackageBinder(lib1);

            if(referenceResolver.suppress(lib1.lib))
                continue;

            // generate ClassFactory
            IndentingWriter o = pkg.createWriter(lib1,"ClassFactory.java");
            pb.generateHeader(o);

            printJavadoc("Defines methods to create COM objects",o);
            o.println("public abstract class ClassFactory {");
            o.in();

            o.println("private ClassFactory() {} // instanciation is not allowed");
            o.println();

            for( TypeLibInfo lib : pkg.typeLibs ) {
                int len = lib.lib.count();
                for( int i=0; i<len; i++ ) {
                    ICoClassDecl t = lib.lib.getType(i).queryInterface(ICoClassDecl.class);
                    if(t==null)     continue;
                    if(!t.isCreatable())    continue;

                    declareFactoryMethod(o, t);
                    t.dispose();
                }
            }

            o.out();
            o.println("}");
            o.close();
        }
    }

    /**
     * Generates a Java package from a type library.
     */
    private class PackageBinder {
        private final TypeLibInfo lib;

        public PackageBinder(TypeLibInfo lib) {
            this.lib = lib;
        }

        public void generate() throws IOException {
            IWTypeLib tlib = lib.lib;
            generatePackageHtml();

            int len = tlib.count();
            for( int i=0; i<len; i++ ) {
                ITypeDecl t = tlib.getType(i);
                switch(t.getKind()) {
                case DISPATCH:
                    generate( t.queryInterface(IDispInterfaceDecl.class) );
                    break;
                case INTERFACE:
                    generate( t.queryInterface(IInterfaceDecl.class) );
                    break;
                case ENUM:
                    generate( t.queryInterface(IEnumDecl.class) );
                    break;
                case ALIAS:
                    {
//                    ITypedefDecl alias = t.queryInterface(ITypedefDecl.class);
//                    System.out.printf("typedef %1s %2s", alias.getName(),
//                        getTypeString(alias.getDefinition()));
                        break;
                    }
                }
                t.dispose();
            }
        }


        private void generatePackageHtml() throws IOException {
            PrintWriter o = lib.createWriter("package.html");
            o.println("<html><body>");
            o.printf("<h2>%1s</h2>",lib.lib.getName());
            o.printf("<p>%1s</p>",lib.lib.getHelpString());
            o.println("</html></body>");
            o.close();
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
            String typeName = lib.getTypeName(t);
            IndentingWriter o = lib.createWriter(typeName+".java");
            generateHeader(o);

            printJavadoc(t.getHelpString(), o);

            o.printf("public enum %1s ",typeName);
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
                con.dispose();

            o.close();
        }

        private void generate( IInterfaceDecl t ) throws IOException {
            String typeName = lib.getTypeName(t);
            IndentingWriter o = lib.createWriter(typeName+".java");
            generateHeader(o);

            printJavadoc(t.getHelpString(), o);

            boolean hasEnum = false;
            for( int j=0; j<t.countMethods() && !hasEnum; j++ ) {
                IMethod m = t.getMethod(j);
                hasEnum = isEnum(m);
            }


            o.printf("@IID(\"%1s\")",t.getGUID());
            o.println();
            o.printf("public interface %1s",typeName);
            if(t.countBaseInterfaces()!=0) {
                o.print(" extends ");
                o.beginCommaMode();
                for( int i=0; i<t.countBaseInterfaces(); i++ ) {
                    o.comma();
                    String baseName;
                    try {
                        baseName = getTypeName(t.getBaseInterface(i));
                    } catch (BindingException e) {
                        e.addContext("interface "+typeName);
                        el.error(e);
                        baseName = "Com4jObject";
                    }
                    o.print(baseName);
                }
                if(hasEnum) {
                    o.comma();
                    o.print("Iterable<Com4jObject>");
                }
                o.endCommaMode();
            }
            o.println(" {");
            o.in();

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
                    Generator.this.generate(m,o);
                    o.commit();
                } catch( BindingException e ) {
                    o.cancel();
                    e.addContext("interface "+t.getName());
                    el.error(e);
                }
                m.dispose();
            }

            o.out();
            o.println("}");

            o.close();
        }

        private void generate( IDispInterfaceDecl t ) throws IOException {
            if(t.isDual())
                generate( t.getVtblInterface() );
            else {
                // TODO: how should we handle this?
            }
//        } catch( BindingException e ) {
//            throw new BindingException(
//                Messages.FAILED_TO_BIND.format(t.getName()),
//                e );
//        }
        }

        private void generateHeader(IndentingWriter o) {
            if(!lib.pkg.isRoot()) {
                o.printf("package %1s;",lib.pkg.name);
                o.println();
                o.println();
            }

            o.println("import com4j.*;");
            o.println();
        }
    }

    /**
     * Returns the primary interface for the given co-class.
     *
     * @return
     *      null if none is found.
     */
    private ITypeDecl getDefaultInterface( ICoClassDecl t ) {
        final int count = t.countImplementedInterfaces();
        // look for the default interface first.
        for( int i=0; i<count; i++ ) {
            IImplementedInterfaceDecl impl = t.getImplementedInterface(i);
            if(impl.isSource())
                continue;
            if(impl.isDefault())
                return impl.getType();
        }

        // if none is found, look for any non-source interface
        for( int i=0; i<count; i++ ) {
            IImplementedInterfaceDecl impl = t.getImplementedInterface(i);
            if(impl.isSource())
                continue;
            return impl.getType();
        }

        return null;
    }

    private void declareFactoryMethod(IndentingWriter o, ICoClassDecl t) {
        ITypeDecl p = getDefaultInterface(t);

        String primaryIntf = Com4jObject.class.getName(); // default interface name
        try {
            primaryIntf = getTypeName(p);
        } catch( BindingException e ) {
            e.addContext("class factory for coclass "+t.getName());
            el.error(e);
        }

        o.println();
        printJavadoc(t.getHelpString(),o);

        o.printf("public static %1s create%2s() {", primaryIntf, t.getName() );
        o.println();
        o.in();

        o.printf("return COM4J.createInstance( %1s.class, \"%2s\" );",
            primaryIntf, t.getGUID());
        o.println();

        o.out();
        o.println("}");
//
//
//        o.println(t.getHelpString());
//        o.println(t.getName());
//        int count = t.countImplementedInterfaces();
//        for( int j=0; j<count; j++ ) {
//            IImplementedInterfaceDecl impl = t.getImplementedInterface(j);
//            o.printf("%1s def:%2b src:%3b rst:%4b\n",
//                impl.getType().getName(),
//                impl.isDefault(),
//                impl.isSource(),
//                impl.isRestricted());
//            impl.dispose();
//        }
    }





    /**
     * Binds a native method to a Java method.
     */
    private class MethodBinder {
        private final IMethod method;
        private final IParam[] params;
        private int retParam;
        /**
         * The return type.
         *
         * "T" of "[out,retval]T* ..."
         */
        private final IType returnType;

        public MethodBinder(IMethod method) throws BindingException {
            this.method = method;

            int len = method.getParamCount();
            params = new IParam[len];
            for( int i=0; i<len; i++ )
                params[i] = method.getParam(i);

            retParam = getReturnParam();
            returnType = getReturnTypeBinding();
        }

        private IType getReturnTypeBinding() throws BindingException {
            if(retParam==-1)
                return null;
            IPtrType pt = params[retParam].getType().queryInterface(IPtrType.class);
            if(pt==null)
                throw new BindingException(Messages.RETVAL_MUST_BY_REFERENCE.format());
            return pt.getPointedAtType();
        }

        /**
         * Returns the index of the return value parameter,
         * or -1 if none.
         */
        private int getReturnParam() {
            // look for [retval] attribute
            for( int i=0; i<params.length; i++ ) {
                if(params[i].isRetval())
                    return i;
            }

            // sometimes a COM method has only one [out] param.
            // treat that as the return value.
            // this is seen frequently in MSHTML (see IHTMLChangeLog, for example)
            int outIdx=-1;
            for( int i=0; i<params.length; i++ ) {
                if(params[i].isOut() && !params[i].isIn()) {
                    if(outIdx==-1)
                        outIdx=i;
                    else
                        return -1;  // more than one out. no return value
                }
            }

            return outIdx;
        }

        public void generateDefaultInterfaceFacade( IndentingWriter o ) throws BindingException {
            IMethod m = method;
            List<IType> intermediates = new ArrayList<IType>();

            while(true) {
                MethodBinder mb = new MethodBinder(m);
                // only handle methods of the form "HRESULT foo([out,retval]IFoo** ppOut);
                if (m.getParamCount() != 1 || mb.retParam != 0 || mb.params[mb.retParam].isIn())
                    break;

                // we expect it to be an interface pointer.
                IPtrType pt = mb.returnType.queryInterface(IPtrType.class);
                IDispInterfaceDecl di = null;
                IInterfaceDecl ii = null;
                if (pt != null) {
                    IType t = pt.getPointedAtType();
                    di = t.queryInterface(IDispInterfaceDecl.class);
                    ii = t.queryInterface(IInterfaceDecl.class);
                }

                if (di == null && ii == null)
                    break;

                IInterface intf;
                if (ii != null) {
                    intf = ii;
                } else {
                    if(di.isDual())
                        intf = di.getVtblInterface();
                    else
                        break;
                }

                // does this target interface has a default method?
                IMethod dm = dmf.getDefaultMethod(intf);
                if (dm == null)
                    return;

                // recursively check...
                m = dm;
                intermediates.add(pt);
            }

            if(intermediates.isEmpty())
                return; // no default method to generate

            if(m.getParamCount()<2)
                return; // the default method has to have at least one in param and one ret val

            o.printf("@VTID(%1d)",
                method.getVtableIndex());
            o.println();

            MethodBinder mb = new MethodBinder(m);
            mb.declareReturnType(o,intermediates);
            this.declareMethodName(o);
            mb.declareParameters(o);
            o.println();
        }

        public void declare( IndentingWriter o ) throws BindingException {
            printJavadoc(method.getHelpString(), o);
//            o.println("// "+method.getKind());
            o.printf("@VTID(%1d)",
                method.getVtableIndex());
            o.println();

            int dispId = method.getDispId();
            if(dispId==0)
                o.println("@DefaultMethod");

            if(isEnum(method)) {
                // this is an enumerator. handle it differently.
                o.println("java.util.Iterator<Com4jObject> iterator();");
                return;
            }

            declareReturnType(o,null);
            declareMethodName(o);
            declareParameters(o);
        }

        private void declareMethodName(IndentingWriter o) {
            String name = escape(camelize(method.getName()));
            if(reservedMethods.contains(name))
                name += '_';
            o.print(name);
        }

        private void declareParameters(IndentingWriter o) throws BindingException {
            o.print('(');
            o.in();

            boolean first = true;
            // declare parameters
            for( IParam p : params ) {
                if( retParam!=-1 && p==params[retParam] && !p.isIn() )
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
            VariableBinding vb = bind(p.getType(),p.getName());
            if(!vb.isDefault) {
                o.printf("@MarshalAs(NativeType.%1s) ",vb.nativeType.name());
            }

            String javaType = vb.javaType;

            if(method.isVarArg() && p==params[params.length-1]) {
                // use varargs if applicable
                if( javaType.endsWith("[]") )
                    javaType = javaType.substring(0,javaType.length()-2)+"...";
            }
            o.print(javaType);
            o.print(' ');
            String name = p.getName();
            if(name==null)  name="rhs";
            o.print(escape(camelize(name)));
        }

        /**
         * Declares the return type.
         */
        private void declareReturnType(IndentingWriter o, List<IType> intermediates ) throws BindingException {
            if(retParam==-1 && intermediates==null) {
                o.print("void ");
            } else {
                // we assume that the [retval] param to be passed by reference
                VariableBinding retBinding = bind(returnType,null);

                // add @ReturnValue if necessary
                if(!retBinding.isDefault || params[retParam].isIn() || retParam!=params.length-1 || intermediates!=null) {
                    o.print("@ReturnValue(");
                    o.beginCommaMode();
                    if(!retBinding.isDefault) {
                        o.comma();
                        o.print("type=NativeType."+retBinding.nativeType.name());
                    }
                    if(params[retParam].isIn()) {
                        o.comma();
                        o.print("inout=true");
                    }
                    if(retParam!=params.length-1) {
                        o.comma();
                        o.print("index="+retParam);
                    }

                    if(intermediates!=null) {
                        o.comma();
                        o.print("defaultPropertyThrough={");
                        o.beginCommaMode();
                        for (IType im : intermediates) {
                            VariableBinding vb = bind(im, null);
                            o.comma();
                            o.print(vb.javaType);
                            o.print(".class");
                        }
                        o.endCommaMode();
                        o.print("}");
                    }

                    o.endCommaMode();
                    o.println(")");
                }

                o.print(retBinding.javaType);
                o.print(' ');
            }
        }
    }

    /**
     * All {@link Package}s keyed by their names.
     */
    private final Map<String,Package> packages = new HashMap<String, Package>();

    private Package getPackage(String name) {
        Package p = packages.get(name);
        if(p==null)
            packages.put(name,p=new Package(name));
        return p;
    }

    private final class Package {
        /**
         * Java package name of this type library.
         * Can be empty but never null.
         */
        final String name;

        /**
         * Short filenames that are generated into this package.
         * <p>
         * Used to detect collisions. The value is the type that
         * generated it.
         */
        private final Map<String,TypeLibInfo> fileNames = new HashMap<String,TypeLibInfo>();

        /**
         * Type libraries generated into this package.
         */
        final Set<TypeLibInfo> typeLibs = new HashSet<TypeLibInfo>();

        public Package(String name) {
            this.name = name;
        }

        private File getDir() {
            if(isRoot())
                return new File(".");
            else
                return new File(name.replace('.',File.separatorChar));
        }

        /**
         * True if this is the root package.
         */
        public boolean isRoot() {
            return name.equals("");
        }

        /**
         * Creates an {@link IndentingWriter} for a given file in this package.
         *
         * @param fileName
         *      such as "Foo.java"
         */
        public IndentingWriter createWriter(TypeLibInfo lib, String fileName) throws IOException {
            TypeLibInfo tli = fileNames.get(fileName);
            if(tli!=null)
                el.error(new BindingException(Messages.FILE_CONFLICT.format(
                    fileName, tli.lib.getName(), lib.lib.getName(), name )));
            else
                fileNames.put(fileName,lib);
            return writer.create(new File(getDir(),fileName));
        }

    }

    /**
     * Type library information.
     */
    private final Map<IWTypeLib,TypeLibInfo> typeLibs = new HashMap<IWTypeLib,TypeLibInfo>();

    /**
     * Information about a type library.
     *
     * <p>
     * Processing a type library often requires references to other
     * type libraries, and in particular how the type names in other
     * type libraries are bound in Java.
     *
     * <p>
     * An instance of this class is created for each type library
     * (including the one that we are binding.)
     */
    private final class TypeLibInfo {
        final IWTypeLib lib;

        /**
         * Java package of this type library.
         */
        final Package pkg;

        /**
         * Every top-level type declaration in this type library and their
         * Java type name.
         */
        private final Map<ITypeDecl,String> aliases = new HashMap<ITypeDecl,String>();

        public TypeLibInfo(IWTypeLib lib) throws BindingException {
            this.lib = lib;
            this.pkg = getPackage(referenceResolver.resolve(lib));
            this.pkg.typeLibs.add(this);

            buildSimpleAliasTable();
        }

        /**
         * MIDL often produces typedefs of the form "typedef A B" where
         * B is an enum declaration, and A is the name given by the user in IDL
         * (A is usually cryptic, B is human readable.)
         *
         * <p>
         * I don't know why MIDL behaves in this way, but in this case
         * it's usually desirable as a binding if we use A everywhere in place of B.
         *
         * <p>
         * We build this map B -> A to simply this.
         */
        private void buildSimpleAliasTable() {
            String prefix = pkg.name;
            if(prefix.length()==0)  prefix += '.';

            int len = lib.count();
            for( int i=0; i<len; i++ ) {
                ITypeDecl t = lib.getType(i);
                if(t.getKind()==TypeKind.ALIAS) {
                    ITypedefDecl typedef = t.queryInterface(ITypedefDecl.class);
                    ITypeDecl def = typedef.getDefinition().queryInterface(ITypeDecl.class);
                    if(def!=null) {
//                    System.out.println(def.getName()+" -> "+typedef.getName());
                        aliases.put( def, typedef.getName() );

                        if(def.getKind()==TypeKind.DISPATCH ) {
                            // if the alias is defined against dispinterface,
                            // also define the same typedef for 'interface'

                            IDispInterfaceDecl dispi = def.queryInterface(IDispInterfaceDecl.class);
                            if(dispi.isDual()) {
                                IInterfaceDecl custitf = dispi.getVtblInterface();
                                aliases.put(custitf, typedef.getName());
                            }
                        }
                    }
                }
                t.dispose();
            }
        }

        /**
         * Gets the simple name of the type (as opposed to FQCN.)
         */
        private String getTypeName(ITypeDecl decl) {
            assert decl.getParent().equals(lib);

            String name;
            if(aliases.containsKey(decl))
                name = aliases.get(decl);
            else
                name = decl.getName();
            if(STDOLE_TYPES.contains(name))
                return "Com4jObject";
            else
                return name;
        }

        public IndentingWriter createWriter(String fileName) throws IOException {
            return pkg.createWriter(this,fileName);
        }
    }

    // TODO: map IFont and IPicture correctly
    private static final Set<String> STDOLE_TYPES = new HashSet<String>(
        Arrays.asList("IUnknown","IDispatch","IFont","IPicture","IFontDisp","IPictureDisp"));


    /**
     * Gets the type name for the given declaration.
     * <p>
     * This takes the followings into account:
     *
     * <ul>
     *  <li>aliases (typedefs)
     *  <li>types in other type libs.
     * </ul>
     */
    private String getTypeName(ITypeDecl decl) throws BindingException {
        Generator.TypeLibInfo tli = getTypeLibInfo(decl.getParent());
        String name = tli.pkg.name;
        if(name.length()>0) name+='.';
        name += tli.getTypeName(decl);
        return name;
    }

    /**
     * Gets or creates a {@link TypeLibInfo} object for the given
     * type library.
     */
    private TypeLibInfo getTypeLibInfo(IWTypeLib p) throws BindingException {
        TypeLibInfo tli = typeLibs.get(p);
        if(tli==null) {
            typeLibs.put(p,tli=new TypeLibInfo(p));
        }
        return tli;
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

        public VariableBinding createByRef() {
            String t = javaType;
            if(boxTypeMap.containsKey(t))
                t = boxTypeMap.get(t);
            return new VariableBinding( "Holder<"+t+">", nativeType.byRef(), isDefault );
        }

        private static final Map<String,String> boxTypeMap = new HashMap<String,String>();

        static {
            boxTypeMap.put("byte","Byte");
            boxTypeMap.put("short","Short");
            boxTypeMap.put("int","Integer");
            boxTypeMap.put("long","Long");
            boxTypeMap.put("float","Float");
            boxTypeMap.put("double","Double");
            boxTypeMap.put("boolean","Boolean");
            boxTypeMap.put("char","Character");
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
        pbind( VarType.VT_LPSTR, String.class, NativeType.CSTR, false );
        pbind( VarType.VT_LPWSTR, String.class, NativeType.Unicode, false );
        // TODO: is it OK to map UI2 to short?
        pbind( VarType.VT_UI1, Byte.TYPE, NativeType.Int8, true );
        pbind( VarType.VT_UI2, Short.TYPE, NativeType.Int16, true );
        pbind( VarType.VT_UI4, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_INT, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_UINT, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_BOOL, Boolean.TYPE, NativeType.VariantBool, true );
        pbind( VarType.VT_R4, Float.TYPE, NativeType.Float, true );
        pbind( VarType.VT_R8, Double.TYPE, NativeType.Double, true );
        pbind( VarType.VT_VARIANT, Object.class, NativeType.VARIANT, false );
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

    private static boolean isPsz( String hint ) {
        if(hint==null)  return false;
        return hint.startsWith("psz") || hint.startsWith("lpsz");
    }

    /**
     * Binds the native type to a Java type and its conversion.
     *
     * @param nameHint
     *      Optional parameter name of the type. If non-null,
     *      this is used to disambiguate common mistake
     *      like declaring LPWSTR as "ushort*", etc.
     */
    private VariableBinding bind( IType t, String nameHint ) throws BindingException {
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
            IInterfaceDecl compDecl = comp.queryInterface(IInterfaceDecl.class);
            if( compDecl!=null ) {
                // t = T* where T is a declared interface
                return new VariableBinding( getTypeName(compDecl), NativeType.ComObject, true );
            }

            IDispInterfaceDecl dispDecl = comp.queryInterface(IDispInterfaceDecl.class);
            if( dispDecl!=null ) {
                // t = T* where T is a declared interface
                return new VariableBinding( getTypeName(dispDecl), NativeType.ComObject,  true );
            }

            // t = coclass*
            ICoClassDecl classdecl = comp.queryInterface(ICoClassDecl.class);
            if(classdecl!=null) {
                // bind to its default interface
                ITypeDecl di = getDefaultInterface(classdecl);
                if(di==null)
                    // no primary interface known. treat it as IUnknown
                    return new VariableBinding(Com4jObject.class, NativeType.ComObject, true);
                else
                    return new VariableBinding( getTypeName(di), NativeType.ComObject, true );
            }

            IPrimitiveType compPrim = comp.queryInterface(IPrimitiveType.class);
            if( compPrim!=null ) {
                if( compPrim.getVarType()==VarType.VT_VARIANT ) {
                    // T = VARIANT*
                    return new VariableBinding(Object.class, NativeType.VARIANT_ByRef, true);
                }
                if( compPrim.getVarType()==VarType.VT_VOID ) {
                    // T = void*
                    return new VariableBinding(Buffer.class,  NativeType.PVOID, true );
                }
                if( compPrim.getVarType()==VarType.VT_UI2 ) {
                    // T = ushort*
                    if( isPsz(nameHint) )
                        // this is a common mistake
                        return new VariableBinding( String.class, NativeType.Unicode, false );
                }
                if( compPrim.getVarType()==VarType.VT_BOOL ) {
                    return new VariableBinding("Holder<Boolean>", NativeType.VariantBool_ByRef, true );
                }
            }

            // a few other random checks
            String name = getTypeString(ptrt);
            if( name.equals("_RemotableHandle*") ) {
                // marshal as the raw pointer value
                return new VariableBinding( Integer.TYPE, NativeType.Int32,  true );
            }
            if(name.equals("GUID*")) {
                return new VariableBinding( "GUID", NativeType.GUID, true );
            }

            // otherwise use a holder
            VariableBinding b = bind(comp,null);
            if(b!=null && b.nativeType.byRef()!=null )
                return b.createByRef();
        }

        ISafeArrayType at = t.queryInterface(ISafeArrayType.class);
        if(at!=null) {
            // T=SAFEARRAY(...)
            IType comp = at.getComponentType();

            IPrimitiveType compPrim = comp.queryInterface(IPrimitiveType.class);
            if( compPrim!=null ) {
                VariableBinding r = primitiveTypeBindings.get(compPrim.getVarType());
                if(r!=null) {
                    return new VariableBinding(r.javaType+"[]", NativeType.SafeArray, true );
                }
            }
        }

        // T = typedef
        ITypedefDecl typedef = t.queryInterface(ITypedefDecl.class);
        if(typedef!=null) {
            return bind(typedef.getDefinition(),nameHint);
        }

        // T = enum
        IEnumDecl enumdef = t.queryInterface(IEnumDecl.class);
        if(enumdef!=null) {
            // TODO: we should probably check the size of the enum.
            // there's no guarantee it's 32 bit.
            return new VariableBinding( getTypeName(enumdef), NativeType.Int32, true );
        }

        // a few other random checks
        String name = getTypeString(t);
        if(name.equals("GUID")) {
            return new VariableBinding( "GUID", NativeType.GUID, true );
        }


        ITypeDecl declt = t.queryInterface(ITypeDecl.class);
        if(declt!=null) {
            // TODO: not clear how we should handle this
            throw new BindingException(Messages.UNSUPPORTED_TYPE.format(getTypeString(t)));
        }

        throw new BindingException(Messages.UNSUPPORTED_TYPE.format(getTypeString(t)));
    }


    private void generate(IMethod m, IndentingWriter o) throws BindingException {
        try {
            MethodBinder mb = new MethodBinder(m);
            mb.declare(o);
            o.println();

            mb.generateDefaultInterfaceFacade(o);
        } catch (BindingException e) {
            e.addContext("method "+m.getName());
            throw e;
        }
    }

    private void printJavadoc(String doc, PrintWriter o) {
        if(doc!=null) {
            o.println("/**");
            o.println(" * "+doc);
            o.println(" */");
        }
    }

    /**
     * Returns a human-readable identifier of the type,
     * but it's not necessarily a correct Java id.
     *
     * This is mainly for debugging.
     */
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
            return decl.getName();

        return "N/A";
    }

    private static String camelize(String s) {
        int idx = 0;

        while(idx<s.length() && Character.isUpperCase(s.charAt(idx)))
            idx++;

        if(idx==s.length())
            return s.toLowerCase();
        if(idx>0) {
            if(idx==1)  idx=2;
            // s=="HTMLProject" then idx==5
            return s.substring(0,idx-1).toLowerCase()+s.substring(idx-1);
        }

        return s;
    }

    private static String escape(String s) {
        return Escape.escape(s);
    }

    private static final Set<String> reservedMethods = new HashSet<String>();

    static {
        reservedMethods.add("equals");
        reservedMethods.add("getClass");
        reservedMethods.add("hashCode");
        reservedMethods.add("notify");
        reservedMethods.add("notifyAll");
        reservedMethods.add("toString");
        reservedMethods.add("wait");
    }

    private static boolean isEnum(IMethod m) {
        return m.getName().equals("_NewEnum");
    }
}
