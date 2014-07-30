#include "stdafx.h"
#include "com4j.h"
#include "com4j_variant.h"
#include "xducer.h"
#include "unmarshaller.h"
#include "safearray.h"
#include "variant.h"

/**  
 * Original auther  (C) Kohsuke Kawaguchi (kk@kohsuke.org)
 * Modified by      (C) Michael Schnell (scm, 2008, Michael-Schnell@gmx.de)
 */


JNIEXPORT void JNICALL Java_com4j_Variant_clear0(JNIEnv* env, jclass, jobject image) {
	HRESULT hr = VariantClear((VARIANT*)env->GetDirectBufferAddress(image));
	if(FAILED(hr))
		error(env,__FILE__,__LINE__,hr,"failed to clear variant");
}

// change the variant type in the same place
void VariantChangeType( JNIEnv* env, VARIANT* v, VARTYPE type ) {
	VARIANT dst;
	VariantInit(&dst);
	HRESULT hr = VariantChangeType(&dst,v,0, type );
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"failed to change the variant type");
		return;
	}
	VariantClear(v);
	*v = dst;
}

JNIEXPORT void JNICALL Java_com4j_Variant_changeType0(JNIEnv* env, jclass, jint type, jobject image) {
	VariantChangeType( env, (VARIANT*)env->GetDirectBufferAddress(image), (VARTYPE)type );
}

JNIEXPORT jobject JNICALL Java_com4j_Variant_convertTo(JNIEnv* env, jobject instance, jclass target) {
	try {
		VARIANT* v = com4jVariantToVARIANT(env,instance);
		while(v->vt & VT_BYREF) // unpeel VT_BYREF to get to the nested VARIANT
			v = reinterpret_cast<VARIANT*>(v->byref);
		jobject r = variantToObject(env,target,*v);
		if(r==reinterpret_cast<jobject>(-1)) {
			jstring name = javaLangClass_getName(env,target);
			error(env,__FILE__,__LINE__,E_FAIL,"Unable to convert VARIANT %d to the %s",
				v->vt,
				LPCSTR(JString(env,name)));
			return NULL;
		}
		return r;
	} catch( _com_error& e ) {
		error(env,__FILE__,__LINE__,e.Error(),"%s",(LPCSTR)_bstr_t(e.ErrorMessage()));
		return NULL;
	}
}

JNIEXPORT void JNICALL Java_com4j_Variant_set0(JNIEnv * env, jobject, jobject value, jobject image) {
	VARIANT *destVar = (VARIANT*)env->GetDirectBufferAddress(image);
	VARIANT *newVar = convertToVariant(env, value);
	VariantCopy(destVar, newVar);
}

JNIEXPORT jobject JNICALL Java_com4j_Variant_get0(JNIEnv *env, jobject, jobject image) {
	VARIANT *var = (VARIANT*)env->GetDirectBufferAddress(image);
	return variantToObject(env, NULL, *var);
}



class VariantHandler {
public:
	// returnss VARIANT allocated by 'new'
	virtual VARIANT* set( JNIEnv* env, jobject src ) = 0;
	virtual jobject get( JNIEnv* env, VARIANT* v, jclass retType ) = 0;
};

template <VARTYPE vt, class XDUCER>
class VariantHandlerImpl : public VariantHandler {
protected:
	inline typename XDUCER::NativeType& addr(VARIANT* v) {
		return *reinterpret_cast<XDUCER::NativeType*>(&v->boolVal);
	}
public:
	VARIANT* set( JNIEnv* env, jobject o ) {
		VARIANT* v = new VARIANT();
		VariantInit(v);
		v->vt = vt;
		addr(v) = XDUCER::toNative(
			env, static_cast<XDUCER::JavaType>(o) );
		return v;
	}
	jobject get( JNIEnv* env, VARIANT* v, jclass retType ) {
		_variant_t dst(v);
		dst.ChangeType(vt);
		jobject o = XDUCER::toJava(env, addr(&dst));
		return o;
	}
};

class ComObjectVariandHandlerImpl : public VariantHandlerImpl<VT_DISPATCH,xducer::Com4jObjectXducer> {
	typedef VariantHandlerImpl<VT_DISPATCH,xducer::Com4jObjectXducer> BASE;

	VARIANT* set( JNIEnv* env, jobject o) {
		VARIANT* v = BASE::set(env,o);
		IDispatch* pDisp = NULL;
		HRESULT hr = addr(v)->QueryInterface(&pDisp);
		if(SUCCEEDED(hr)) {
			// if possible, use VT_DISPATCH.
			addr(v)->Release();
			addr(v) = pDisp;
			v->vt = VT_DISPATCH;
		} // otherwise use VT_UNKNOWN. See java.net issue 2.
		return v;
	}
	jobject get( JNIEnv* env, VARIANT* v, jclass retType ) {
		jobject o = BASE::get(env,v,retType);
		if(o==NULL)		return NULL;

		// if the return type is an interface, use that to create a strongly typed object.
		// otherwise just return it as Com4jObject
		if(env->IsSameObject(retType,javaLangObject) || env->IsSameObject(retType,com4j_Com4jObject))
			return o;
		else {
			jobject o2 = com4jWrapper_queryInterface(env,o,retType);
			com4jWrapper_dispose0(env,o);
			return o2;
		}
	}
};

class NoopVariantHandlerImpl : public VariantHandler {
public:
	VARIANT* set( JNIEnv* env, jobject src ) {
		VARIANT* pv = new VARIANT();
		VariantInit(pv);
		VariantCopy(pv, com4jVariantToVARIANT(env,src));
		return pv;
	}
	jobject get( JNIEnv* env, VARIANT* v, jclass retType ) {
		jobject r = com4j_Variant_new(env);
		::VariantCopy(com4jVariantToVARIANT(env,r),v);
		return r;
	}
};

class ComEnumHandlerImpl : public VariantHandler {
public:
	VARIANT* set( JNIEnv* env, jobject src ) {
		VARIANT* pv = new VARIANT();
		VariantInit(pv);
		pv->vt = VT_I4;
		pv->intVal = com4j_ComEnum_comEnumValue(env,src);
		return pv;
	}
	jobject get( JNIEnv* env, VARIANT* v, jclass retType ) {
		_variant_t dst(v);
		dst.ChangeType(VT_I4);
		
		return com4j_enumDictionary_get(env,retType,dst.intVal);
	}
};

class DateHandlerImpl : public VariantHandler {
public:
	VARIANT* set( JNIEnv* env, jobject src ) {
		jdouble d = com4j_Variant_fromDate(env,src);
		VARIANT* pv = new VARIANT();
		VariantInit(pv);
		pv->vt = VT_DATE;
		pv->date = d;
		return pv;
	}
	jobject get( JNIEnv* env, VARIANT* v, jclass retType ) {
		_variant_t dst(v);
		dst.ChangeType(VT_DATE);
		return com4j_Variant_toDate(env,dst.date);
	}
};

class DecimalHandlerImpl : public VariantHandler {
public:
	VARIANT* set( JNIEnv* env, jobject src ) {
		VARIANT* pv = new VARIANT();
		VariantInit(pv);
		pv->vt = VT_DECIMAL;

		jstring s = javaMathBigDecimal_toString(env,src);
		BSTR bs = SysAllocString(JString(env,s));
		::VarDecFromStr( bs, LOCALE_USER_DEFAULT, 0, &(pv->decVal) );
		SysFreeString(bs);
		return pv;
	}
	jobject get( JNIEnv* env, VARIANT* v, jclass retType ) {
		_variant_t dst(v);
		dst.ChangeType(VT_DECIMAL);
		BSTR bs;
		::VarBstrFromDec(&dst.decVal, LOCALE_USER_DEFAULT, 0, &bs);
		jobject r = javaMathBigDecimal_new_Str(env, env->NewString(bs, SysStringLen(bs)));
		SysFreeString(bs);
		return r;
	}
};

class NullVariantHandlerImpl : public VariantHandler {
public:
	VARIANT* set( JNIEnv* env, jobject src ) {
		_ASSERT(FALSE);
		return NULL;
	}
	jobject get( JNIEnv* env, VARIANT* v, jclass retType ) {
		return NULL;
	}
};
// conversion table for variant
// from Java->native, we look for the cls field that can accept the current object,
// then if they match, we'll call the handler.
// from native->Java, we look for the vt field that can accept the current variant type.
// then if they match, we'll call the handler
struct SetterEntry {
	JClassID* cls;
	VARTYPE vt;
	VariantHandler* handler;
};

static SetterEntry setters[] = {
	{ &javaLangBoolean, VT_BOOL,		new VariantHandlerImpl<VT_BOOL,		xducer::BoxedVariantBoolXducer>() },
	{ &javaLangString,	VT_BSTR,		new VariantHandlerImpl<VT_BSTR,		xducer::StringXducer>() },
	{ &javaLangFloat,	VT_R4,			new VariantHandlerImpl<VT_R4,		xducer::BoxedFloatXducer>() },
	{ &javaLangDouble,	VT_R8,			new VariantHandlerImpl<VT_R8,		xducer::BoxedDoubleXducer>() },
	{ &javaLangByte,	VT_I1,			new VariantHandlerImpl<VT_I1,		xducer::BoxedByteXducer>() }, 
	{ &javaLangShort,	VT_I2,			new VariantHandlerImpl<VT_I2,		xducer::BoxedShortXducer>() },
	{ &javaLangInteger,	VT_I4,			new VariantHandlerImpl<VT_I4,		xducer::BoxedIntXducer>() },
	{ &javaLangLong,	VT_I8,			new VariantHandlerImpl<VT_I8,		xducer::BoxedLongXducer>() },
	{ &javaLangLong,	VT_INT,			new VariantHandlerImpl<VT_INT,		xducer::BoxedIntXducer>() },
	{ &javaUtilDate,	VT_DATE,		new DateHandlerImpl() },
	{ &javaMathBigDecimal,	VT_DECIMAL,	new DecimalHandlerImpl() },
	// see issue 2 on java.net. I used to convert a COM object to VT_UNKNOWN
	{ &com4j_Com4jObject,VT_DISPATCH,	new ComObjectVariandHandlerImpl() },
	{ &com4j_Com4jObject,VT_UNKNOWN,	new ComObjectVariandHandlerImpl() },
	{ &com4j_Variant,	-1,				new NoopVariantHandlerImpl() }, // don't match from native->Java
	{ &com4j_ComEnum,	-1,				new ComEnumHandlerImpl() }, // don't match from native->Java
	// unsigned versions. when converting from Java, prefer signed versions
	{ &javaLangShort,	VT_UI1,			new VariantHandlerImpl<VT_I2,		xducer::BoxedShortXducer>() }, // use VT_I2 for VariantHandlerImpl since that's what BoxedShortXducer expect
	{ &javaLangInteger,	VT_UI2,			new VariantHandlerImpl<VT_I4,		xducer::BoxedIntXducer>() },
	{ &javaLangLong,	VT_UI4,			new VariantHandlerImpl<VT_I8,		xducer::BoxedLongXducer>() },
	{ &javaMathBigInteger,VT_UI8,		new VariantHandlerImpl<VT_UI8,		xducer::BigIntegerXducer>() },
	// TODO: Holder support
	{ NULL, 0, NULL }
};

// returns a VARIANT allocated by 'new'
VARIANT* convertToVariant( JNIEnv* env, jobject o ) {
	jclass cls = env->GetObjectClass(o);
	
	// consdier a conversion for scalars
	for( SetterEntry* p = setters; p->cls!=NULL; p++ ) {
		if( env->IsAssignableFrom( cls, *(p->cls) ) ) {
			VARIANT* v = p->handler->set(env,o);
			return v;
		}
	}

	// consider a conversion to SAFEARRAY
	pair<SAFEARRAY*,VARTYPE> sa = safearray::SafeArrayXducer::toNative2(env,static_cast<jarray>(o));
	if(sa.first!=NULL) {
		VARIANT* v = new VARIANT();
		VariantInit(v);
		v->vt = VT_ARRAY|sa.second;
		v->parray = sa.first;
		return v;
	}

	return NULL;
}

jobject variantToObject( JNIEnv* env, jclass retType, VARIANT& v ) {
	if(v.vt==VT_ERROR || v.vt==VT_EMPTY || v.vt==VT_NULL)
		return NULL;

	if(retType != NULL){
		// return type driven
		for( SetterEntry* p = setters; p->cls!=NULL; p++ ) {
			if( env->IsAssignableFrom( retType, *(p->cls) ) ) {
				return p->handler->get(env,&v,retType);
			}
		}
	}

	// if none is found, drive by the variant type
	for( SetterEntry* p = setters; p->cls!=NULL; p++ ) {
		if( v.vt==p->vt ) {
			return p->handler->get(env,&v,retType);
		}
	}

	// consider a conversion from SAFEARRAY
	if((v.vt&VT_ARRAY)!=0) {
		return safearray::SafeArrayXducer::toJava(env,v.parray);
	}

	// everything failed
	return reinterpret_cast<jobject>(-1);
}
