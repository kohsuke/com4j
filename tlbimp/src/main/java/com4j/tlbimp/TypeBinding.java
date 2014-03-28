package com4j.tlbimp;

import com4j.COM4J;
import com4j.NativeType;
import com4j.Com4jObject;
import com4j.stdole.IFontDisp;
import com4j.stdole.IPictureDisp;
import com4j.tlbimp.def.IType;
import com4j.tlbimp.def.IPrimitiveType;
import com4j.tlbimp.def.IPtrType;
import com4j.tlbimp.def.IInterfaceDecl;
import com4j.tlbimp.def.IDispInterfaceDecl;
import com4j.tlbimp.def.ICoClassDecl;
import com4j.tlbimp.def.ITypeDecl;
import com4j.tlbimp.def.VarType;
import com4j.tlbimp.def.ISafeArrayType;
import com4j.tlbimp.def.ITypedefDecl;
import com4j.tlbimp.def.IEnumDecl;

import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Date;
import java.nio.Buffer;
import java.math.BigDecimal;

/**
 * Represents how a type is bound between native type
 * and Java type.
 *
 * @author Kohsuke Kawaguchi
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
final class TypeBinding {
    public final String javaType;
    public final NativeType nativeType;
    /**
     * True if the {@link #nativeType} is the default native type
     * for the {@link #javaType}.
     */
    public final boolean isDefault;

    public TypeBinding(Class<?> javaType, NativeType nativeType, boolean isDefault) {
        this(javaType.getName(),nativeType,isDefault);
    }

    public TypeBinding(String javaType, NativeType nativeType, boolean isDefault) {
        this.javaType = javaType;
        this.nativeType = nativeType;
        this.isDefault = isDefault;
    }

    public TypeBinding createByRef() {
        String t = javaType;
        if(boxTypeMap.containsKey(t))
            t = boxTypeMap.get(t);
        return new com4j.tlbimp.TypeBinding( "Holder<"+t+">", nativeType.byRef(), isDefault );
    }

    /**
     * Binds the native type to a Java type and its conversion.
     *
     * @param nameHint
     *      Optional parameter name of the type. If non-null,
     *      this is used to disambiguate common mistake
     *      like declaring LPWSTR as "ushort*", etc.
     */
    public static TypeBinding bind( Generator g, IType t, String nameHint ) throws BindingException {
        IPrimitiveType pt = t.queryInterface(IPrimitiveType.class);
        if(pt!=null) {
            // primitive
            TypeBinding r = primitiveTypeBindings.get(pt.getVarType());
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
                return new TypeBinding( g.getTypeName(compDecl), NativeType.ComObject, true );
            }

            IDispInterfaceDecl dispDecl = comp.queryInterface(IDispInterfaceDecl.class);
            if( dispDecl!=null ) {
                // t = T* where T is a declared interface
                return new TypeBinding( g.getTypeName(dispDecl), NativeType.ComObject,  true );
            }

            // t = coclass*
            ICoClassDecl classdecl = comp.queryInterface(ICoClassDecl.class);
            if(classdecl!=null) {
                // bind to its default interface
                ITypeDecl di = g.getDefaultInterface(classdecl);
                if(di==null)
                    // no primary interface known. treat it as IUnknown
                    return new TypeBinding(Com4jObject.class, NativeType.ComObject, true);
                else
                    return new TypeBinding( g.getTypeName(di), NativeType.ComObject, true );
            }

            IPrimitiveType compPrim = comp.queryInterface(IPrimitiveType.class);
            if( compPrim!=null ) {
                // T = PRIMITIVE*
                if( compPrim.getVarType()== VarType.VT_VARIANT ) {
                    // T = VARIANT*
                    return new TypeBinding(Object.class, NativeType.VARIANT_ByRef, true);
                }
                if( compPrim.getVarType()==VarType.VT_VOID ) {
                    // T = void*
                    return new TypeBinding(Buffer.class,  NativeType.PVOID, true );
                }
                if( compPrim.getVarType()==VarType.VT_UI2 ) {
                    // T = ushort*
                    if( isPsz(nameHint) )
                        // this is a common mistake
                        return new TypeBinding( String.class, NativeType.Unicode, false );
                }
                if( compPrim.getVarType()==VarType.VT_BOOL ) {
                    return new TypeBinding("Holder<Boolean>", NativeType.VariantBool_ByRef, true );
                }
            }

            // a few other random checks
            String name = getTypeString(ptrt);
            if( name.equals("_RemotableHandle*") ) {
                // marshal as the raw pointer value
                return new TypeBinding( Integer.TYPE, NativeType.Int32,  true );
            }
            if(name.equals("GUID*")) {
                return new TypeBinding( "GUID", NativeType.GUID, true );
            }

            // otherwise use a holder
            TypeBinding b = bind(g,comp,null);
            if(b!=null && b.nativeType.byRef()!=null )
                return b.createByRef();
        }

        ISafeArrayType at = t.queryInterface(ISafeArrayType.class);
        if(at!=null) {
            // T=SAFEARRAY(...)
            IType comp = at.getComponentType();

            IPrimitiveType compPrim = comp.queryInterface(IPrimitiveType.class);
            if( compPrim!=null ) {
                TypeBinding r = primitiveTypeBindings.get(compPrim.getVarType());
                if(r!=null) {
                    return new TypeBinding(r.javaType+"[]", NativeType.SafeArray, true );
                }
            }
            TypeBinding tb = TypeBinding.bind(g, comp, null);
            if(tb.nativeType == NativeType.VARIANT){
              return new TypeBinding("Object[]", NativeType.SafeArray, true);
            }
        }

        // T = typedef
        ITypedefDecl typedef = t.queryInterface(ITypedefDecl.class);
        if(typedef!=null) {
            return bind(g,typedef.getDefinition(),nameHint);
        }

        // T = enum
        IEnumDecl enumdef = t.queryInterface(IEnumDecl.class);
        if(enumdef!=null) {
            // TODO: we should probably check the size of the enum.
            // there's no guarantee it's 32 bit.
            return new TypeBinding( g.getTypeName(enumdef), NativeType.Int32, true );
        }

        // a few other random checks
        String name = getTypeString(t);
        if(name.equals("GUID")) {
            return new TypeBinding( "GUID", NativeType.GUID, true );
        }

        IDispInterfaceDecl disp = t.queryInterface(IDispInterfaceDecl.class);
        if(disp != null) {
            // TODO check this: this is a dispatch interface, so bind to Com4jObject?
            if(disp.getGUID().equals(COM4J.IID_IPictureDisp)){
                return new TypeBinding(IPictureDisp.class, NativeType.ComObject, true);
            }
            if(disp.getGUID().equals(COM4J.IID_IFontDisp)){
                return new TypeBinding(IFontDisp.class, NativeType.ComObject, true);
            }
            // TODO: not clear how we should handle this
            throw new BindingException(Messages.UNSUPPORTED_TYPE.format(getTypeString(t)));
        }

        ITypeDecl declt = t.queryInterface(ITypeDecl.class);
        if(declt!=null) {
            // TODO: not clear how we should handle this
            throw new BindingException(Messages.UNSUPPORTED_TYPE.format(getTypeString(t)));
        }

        throw new BindingException(Messages.UNSUPPORTED_TYPE.format(getTypeString(t)));
    }

    /**
     * Returns a human-readable identifier of the type,
     * but it's not necessarily a correct Java id.
     *
     * This is mainly for debugging.
     */
    private static String getTypeString(IType t) {
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

        ISafeArrayType sa = t.queryInterface(ISafeArrayType.class);
        if (sa != null) {
          return "SAVEARRAY(" + getTypeString(sa.getComponentType()) + ")";
        }

        return "N/A";
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

    /**
     * Defines the type bindings for the primitive types.
     */
    private static final Map<VarType,TypeBinding> primitiveTypeBindings
        = new EnumMap<VarType,TypeBinding>(VarType.class);

    static {
        // initialize the primitive binding
        pbind( VarType.VT_I1, Byte.TYPE, NativeType.Int8, true );
        pbind( VarType.VT_I2, Short.TYPE, NativeType.Int16, true );
        pbind( VarType.VT_I4, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_I8, Long.TYPE, NativeType.Int64, true );
        pbind( VarType.VT_BSTR, String.class, NativeType.BSTR, true );
        pbind( VarType.VT_LPSTR, String.class, NativeType.CSTR, false );
        pbind( VarType.VT_LPWSTR, String.class, NativeType.Unicode, false );
        // TODO: is it OK to map UI2 to short?
        pbind( VarType.VT_UI1, Byte.TYPE, NativeType.Int8, true );
        pbind( VarType.VT_UI2, Short.TYPE, NativeType.Int16, true );
        pbind( VarType.VT_UI4, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_UI8, Long.TYPE, NativeType.Int64, true );
        pbind( VarType.VT_INT, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_HRESULT, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_UINT, Integer.TYPE, NativeType.Int32, true );
        pbind( VarType.VT_BOOL, Boolean.TYPE, NativeType.VariantBool, true );
        pbind( VarType.VT_R4, Float.TYPE, NativeType.Float, true );
        pbind( VarType.VT_R8, Double.TYPE, NativeType.Double, true );
        pbind( VarType.VT_VARIANT, Object.class, NativeType.VARIANT, false );
        pbind( VarType.VT_DISPATCH, Com4jObject.class, NativeType.Dispatch, false );
        pbind( VarType.VT_UNKNOWN, Com4jObject.class, NativeType.ComObject, true );
        pbind( VarType.VT_DATE, Date.class, NativeType.Date, true );
        pbind( VarType.VT_CY, BigDecimal.class, NativeType.Currency, false );
        pbind( VarType.VT_VOID, void.class, NativeType.Bool/*dummy*/, true );
    }

    private static void pbind( VarType vt, Class<?> c, NativeType n, boolean isDefault ) {
        primitiveTypeBindings.put(vt,new TypeBinding(c,n,isDefault));
    }

    private static boolean isPsz( String hint ) {
        if(hint==null)  return false;
        return hint.startsWith("psz") || hint.startsWith("lpsz");
    }
}
