package com4j.tlbimp;

import com4j.ComException;
import com4j.MarshalAs;
import com4j.Variant;
import com4j.tlbimp.def.IMethod;
import com4j.tlbimp.def.IParam;
import com4j.tlbimp.def.IPrimitiveType;
import com4j.tlbimp.def.IPtrType;
import com4j.tlbimp.def.IType;
import com4j.tlbimp.def.InvokeKind;
import com4j.tlbimp.def.VarType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Binds a native method to a Java method.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
abstract class MethodBinder {
    protected final IMethod method;
    protected final IParam[] params;

    /**
     * If the return value is a part of the parameter
     * (such as in the VTBL binding), this value points to that index.
     * Otherwise -1.
     *
     * <p>
     * For disp interfaces, this value is always -1,
     * since return type shows up in the "return type" part.
     */
    protected final int retParam;

    /**
     * The return type.
     *
     * <p>
     * "T" of "[out,retval]T* ..."
     *
     * null if there's no return type.
     */
    protected final IType returnType;

    protected final Generator g;


    protected MethodBinder(Generator g, IMethod method) throws BindingException {
        this.g = g;
        this.method = method;

        int len = method.getParamCount();
        params = new IParam[len];
        for( int i=0; i<len; i++ )
            params[i] = method.getParam(i);

        retParam = getReturnParam();
        returnType = getReturnTypeBinding();
    }

    protected IType getReturnTypeBinding() throws BindingException {
        if(retParam==-1){
            return null;
        }
        IPtrType pt = params[retParam].getType().queryInterface(IPtrType.class);
        if(pt==null){
            throw new BindingException(Messages.RETVAL_MUST_BY_REFERENCE.format());
        }
        return pt.getPointedAtType();
    }

    /**
     * Returns the index of the return value parameter,
     * or -1 if none.
     */
    private int getReturnParam() {
        // look for [retval] attribute
        for( int i=0; i<params.length; i++ ) {
            if(params[i].isRetval()){
                return i;
            }
        }

        // sometimes a COM method has only one [out] param.
        // treat that as the return value.
        // this is seen frequently in MSHTML (see IHTMLChangeLog, for example)
        int outIdx=-1;
        for( int i=0; i<params.length; i++ ) {
            if(params[i].isOut() && !params[i].isIn()) {
                if(outIdx==-1) {
                    outIdx=i;
                } else {
                    return -1;  // more than one out. no return value
                }
            }
        }
        return outIdx;
    }


    public void declare( IndentingWriter o ) throws BindingException {
        o.printJavadoc(method.getHelpString());
//            o.println("// "+method.getKind());

        annotate(o);

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

    /**
     * A chance to generate annotations on the method.
     */
    protected abstract void annotate(IndentingWriter o);

    protected final void declareMethodName(IndentingWriter o) {
        String methodName = method.getName();
        if(g.renameGetterAndSetters){
          String methodStart = methodName.length() > 3 ? methodName.substring(0, 3) : "";
          switch(method.getKind()){
            case PROPERTYGET:
              if (!methodStart.equalsIgnoreCase("get")) {
                methodName = "get" + methodName.substring(0, 1).toUpperCase(g.locale) + methodName.substring(1);
              }
              break;
            case PROPERTYPUT:
            case PROPERTYPUTREF:
              if (methodStart.equalsIgnoreCase("put")) {
                methodName = "set" + methodName.substring(3);
              } else if (!methodStart.equalsIgnoreCase("set")) {
                methodName = "set" + methodName.substring(0, 1).toUpperCase(g.locale) + methodName.substring(1);
              }
              break;
          }
        }
        String name = escape(camelize(methodName));
        if(reservedMethods.contains(name))
            name += '_';
        o.print(name);
    }

    protected final void declareParameters(IndentingWriter o) throws BindingException {
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

        o.print(")");
        terminate(o);
    }

    /**
     * Terminates the method.
     */
    protected abstract void terminate(IndentingWriter o);

    /**
     * Declares a parameter.
     */
    private void declare( IParam p, IndentingWriter o ) throws BindingException {
        TypeBinding vb = TypeBinding.bind(g,p.getType(),p.getName());
        if(!vb.isDefault && needsMarshalAs()) {
            o.printf("@MarshalAs(NativeType.%1s) ",vb.nativeType.name());
        }

        String javaType = vb.javaType;

        if(method.isVarArg() && p==params[params.length-1]) {
            // use varargs if applicable
            if( javaType.endsWith("[]") )
                javaType = javaType.substring(0,javaType.length()-2)+"...";
        }
        Variant defValue = p.getDefaultValue();
        if(defValue!=null)
            try {
                o.print("@DefaultValue(\""+ defValue.stringValue()+"\")");
            } catch (ComException e) {
                // in rare occasions we get default values that are not printable.
                // ignore such an error.
            }
        o.print(javaType);
        o.print(' ');
        String name = p.getName();
        if(name==null)  name="rhs";
        o.print(escape(camelize(name)));
    }

    /**
     * Returns true if this method generator needs to generate
     * {@link MarshalAs}. This is only necessary for VTBL method.
     */
    protected boolean needsMarshalAs() {
        return false;
    }

    /**
     * Declares the return type.
     */
    protected final void declareReturnType(IndentingWriter o, List<IType> intermediates ) throws BindingException {
        generateAccessModifier(o);

        if(returnType==null && intermediates==null) {
            o.print("void ");
        } else {
            // we assume that the [retval] param to be passed by reference
            TypeBinding retBinding = TypeBinding.bind(g,returnType,null);

            // add @ReturnValue if necessary
            if((!retBinding.isDefault && needsMarshalAs()) || (retParam!=-1 && (params[retParam].isIn() || retParam!=params.length-1)) || intermediates!=null) {
                o.print("@ReturnValue(");
                o.beginCommaMode();
                if(!retBinding.isDefault && needsMarshalAs()) {
                    o.comma();
                    o.print("type=NativeType."+retBinding.nativeType.name());
                }
                if(retParam!=-1 && params[retParam].isIn()) {
                    o.comma();
                    o.print("inout=true");
                }
                if(retParam!=-1 && retParam!=params.length-1) {
                    o.comma();
                    o.print("index="+retParam);
                }

                if(intermediates!=null) {
                    o.comma();
                    o.print("defaultPropertyThrough={");
                    o.beginCommaMode();
                    for (IType im : intermediates) {
                        TypeBinding vb = TypeBinding.bind(g,im, null);
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

    /**
     * Chance to generate access modifiers.
     */
    protected void generateAccessModifier(IndentingWriter o) {
    }

    private static String escape(String s) {
        return Escape.escape(s);
    }

    private String camelize(String s) {
        int idx = 0;

        while(idx<s.length() && Character.isUpperCase(s.charAt(idx)))
            idx++;

        if(idx==s.length())
            return s.toLowerCase(g.locale);
        if(idx>0) {
            if(idx==1)  idx=2;
            // s=="HTMLProject" then idx==5
            return s.substring(0,idx-1).toLowerCase(g.locale)+s.substring(idx-1);
        }

        return s;
    }

    static boolean isEnum(IMethod m) {
        return m.getName().equals("_NewEnum");
    }

    /**
     * Computes the return type for disp-only interface.
     */
    protected final IType getDispInterfaceReturnType() {
        IType r = method.getReturnType();

        // if the return type is HRESULT, bind it to 'void'.
        // dispinterfaces defined by C++ often uses HRESULT
        // as the return value
        IPrimitiveType pt = r.queryInterface(IPrimitiveType.class);
        if(pt!=null && pt.getVarType()== VarType.VT_HRESULT)
            return null;

        return r;
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
}
