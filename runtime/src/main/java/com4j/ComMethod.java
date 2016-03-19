package com4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.Buffer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;



/**
 * Internal abstraction that represents a COM method invocation.
 *
 * <p>
 * Instances hide the details of how to invoke a COM method.
 * (or a series of them.)
 *
 * @author Kohsuke Kawaguchi
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
abstract class ComMethod {

    /** The Java method that corresponds to the native COM method */
    final protected Method method;

    /**
     * The array of default parameters of the COM methods
     * @see #defaultParameterIndex
     */
    final protected Object[] defaultParameters;

    /**
     * The parameter indexes of default parameters. Each position in this array corresponds to a position in {@link #defaultParameters}
     *
     * <p>
     *  <h3>Example</h3>
     *  Let's assume the following values:
     *  <pre>
     *  defaultParameterIndex = {2, 4};
     *  defaultParameters = {new Integer(3), "Hello"};
     *  </pre>
     *  In this example the second parameter of the {@link ComMethod} has a default value of <code>new Integer(3)</code> and the
     *  forth parameter has a default value of "Hello".
     * </p>
     * @see #defaultParameters
     */
    final protected int[] defaultParameterIndex;

    /** Array of NativeTypes that describe the parameters of the method */
    final NativeType[] params;
    /** The parameter conversion codes passed to the native part of the invoke method */
    final int[] paramConvs;
    /** The index in the parameters of the return value ([retval, out] value) */
    final int returnIndex;
    /** True, if the [retval] parameters is [in, out] */
    final boolean returnIsInOut;
    /** The value conversion code for the return value */
    final NativeType returnConv;
    /** The Class objects of the parameters */
    final Class<?>[] paramTypes;
    /** The Type objects of the generic parameters */
    final Type[] genericParamTypes;

    /**
     * Constructs a new ComMethod for the given {@link Method}
     * @param method The Method designed to be a ComMethod (annotations)
     */
    public ComMethod(Method method){
      this.method = method;

      //generate default values
      UseDefaultValues defValues = method.getAnnotation(UseDefaultValues.class);
      if(defValues != null){
        defaultParameters = new Object[defValues.optParamIndex().length];
        defaultParameterIndex = defValues.optParamIndex();
        generateDefaultParameters(defValues);
      }else{
        defaultParameters = new Object[0];
        defaultParameterIndex = new int[0];
      }


      MethodIntrospector mi = new MethodIntrospector(method);
      Annotation[][] pa = method.getParameterAnnotations();
      int paramLen = pa.length;

      // retrieve information about the return value
      ReturnValue rt = method.getAnnotation(ReturnValue.class);
      if(rt!=null) {
          if(rt.index()==-1)  returnIndex=pa.length;
          else                returnIndex=rt.index();
          returnIsInOut = rt.inout();
          if(rt.type() == NativeType.Default){
            returnConv = getDefaultConversion(method.getReturnType());
          } else {
            returnConv = rt.type();
          }
      } else {
          // guess the default
          if( method.getReturnType()==Void.TYPE ) {
              // no return type
              returnIndex = -1;
              returnIsInOut = false;
              returnConv = NativeType.Default;    // unused
          } else {
              returnIndex = paramLen;
              returnIsInOut = false;
              returnConv = getDefaultConversion(method.getReturnType());
          }
      }

      // retrieve information about the parameters
      Class<?>[] paramTypesLocal = method.getParameterTypes();
      Type[] genericParamTypesLocal = method.getGenericParameterTypes();

      // Optional parameter
      int defValsCount = 0;
      if(defValues != null){
        defValsCount = defValues.optParamIndex().length;
      }
      params = new NativeType[paramLen + defValsCount];
      paramConvs = new int[paramLen + defValsCount];
      paramTypes = new Class<?>[paramLen + defValsCount];
      genericParamTypes = new Type[paramLen + defValsCount];

      // the "user visalbe" parameters of the Java interface
      for( int i=0; i<paramLen; i++ ) {
        int destPos = i;
        if(defValues != null){
          // remap the parameter.
          destPos = defValues.paramIndexMapping()[i];
        }
          NativeType n = mi.getParamConversation(i);
          params[destPos] = n;
          paramConvs[destPos] = n.code;
          paramTypes[destPos] = paramTypesLocal[i];
          genericParamTypes[destPos] = genericParamTypesLocal[i];
      }

      // add the default/optional parameters
      for(int i = 0; i < defValsCount; i++){
        int ind = defValues.optParamIndex()[i];
        params[ind] = defValues.nativeType()[i];
        paramConvs[ind] = params[ind].code;
        paramTypes[ind] = defValues.javaType()[i];
        genericParamTypes[ind] = defValues.javaType()[i];
      }

    }

    /**
     * Invokes a method and returns a value.
     *
     * @param ptr  The interface pointer. {@link ComMethod} has apriori knowledge
     *             of what interface it points to.
     *
     * @param args The invocation arguments.
     *
     * @return The return value of the method invocation
     */
    abstract Object invoke( long ptr, Object[] args );

    /**
     * Converts the parameters to be more native friendly.
     * @param args the array of objects to be converted.
     */
    @SuppressWarnings("unchecked")
    protected void messageParameters(Object[] args){
        for( int i=0; i<args.length; i++ ) {
            if(args[i] instanceof Holder && params[i].getNoByRef()!=null) {
                // massage the value of Holder, not the Holder itself
                Holder h = (Holder)args[i];
                h.value = params[i].getNoByRef().toNative(h.value);
            } else {
                args[i] = params[i].toNative(args[i]);
            }
        }
    }

    private static final Map<Class<?>,NativeType> defaultConversions = new HashMap<Class<?>, NativeType>();

    static {
        defaultConversions.put( Iterator.class, NativeType.ComObject );
        defaultConversions.put( GUID.class, NativeType.GUID );
        defaultConversions.put( double.class, NativeType.Double );
        defaultConversions.put( float.class, NativeType.Float );
        defaultConversions.put( long.class, NativeType.Int64 );
        defaultConversions.put( int.class, NativeType.Int32 );
        defaultConversions.put( short.class, NativeType.Int16 );
        defaultConversions.put( byte.class, NativeType.Int8 );
        defaultConversions.put( boolean.class, NativeType.VariantBool );
        defaultConversions.put( String.class, NativeType.BSTR );
        defaultConversions.put( Object.class, NativeType.VARIANT_ByRef );
        defaultConversions.put( Variant.class, NativeType.VARIANT_ByRef );
        defaultConversions.put( Date.class, NativeType.Date );
    }

    /**
     * Computes the default conversion for the given type.
     */
    static NativeType getDefaultConversion(Type t) {
        if( t instanceof Class ) {
            Class<?> c = (Class<?>)t;
            NativeType r = defaultConversions.get(c);
            if(r!=null) return r;

            if(Com4jObject.class.isAssignableFrom(c))
                return NativeType.ComObject;
            if(Enum.class.isAssignableFrom(c))
                return NativeType.Int32;
            if(Buffer.class.isAssignableFrom(c))
                return NativeType.PVOID;
            if(Calendar.class.isAssignableFrom(c))
                return NativeType.Date;
            if(BigDecimal.class.isAssignableFrom(c))
                return NativeType.Currency;
            if(c.isArray())
                return NativeType.SafeArray;
        }

        if( t instanceof ParameterizedType ) {
            ParameterizedType p = (ParameterizedType) t;
            if( p.getRawType()==Holder.class ) {
                // let p=Holder<V>
                Type v = p.getActualTypeArguments()[0];
                Class<?> c = (v instanceof Class) ? (Class<?>)v : null;
                if(c!=null) {
                  if(Com4jObject.class.isAssignableFrom(c))
                    return NativeType.ComObject_ByRef;
                  if(String.class==c)
                    return NativeType.BSTR_ByRef;
                  if(Integer.class==c || Enum.class.isAssignableFrom(c))
                    return NativeType.Int32_ByRef;
                  if(Boolean.class==c)
                    return NativeType.VariantBool_ByRef;
                  if(Buffer.class.isAssignableFrom(c))
                    return NativeType.PVOID_ByRef;
                  if(Double.class.isAssignableFrom(c))
                    return NativeType.Double_ByRef;
                  if(c.isArray())
                    return NativeType.SafeArray_ByRef;
                }
                if(v instanceof GenericArrayType){
                  return NativeType.SafeArray_ByRef;
                }
            }
            if( p.getRawType()==Iterator.class ) {
                return NativeType.ComObject;
            }
        }

        throw new IllegalAnnotationException("no default conversion available for "+t);
    }

    /**
     * Generates the default values for this ComMethod using the given {@link UseDefaultValues} annotation
     * @param defValues the annotation containing the information to generate the default values.
     */
    protected void generateDefaultParameters(UseDefaultValues defValues){
      int count = defValues.optParamIndex().length;
      NativeType[] nt = defValues.nativeType();
      Variant.Type[] vt = defValues.variantType();
      String[] literal = defValues.literal();
      for (int i = 0; i < count; i++) {
        switch(nt[i]){
          case Bool:
          case VariantBool:
          case VariantBool_ByRef:
            defaultParameters[i] = Boolean.parseBoolean(literal[i]);
            break;
          case BSTR:
          case BSTR_ByRef:
          case CSTR:
          case Unicode:
            defaultParameters[i] = literal[i];
            break;
          case Double:
          case Double_ByRef:
            defaultParameters[i] = Double.parseDouble(literal[i]);
            break;
          case Float:
          case Float_ByRef:
            defaultParameters[i] = Float.parseFloat(literal[i]);
            break;
          case Int8:
          case Int8_ByRef:
            defaultParameters[i] = Byte.parseByte(literal[i]);
            break;
          case Int16:
          case Int16_ByRef:
            defaultParameters[i] = Short.parseShort(literal[i]);
            break;
          case Int32:
          case Int32_ByRef:
            defaultParameters[i] = Integer.parseInt(literal[i]);
            break;
          case Int64:
          case Int64_ByRef:
            defaultParameters[i] = Long.parseLong(literal[i]);
            break;
          case GUID:
            defaultParameters[i] = new GUID(literal[i]);
            break;
          case Currency:
          case Currency_ByRef:
            defaultParameters[i] = new BigDecimal(literal[i]);
            break;
          case VARIANT:
            Variant v = new Variant();
            switch(vt[i]){
              case VT_I1:
              case VT_UI1:
                v.set(Byte.parseByte(literal[i]));
                break;
              case VT_I2:
              case VT_UI2:
                v.set(Short.parseShort(literal[i]));
                break;
              case VT_I4:
              case VT_UI4:
              case VT_INT:
              case VT_UINT:
                v.set(Integer.parseInt(literal[i]));
                break;
              case VT_I8:
                v.set(Long.parseLong(literal[i]));
                break;
              case VT_R4:
                v.set(Float.parseFloat(literal[i]));
                break;
              case VT_R8:
                v.set(Double.parseDouble(literal[i]));
                break;
              case VT_BOOL:
                v.set(Boolean.parseBoolean(literal[i]));
                break;
              case VT_BSTR:
                v.set(literal[i]);
                break;
              case VT_EMPTY:
                v= new Variant();
                break;
              case VT_ERROR:
                v.makeError((int)Long.parseLong(literal[i], 16));
                break;
//              case VT_CY: ...
              default:
                throw new UnsupportedOperationException("Don't know how to parse literal " + literal[i] + " to an Java Object corresponding to Variant.Type." + vt[i].name());
            }
            v.setType(vt[i]);
            defaultParameters[i] = v;
            break;
          default:
            throw new UnsupportedOperationException("Don't know how to parse literal " + literal[i] + " to an Java Object corresponding to NativeType." + nt[i].name());
        }
      }
    }
}
