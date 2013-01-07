#pragma once
//
// unmarshals the value from a native type to a Java type
// after the method invocation is done.
// 

#include "cleanup.h"
#include "xducer.h"
#include "variant.h"
#include <atlcur.h>

// wraps the com4j.Holder object for convenient access
class Holder : public _jobject {
public:
	jobject get(JNIEnv* env) {
		return env->GetObjectField( this, com4j_Holder_value );
	}
	void set(JNIEnv* env, jobject value ) {
		env->SetObjectField( this, com4j_Holder_value, value );
	}
};
typedef Holder* jholder;


class Unmarshaller {
public:
	virtual jobject unmarshal( JNIEnv* env ) = 0;
	// the address of the variable that should receive the value during a method invocation
	virtual void* addr() = 0;
	virtual ~Unmarshaller() {}
};


// write back an out parameter to a holder
class OutParamHandler : public PostAction {
protected:
	// receive the value after the unmarshalling
	jholder holder;
	Unmarshaller* const		unmarshaller;

public:
	OutParamHandler( jholder _holder, Unmarshaller* unm )
		: holder(_holder), unmarshaller(unm) {}

	void act( JNIEnv* env ) {
		holder->set( env, unmarshaller->unmarshal(env) );
	}

	~OutParamHandler() {
		delete unmarshaller;
	}
};

class BSTRUnmarshaller : public Unmarshaller {
	BSTR bstr;
public:
	BSTRUnmarshaller( BSTR _bstr )
		: bstr(_bstr) {}

	~BSTRUnmarshaller() {
		SysFreeString(bstr);
	}

	void* addr() {
		return &bstr;
	}

	jobject unmarshal( JNIEnv* env ) {
		return xducer::StringXducer::toJava(env,bstr);
	}
};

template < class XDUCER >
class PrimitiveUnmarshaller : public Unmarshaller {
	typename XDUCER::NativeType value;
public:
	PrimitiveUnmarshaller( JNIEnv* env, typename XDUCER::JavaType i ) {
		value = 0;
		if(i!=NULL)
			value = XDUCER::toNative(env,i);
	}
	
	void* addr() { return &value; }

	typename XDUCER::JavaType unmarshal( JNIEnv* env ) {
		return XDUCER::toJava(env,value);
	}
};

typedef PrimitiveUnmarshaller<xducer::BoxedByteXducer>		ByteUnmarshaller;
typedef PrimitiveUnmarshaller<xducer::BoxedShortXducer>		ShortUnmarshaller;
typedef PrimitiveUnmarshaller<xducer::BoxedIntXducer>		IntUnmarshaller;
typedef PrimitiveUnmarshaller<xducer::BoxedLongXducer>		LongUnmarshaller;
typedef PrimitiveUnmarshaller<xducer::BoxedFloatXducer>		FloatUnmarshaller;
typedef PrimitiveUnmarshaller<xducer::BoxedDoubleXducer>	DoubleUnmarshaller;



typedef PrimitiveUnmarshaller<xducer::BoxedBoolXducer>	BoolUnmarshaller;
typedef PrimitiveUnmarshaller<xducer::BoxedVariantBoolXducer>	VariantBoolUnmarshaller;



class ComObjectUnmarshaller : public Unmarshaller {
	IUnknown* pv;
public:
	ComObjectUnmarshaller() {
		pv = NULL;
	}

	void* addr() {
		return &pv;
	}

	jobject unmarshal( JNIEnv* env ) {
		if(pv==NULL)
			return NULL;

		return javaLangLong_valueOf( env, reinterpret_cast<jlong>(pv) );
	}
};


// unmarshals a GUID into 2-length long array
class GUIDUnmarshaller : public Unmarshaller {
	GUID guid;
public:
	virtual jobject unmarshal( JNIEnv* env ) {
		jlongArray r = env->NewLongArray(2);
		_ASSERT(sizeof(GUID)==sizeof(jlong)*2);
		env->SetLongArrayRegion(r,0,2, reinterpret_cast<jlong*>(&guid) );
		return r;
	}
	virtual void* addr() {
		return &guid;
	}
};

// unmarshals CURRENCY into a BigDecimal
class CurrencyUnmarshaller : public Unmarshaller {
	CComCurrency cy;
public:
	CurrencyUnmarshaller() {}
	CurrencyUnmarshaller(CComCurrency _cy) : cy(_cy) {}

	virtual jobject unmarshal( JNIEnv* env ) {
		char w[128];
		sprintf(w,"%I64i",cy.m_currency.int64);

		jobject absolute = javaMathBigInteger_new(env,env->NewStringUTF(w));
		return javaMathBigDecimal_new(env,absolute,4);
	}
	virtual void* addr() {
		return &(cy.m_currency);
	}
};

class VariantUnmarshaller : public Unmarshaller {
	// we expect the invoked method to set this VARIANT
	VARIANT v;
public:
	VariantUnmarshaller() {
		VariantInit(&v);
	}
	virtual jobject unmarshal( JNIEnv* env ) {
		jobject var = com4j_Variant_new(env);
		*com4jVariantToVARIANT(env,var) = v;	// direct copy, so no need to VariantClear(v);
		return var;
	}
	virtual void* addr() {
		return &v;
	}
};

class SafeArrayUnmarshaller : public Unmarshaller {
	SAFEARRAY* pOriginalSA;
	SAFEARRAY* pNewSA;
	jobject originalJSA;
public:
	SafeArrayUnmarshaller(SAFEARRAY* pOriginalSA, jobject originalJSA) {
		this->pOriginalSA = pOriginalSA;
		this->originalJSA = originalJSA;
		this->pNewSA = pOriginalSA;
	}
	virtual jobject unmarshal( JNIEnv* env ) {
		//The method did not change the value of the out param.  In this
		//case we can return the original wrapper
		if(pOriginalSA == pNewSA) {
			return originalJSA;
		}

		//The value of the out param changed.  Tell the managed side
		//that it's underlying SAFEARRAY was deleted natively by the
		//callee.
		if(originalJSA) {
			com4jSafeArray_markNativeDestroyed(env, originalJSA);
		}

		if(pNewSA == NULL)
			return NULL;

		//Turn the native SAFEARRAY into a Java wrapper object.  From there the java side
		//can unmarshall the array components.
		jobject sa = com4jSafeArray_new(env, reinterpret_cast<jlong>(pNewSA));
		return sa;
	}
	virtual void* addr() {
		return &pNewSA;
	}
};
