#include "stdafx.h"
#include "safearray.h"

using namespace safearray;


SafeArrayXducer::NativeType SafeArrayXducer::toNative(
	JNIEnv* env, SafeArrayXducer::JavaType a ) {

	struct Entry {
		JClassID* clazz;
		SAFEARRAY* (* handler)( JNIEnv* env, jarray javaArray );
	};

	// conversion table
	static Entry entries[] = {
		{ &booleanArray,PrimitiveArrayXducer<VT_BOOL,VARIANT_BOOL,jboolean>::toNative },
		{ &byteArray,	PrimitiveArrayXducer<VT_UI1,byte,jbyte>::toNative },
		{ &charArray,	PrimitiveArrayXducer<VT_UI2,unsigned short,jchar>::toNative },
		{ &doubleArray,	PrimitiveArrayXducer<VT_R8,double,jdouble>::toNative },
		{ &floatArray,	PrimitiveArrayXducer<VT_R4,float,jfloat>::toNative },
		{ &intArray,	PrimitiveArrayXducer<VT_I4,INT32,jint>::toNative },
		{ &longArray,	PrimitiveArrayXducer<VT_I8,INT64,jlong>::toNative },
		{ &shortArray,	PrimitiveArrayXducer<VT_I2,short,jshort>::toNative },
		{ &stringArray,	BasicArrayXducer<VT_BSTR,xducer::StringXducer>::toNative },
		{ &objectArray,	BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toNative },
		{ NULL, NULL }
	};

	jclass clz = env->GetObjectClass(a);

	for( Entry* e=entries; e->clazz!=NULL; e++ ) {
		if(env->IsSameObject(clz,*(e->clazz)))
			return (e->handler)(env,a);
	}
	return NULL;
}
