#include "stdafx.h"
#include "com4j.h"
#include "com4j_variant.h"

// used to map two jlongs to the memory image of VARIANT.
class VARIANT_T : public VARIANT {
public:
	VARIANT_T() {
		VariantInit(this);
	}
	VARIANT_T( jlong a, jlong b ) {
		jlong* pThis = reinterpret_cast<jlong*>(this);
		pThis[0] = a;
		pThis[1] = b;
	}
	VARIANT_T( JNIEnv* env, jlongArray array ) {
		env->GetLongArrayRegion(array,0,2,reinterpret_cast<jlong*>(this));
	}
	
	// write back to the jlongarray
	void writeTo( JNIEnv* env, jlongArray array ) {
		env->GetLongArrayRegion(array,0,2,reinterpret_cast<jlong*>(this));
	}
};


JNIEXPORT void JNICALL Java_com4j_Variant_clear0(JNIEnv* env, jclass, jlong image0, jlong image1) {
	VARIANT_T v(image0,image1);
	HRESULT hr = VariantClear(&v);
	if(FAILED(hr))
		error(env,hr,"failed to clear variant");
}

// change the variant type in the same place
void VariantChangeType( JNIEnv* env, VARIANT* v, VARTYPE type ) {
	VARIANT_T dst;
	HRESULT hr = VariantChangeType(&dst,v,0, type );
	if(FAILED(hr)) {
		error(env,hr,"failed to change the variant type");
		return;
	}
	VariantClear(v);
	*v = dst;
}

JNIEXPORT void JNICALL Java_com4j_Variant_changeType0(JNIEnv* env, jclass, jint type, jlongArray image) {
	VARIANT_T v(env,image);
	VariantChangeType( env, &v, (VARTYPE)type );
	v.writeTo(env,image);
}

JNIEXPORT jfloat JNICALL Java_com4j_Variant_castToFloat0(JNIEnv* env, jclass, jlongArray image) {
	VARIANT_T v(env,image);
	VariantChangeType( env, &v, VT_R4 );
	v.writeTo(env,image);
	return v.fltVal;
}

JNIEXPORT jdouble JNICALL Java_com4j_Variant_castToDouble0(JNIEnv* env, jclass, jlongArray image) {
	VARIANT_T v(env,image);
	VariantChangeType( env, &v, VT_R8 );
	v.writeTo(env,image);
	return v.dblVal;
}
