package com4j.tlbimp;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com4j.ComException;
import com4j.GUID;
import com4j.MarshalAs;
import com4j.NativeType;
import com4j.Variant;
import com4j.tlbimp.def.IMethod;
import com4j.tlbimp.def.IParam;
import com4j.tlbimp.def.IPrimitiveType;
import com4j.tlbimp.def.IPtrType;
import com4j.tlbimp.def.IType;
import com4j.tlbimp.def.InvokeKind;
import com4j.tlbimp.def.VarType;

import static java.util.Arrays.asList;

/**
 * Binds a native method to a Java method.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
abstract class MethodBinder
{
  protected final IMethod method;
  protected final IParam[] params;

  /**
   * If the return value is a part of the parameter (such as in the VTBL binding), this value points to that index. Otherwise -1.
   * <p>
   * For disp interfaces, this value is always -1, since return type shows up in the "return type" part.
   */
  protected final int retParam;

  /**
   * The return type.
   * <p>
   * "T" of "[out,retval]T* ..." null if there's no return type.
   */
  protected final IType returnType;

  protected final Generator g;

  protected MethodBinder(Generator g, IMethod method) throws BindingException {
    this.g = g;
    this.method = method;

    int len = method.getParamCount();
    params = new IParam[len];
    for (int i = 0; i < len; i++)
      params[i] = method.getParam(i);

    retParam = getReturnParam();
    returnType = getReturnTypeBinding();
  }

  protected IType getReturnTypeBinding() throws BindingException {
    if (retParam == -1) {
      return null;
    }
    IParam p = params[retParam];
    IType t = p.getType();
    if (t == null) {
      throw new BindingException(Messages.UNSUPPORTED_TYPE.format());
    }
    IPtrType pt = t.queryInterface(IPtrType.class);
    if (pt == null) {
      throw new BindingException(Messages.RETVAL_MUST_BY_REFERENCE.format());
    }
    return pt.getPointedAtType();
  }

  /**
   * Returns the index of the return value parameter, or -1 if none.
   */
  private int getReturnParam() {
    // look for [retval] attribute
        for (int i = 0; i < params.length; i++) {
      if (params[i].isRetval()) {
        return i;
      }
    }

    // sometimes a COM method has only one [out] param.
    // treat that as the return value.
    // this is seen frequently in MSHTML (see IHTMLChangeLog, for example)
    int outIdx = -1;
    for (int i = 0; i < params.length; i++) {
      if (params[i].isOut() && !params[i].isIn()) {
        if (outIdx == -1) {
          outIdx = i;
        } else {
          return -1; // more than one out. no return value
        }
      }
    }
    return outIdx;
  }

    public void declare(IndentingWriter o) throws BindingException {
        Parameter[] defaultParams = generateDefaults();
        boolean[] useDefault = new boolean[defaultParams.length];
        for (int i = 0; i < useDefault.length; i++) {
            useDefault[i] = defaultParams[i] != null;
        }

        for (int pos = 0; g.generateDefaultMethodOverloads && pos < useDefault.length; pos++) {
            // search the next optional parameter
            if (!useDefault[pos])   continue;

            // if we have default parameters left, then declare with there default value
            declareWithDefaults(o, defaultParams, useDefault);
            useDefault[pos] = false;
        }
        
        declareWithDefaults(o, defaultParams, null);
    }

  class Parameter {
    String javaTypeName;
    NativeType nativeType;
    Variant.Type variantType;
    String literal;
    String javaCode;

      /**
       * {@link #javaTypeName} can be a {@link Type}, not just {@link Class} (for example, {@code Holder&lt;Something>},
       * so here we try to convert it to a Class object.
       *
       * <p>
       * It's not clear to me if this even makes sense --- this loss of information might kill the runtime.
       * But at least it gets the compiler going.
       *
       * TODO: verify that the runtime works with this loss of information. If it doesn't,
       * tlbimp shouldn't generate such a signature.
       */
      public String toClassName() {
          int idx = javaTypeName.indexOf('<');
          if (idx>=0)   return javaTypeName.substring(0,idx);
          else  return javaTypeName;
      }
  }

  private Parameter[] generateDefaults(){
    Parameter[] defParam = new Parameter[params.length];
    for (int i = 0; i < params.length; i++) {
      if(params[i].isOptional() || (params[i].isLCID() && g.defaultLcid != null)){
        TypeBinding vb;
        try {
          vb = TypeBinding.bind(g, params[i].getType(), params[i].getName());
        }catch (BindingException e){
          g.el.error(e);
          continue;
        }
        defParam[i] = new Parameter();
        Variant defValue = params[i].getDefaultValue();
        if (defValue == null && params[i].isLCID() && null != g.defaultLcid) {
          defValue = new Variant(Variant.Type.VT_I4);
          defValue.set(g.defaultLcid);
        }
        defParam[i].nativeType = vb.nativeType;
        defParam[i].javaTypeName = vb.javaType;
        if(defValue != null) {
          defParam[i].variantType = defValue.getType();
          defParam[i].literal = defValue.getParseableString(); //FIXME: this might fail! try-catch!
          defParam[i].javaCode = defValue.getJavaCode();
        } else {
          defValue = Variant.getMissing();
          switch(vb.nativeType){
            case Bool:
            case VariantBool:
            case VariantBool_ByRef:
              defParam[i].literal = "false";
              defParam[i].javaCode = defParam[i].literal;
              break;
            case BSTR:
            case BSTR_ByRef:
            case CSTR:
            case Unicode:
              defParam[i].literal = "";
              defParam[i].javaCode = "\"\"";
              break;
            case Int8:
            case Int8_ByRef:
              defParam[i].literal = "0";
              defParam[i].javaCode = "(byte) " +defParam[i].literal;
              break;
            case Int16:
            case Int16_ByRef:
              defParam[i].literal = "0";
              defParam[i].javaCode = "(short) " +defParam[i].literal;
              break;
            case Int32:
            case Int32_ByRef:
              defParam[i].literal = "0";
              defParam[i].javaCode = defParam[i].literal;
              break;
            case Int64:
            case Int64_ByRef:
              defParam[i].literal = "0";
              defParam[i].javaCode = defParam[i].literal +"L";
              break;
            case Float: // Float and Double parse "0" just fine.
            case Float_ByRef:
              defParam[i].literal = "0";
              defParam[i].javaCode = defParam[i].literal +".0f";
              break;
            case Double:
            case Double_ByRef:
              defParam[i].literal = "0.0";
              defParam[i].javaCode = defParam[i].literal;
              break;
            case GUID:
              defParam[i].literal = GUID.GUID_NULL.toString();
              defParam[i].javaCode = defParam[i].literal;
              break;
            case VARIANT:
            case VARIANT_ByRef:
              defParam[i].literal = defValue.getParseableString();
              defParam[i].javaCode = defValue.getJavaCode();
              break;
              // TODO: add the missing types
            default:
              defParam[i].literal = null; // TODO..?
              defParam[i].javaCode = defParam[i].literal;
              break;
          }
          if(vb.nativeType == NativeType.VARIANT){
            defParam[i].variantType = defValue.getType(); // this is Variant.Type.VT_ERROR (Variant.MISSING)
          } else {
            // we do not need this variant type (since it is not a variant...)
            defParam[i].variantType = Variant.Type.NO_TYPE;
          }
        }
      }
    }
    return defParam;
  }

  private void declareJavaDoc(IndentingWriter o, Parameter[] defaultParam, boolean[] useDefault){
    // JavaDoc
    o.beginJavaDocMode();
    String helpString = method.getHelpString();
    if(helpString != null){
      o.println("<p>");
      o.println(helpString);
      o.println("</p>");
    }
    if(method.getKind() == InvokeKind.PROPERTYGET){
      o.println("<p>");
      o.println("Getter method for the COM property \"" + method.getName() + "\"");
      o.println("</p>");
    }
    if(method.getKind() == InvokeKind.PROPERTYPUT || method.getKind() == InvokeKind.PROPERTYPUTREF){
      o.println("<p>");
      o.println("Setter method for the COM property \""+method.getName()+"\"");
      o.println("</p>");
    }
    if(useDefault != null){
      o.println("<p>");
      o.println("This method uses predefined default values for the following parameters:");
      o.println("</p>");
      o.println("<ul>");
      o.in();
      for(int i = 0; i < defaultParam.length; i++){
        if(useDefault[i]){
          o.print("<li>"+ defaultParam[i].javaTypeName +" parameter ");
          declareParamName(o, params[i]);
          o.print(" is set to " + defaultParam[i].javaCode + "</li>");
        }
      }
      o.out();
      o.println("</ul>");
      o.println("<p>");
      o.println("Therefore, using this method is equivalent to");
      o.println("<code>");
      declareMethodName(o);
      o.print("(");
      boolean first = true;
      for(int i = 0; i < defaultParam.length; i++){
        if(i == retParam){
          continue;
        }
        if(!first){
          o.print(", ");
        } else {
          first = false;
        }
        if(useDefault[i]){
          o.print(defaultParam[i].javaCode);
        } else {
          declareParamName(o, params[i]);
        }
      }
      o.println(");");
      o.println("</code>");
      o.println("</p>");
    }
    for(int i = 0; i < params.length; i++){
      IParam p = params[i];
      if ((retParam != -1 && p == params[retParam] && !p.isIn()) || useDefault != null && useDefault[i])
        continue; // skip, cause it's showing up as the return value or is set up to use a default value
      o.print("@param ");
      declareParamName(o, p);
      if (p.isOptional()) {
        o.print(" Optional parameter.");
        if (defaultParam[i].javaCode != null) {
          o.print(" Default value is " + defaultParam[i].javaCode);
        } else {
          o.print(" Default value is unprintable.");
        }
      } else {
        TypeBinding tb;
        try {
          tb = TypeBinding.bind(g, params[i].getType(), params[i].getName());
          o.print(" Mandatory "+tb.javaType+" parameter.");
        } catch (BindingException e) {
          o.print(" Mandatory parameter.");
        }
      }
      o.println();
    }
    if(getReturnParam() >= 0 && !isEnum(method)){
      TypeBinding tb;
      o.print("@return ");
      try {
        tb = TypeBinding.bind(g, getReturnTypeBinding(), null);
        o.println(" Returns a value of type "+tb.javaType);
      } catch (BindingException e) {
        o.println();
      }
    }
    o.endJavaDocMode();
    o.println();
  }

  private void declareWithDefaults(IndentingWriter o, Parameter[] defaultParam, boolean[] useDefault) throws BindingException {

        declareJavaDoc(o, defaultParam, useDefault);

        annotate(o);

        int dispId = method.getDispId();
        if(dispId==0)
            o.println("@DefaultMethod");

        // generate UseDefaultValues annotation string
        if (useDefault != null) {
          String paramIndexMappings = "paramIndexMapping = {";
          String optParamIndices = "optParamIndex = {";
          String javaTypes = "javaType = {";
          String nativeTypes = "nativeType = {";
          String variantTypes = "variantType = {";
          String literals = "literal = {";

          boolean firstDefault = true, firstJavaParam = true;
          for (int i = 0; i < defaultParam.length; i++) {
            if(useDefault[i]){
              if (!firstDefault) {
                optParamIndices += ", ";
                javaTypes += ", ";
                nativeTypes += ", ";
                variantTypes += ", ";
                literals += ", ";
              } else {
                firstDefault = false;
              }
              optParamIndices += i;
              javaTypes += defaultParam[i].toClassName()+".class";
              nativeTypes += "NativeType." + defaultParam[i].nativeType.name();
              variantTypes += "Variant.Type." + defaultParam[i].variantType.name();
              literals += '"'+defaultParam[i].literal+'"';
            } else {
              if(!firstJavaParam){
                paramIndexMappings += ", ";
              } else {
                firstJavaParam = false;
              }
              paramIndexMappings += i;
            }
          }
          paramIndexMappings += "}";
          optParamIndices += "}";
          javaTypes += "}";
          nativeTypes += "}";
          variantTypes += "}";
          literals += "}";

          String defaultsAnnotation = "@UseDefaultValues("
                  +join(asList(paramIndexMappings, optParamIndices, javaTypes, nativeTypes, variantTypes, literals),", ")+')';

          o.println(defaultsAnnotation);
        }

        if(isEnum(method)) {
            // this is an enumerator. handle it differently.
            o.println("java.util.Iterator<Com4jObject> iterator();");
            return;
        }

        declareReturnType(o,null, useDefault!= null);
        declareMethodName(o);
        declareParameters(o, useDefault);
        o.println();
    }

    // TODO: what's the better place for this?
    private static String join(Collection<?> args, String delim) {
        StringBuilder buf = new StringBuilder();
        for (Object arg : args) {
            if (buf.length()>0) buf.append(delim);
            buf.append(arg);
        }
        return buf.toString();
    }

  /**
   * A chance to generate annotations on the method.
   */
  protected void annotate(IndentingWriter o) {

  }

  protected final void declareMethodName(IndentingWriter o) {
    String methodName = method.getName();
    if (g.renameGetterAndSetters) {
      String methodStart = methodName.length() > 3 ? methodName.substring(0, 3) : "";
      switch (method.getKind()) {
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
    if (reservedMethods.contains(name))
      name += '_';
    o.print(name);
  }

  protected final void declareParameters(IndentingWriter o, boolean[] useDefaults) throws BindingException {
    o.print('(');
    o.in();

    boolean first = true;
    // declare parameters
    for (int i = 0; i < params.length; i++){
      if(useDefaults != null && useDefaults[i]){
        // omit this parameter, since it is set up to use a default value.
        continue;
      }
      IParam p = params[i];
      if (retParam != -1 && p == params[retParam] && !p.isIn())
        continue; // skip, cause it's showing up as the return value
      if (!first)
        o.println(',');
      else
        o.println();
      first = false;
      declare(o, p);
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
  private void declare(IndentingWriter o, IParam p) throws BindingException {
    TypeBinding vb = TypeBinding.bind(g, p.getType(), p.getName());

    String javaType = vb.javaType;

    if (method.isVarArg() && p == params[params.length - 1]) {
      // use varargs if applicable
      if (javaType.endsWith("[]"))
        javaType = javaType.substring(0, javaType.length() - 2) + "...";
    }
    if (p.isOptional()) {
      o.print("@Optional ");
    }
    Variant defValue = p.getDefaultValue();
    if (defValue != null) {
      try {
        o.print("@DefaultValue(\"" + defValue.stringValue() + "\") ");
      } catch (ComException e) {
        // in rare occasions we get default values that are not printable.
        // ignore such an error.
      }
    }
    if(p.isLCID()){
      o.print("@LCID ");
    }
    if (!vb.isDefault && needsMarshalAs()) {
      o.printf("@MarshalAs(NativeType.%1s) ", vb.nativeType.name());
    }

    o.print(javaType);
    o.print(' ');
    declareParamName(o, p);

  }

  protected void declareParamName(IndentingWriter o, IParam p){
    String name = p.getName();
    if (name == null)
      name = "rhs";
    o.print(escape(camelize(name)));
  }

  /**
   * Returns true if this method generator needs to generate {@link MarshalAs}. This is only necessary for VTBL method.
   */
  protected boolean needsMarshalAs() {
    return false;
  }

  /**
   * Declares the return type.
   */
  protected final void declareReturnType(IndentingWriter o, List<IType> intermediates, boolean usesDefaltValues) throws BindingException {
    generateAccessModifier(o);

    if (returnType == null && intermediates == null) {
      o.print("void ");
    } else {
      // we assume that the [retval] param to be passed by reference
      TypeBinding retBinding = TypeBinding.bind(g, returnType, null);

      // add @ReturnValue if necessary
      if ((!retBinding.isDefault && needsMarshalAs()) || (retParam != -1 && (params[retParam].isIn() || retParam != params.length - 1))
          || intermediates != null
          || usesDefaltValues) {
        o.print("@ReturnValue(");
        o.beginCommaMode();
        if (!retBinding.isDefault && needsMarshalAs()) {
          o.comma();
          o.print("type=NativeType." + retBinding.nativeType.name());
        }
        if (retParam != -1 && params[retParam].isIn()) {
          o.comma();
          o.print("inout=true");
        }
        if (retParam != -1 && retParam != params.length - 1 || usesDefaltValues) {
          o.comma();
          o.print("index=" + retParam);
        }

        if (intermediates != null) {
          o.comma();
          o.print("defaultPropertyThrough={");
          o.beginCommaMode();
          for (IType im : intermediates) {
            TypeBinding vb = TypeBinding.bind(g, im, null);
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

    while (idx < s.length() && Character.isUpperCase(s.charAt(idx)))
      idx++;

    if (idx == s.length())
      return s.toLowerCase(g.locale);
    if (idx > 0) {
      if (idx == 1)
        idx = 2;
      // s=="HTMLProject" then idx==5
      return s.substring(0, idx - 1).toLowerCase(g.locale) + s.substring(idx - 1);
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
    if (pt != null && pt.getVarType() == VarType.VT_HRESULT) {
      // TODO: This causes a null pointer exception in some situations!
      return null;
    }

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
