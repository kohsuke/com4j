#pragma once
//
// unmarshals the value from a native type to a Java type
// after the method invocation is done.
// 

#include "cleanup.h"
#include "xducer.h"

extern jfieldID com4j_Holder_value;

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

		return javaLangInteger_valueOf( env, reinterpret_cast<jint>(pv) );
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

class VariantUnmarshaller : public Unmarshaller {
	// the return type that the caller is expecting.
	jclass const		retType;
	// we expect the invoked method to set this VARIANT
	VARIANT v;
public:
	VariantUnmarshaller( jclass _retType ) : retType(_retType) {
		VariantInit(&v);
	}
	virtual jobject unmarshal( JNIEnv* env );
	virtual void* addr() {
		return &v;
	}
};
