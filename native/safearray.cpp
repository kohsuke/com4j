#include "stdafx.h"
#include "safearray.h"

using namespace safearray;


pair<SafeArrayXducer::NativeType,VARTYPE> SafeArrayXducer::toNative2(
	JNIEnv* env, SafeArrayXducer::JavaType a ) {

	struct Entry {
		JClassID* clazz;
		VARTYPE vt;
		SAFEARRAY* (* handler)( JNIEnv* env, jarray javaArray );
	};

	// conversion table
	static Entry entries[] = {
		{ &booleanArray,VT_BOOL,	PrimitiveArrayXducer<VT_BOOL,VARIANT_BOOL,jboolean>::toNative },
		{ &byteArray,	VT_UI1,		 PrimitiveArrayXducer<VT_UI1,byte,jbyte>::toNative },
		{ &charArray,	VT_UI2,		PrimitiveArrayXducer<VT_UI2,unsigned short,jchar>::toNative },
		{ &doubleArray,	VT_R4,		PrimitiveArrayXducer<VT_R8,double,jdouble>::toNative },
		{ &floatArray,	VT_R8,		PrimitiveArrayXducer<VT_R4,float,jfloat>::toNative },
		{ &intArray,	VT_I4,		PrimitiveArrayXducer<VT_I4,INT32,jint>::toNative },
		{ &longArray,	VT_I8,		PrimitiveArrayXducer<VT_I8,INT64,jlong>::toNative },
		{ &shortArray,	VT_I2,		PrimitiveArrayXducer<VT_I2,short,jshort>::toNative },
		{ &stringArray,	VT_BSTR,	BasicArrayXducer<VT_BSTR,xducer::StringXducer>::toNative },
		{ &objectArray,	VT_VARIANT,	BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toNative },
		{ NULL, NULL }
	};

	jclass clz = env->GetObjectClass(a);

	for( Entry* e=entries; e->clazz!=NULL; e++ ) {
		if(env->IsSameObject(clz,*(e->clazz)))
			return pair<SAFEARRAY*,VARTYPE>( (e->handler)(env,a), e->vt );
	}
	return pair<SAFEARRAY*,VARTYPE>(NULL,VT_EMPTY);
}
