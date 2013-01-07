#include "stdafx.h"
#include "com4j_SafeArray.h"
#include "safearray.h"

using namespace safearray;

/**  
 * Original auther  (C) Kohsuke Kawaguchi (kk@kohsuke.org)
 * Modified by      (C) Michael Schnell (scm, 2008, Michael-Schnell@gmx.de)
 * Modified by		(C) Michael Poindexter (staticsnow@gmail.com)
 */

struct Entry {
	JClassID* clazz;
	VARTYPE vt;
	SAFEARRAY* (* toNative)( JNIEnv* env, jarray javaArray );
	jarray (* toJava)( JNIEnv* env, SAFEARRAY* nativeArray );
	jobject (* getJavaElement)( JNIEnv* env, SAFEARRAY* nativeArray, jintArray indices );
	void (* setNativeElement)( JNIEnv* env, SAFEARRAY* nativeArray, jintArray indices, jobject data );
};

JClassID variantArray("[Lcom4j/Variant;");

#define TABLE_ENTRY_PRIMITIVE(type, VariantType, NativeType, BoxedXDUCER) \
{  &type##Array, VariantType, PrimitiveArrayXducer<VariantType,NativeType,j##type,BoxedXDUCER>::toNative, PrimitiveArrayXducer<VariantType,NativeType,j##type,BoxedXDUCER>::toJava, PrimitiveArrayXducer<VariantType,NativeType,j##type,BoxedXDUCER>::getJavaElement, PrimitiveArrayXducer<VariantType,NativeType,j##type,BoxedXDUCER>::setNativeElement }

// conversion table
static Entry entries[] = {
	TABLE_ENTRY_PRIMITIVE(boolean,	VT_BOOL,	VARIANT_BOOL,	xducer::BoxedVariantBoolXducer),
	TABLE_ENTRY_PRIMITIVE(byte,		VT_UI1,		byte,			xducer::BoxedByteXducer),
	TABLE_ENTRY_PRIMITIVE(char,		VT_UI2,		unsigned short, xducer::BoxedCharXducer),
	TABLE_ENTRY_PRIMITIVE(double,	VT_R8,		double,			xducer::BoxedDoubleXducer),
	TABLE_ENTRY_PRIMITIVE(float,	VT_R4,		float,			xducer::BoxedFloatXducer),
	TABLE_ENTRY_PRIMITIVE(int,		VT_I4,		INT32,			xducer::BoxedIntXducer),
	TABLE_ENTRY_PRIMITIVE(long,		VT_I8,		INT64,			xducer::BoxedLongXducer),
	TABLE_ENTRY_PRIMITIVE(short,	VT_I2,		short,			xducer::BoxedShortXducer),
	{ &stringArray,		VT_BSTR,	BasicArrayXducer<VT_BSTR,xducer::StringXducer>::toNative,			BasicArrayXducer<VT_BSTR,xducer::StringXducer>::toJava,			BasicArrayXducer<VT_BSTR,xducer::StringXducer>::getJavaElement,			BasicArrayXducer<VT_BSTR,xducer::StringXducer>::setNativeElement},
	{ &objectArray,		VT_VARIANT,	BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toNative,		BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toJava,		BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::getJavaElement,		BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::setNativeElement},
	{ &variantArray,	VT_VARIANT,	BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toNative,		BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toJava,		BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::getJavaElement,		BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::setNativeElement},
	{ &objectArray,		VT_UNKNOWN,	BasicArrayXducer<VT_UNKNOWN,xducer::Com4jObjectXducer>::toNative,	BasicArrayXducer<VT_UNKNOWN,xducer::Com4jObjectXducer>::toJava,	BasicArrayXducer<VT_UNKNOWN,xducer::Com4jObjectXducer>::getJavaElement,	BasicArrayXducer<VT_UNKNOWN,xducer::Com4jObjectXducer>::setNativeElement},
	{ &objectArray,		VT_DISPATCH,BasicArrayXducer<VT_DISPATCH,xducer::Com4jObjectXducer>::toNative,	BasicArrayXducer<VT_DISPATCH,xducer::Com4jObjectXducer>::toJava,BasicArrayXducer<VT_DISPATCH,xducer::Com4jObjectXducer>::getJavaElement,BasicArrayXducer<VT_DISPATCH,xducer::Com4jObjectXducer>::setNativeElement},
	{ NULL, NULL, NULL, NULL }
};


pair<SafeArrayXducer::NativeType,VARTYPE> SafeArrayXducer::toNative2(
	JNIEnv* env, SafeArrayXducer::JavaType a ) {


	jclass clz = env->GetObjectClass(a);

	for( Entry* e=entries; e->clazz!=NULL; e++ ) {
		if(env->IsSameObject(clz,*(e->clazz)))
			return pair<SAFEARRAY*,VARTYPE>( (e->toNative)(env,a), e->vt );
	}
	return pair<SAFEARRAY*,VARTYPE>(NULL,VT_EMPTY);
}

SafeArrayXducer::JavaType SafeArrayXducer::toJava( JNIEnv* env, SAFEARRAY* value ) {
	WORD feature = value->fFeatures;

	if((feature&FADF_BSTR)!=0)
		return BasicArrayXducer<VT_BSTR,xducer::StringXducer>::toJava(env,value);
	if((feature&(FADF_UNKNOWN|FADF_DISPATCH))!=0)
		return BasicArrayXducer<VT_UNKNOWN,xducer::Com4jObjectXducer>::toJava(env,value);
	if((feature&FADF_VARIANT)!=0)
		return BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toJava(env,value);
	if((feature&FADF_HAVEVARTYPE) != 0)
	{
	    VARTYPE elemType = HIWORD(value->cLocks)&VT_TYPEMASK;
		for( Entry* e=entries; e->clazz!=NULL; e++ ) {
			if(elemType==e->vt)
				return e->toJava(env,value);
		}
	}
	return NULL;
}

JNIEXPORT void JNICALL Java_com4j_SafeArray_releaseArray(JNIEnv* env, jclass _, jlong ptr)
{
	HRESULT hr = SafeArrayDestroy(reinterpret_cast<SAFEARRAY*>(ptr));
	if(FAILED(hr))
		error(env,__FILE__,__LINE__,hr,"failed to destroy array");
}

JNIEXPORT jlong JNICALL Java_com4j_SafeArray_create0(JNIEnv* env, jclass _, jint jvarType, jintArray jbounds)
{
	jint* boundAry = env->GetIntArrayElements(jbounds, NULL);
	int nBounds = env->GetArrayLength(jbounds) / 2;
	SAFEARRAYBOUND* pBounds = new SAFEARRAYBOUND[nBounds];
	for(int i = 0; i < nBounds; i++) {
		pBounds[i].cElements = boundAry[i * 2 + 1] - boundAry[i * 2 + 0];
		pBounds[i].lLbound = boundAry[i * 2 + 0];
	}

	SAFEARRAY* psa = SafeArrayCreate(static_cast<VARTYPE>(jvarType), nBounds, pBounds);
	delete[] pBounds;

	if(!psa)
		error(env,__FILE__,__LINE__,"failed to create array");

	env->ReleaseIntArrayElements(jbounds, boundAry, JNI_ABORT);

	return reinterpret_cast<jlong>(psa);
}

JNIEXPORT jlong JNICALL Java_com4j_SafeArray_createAndInit0(JNIEnv* env, jclass _, jint jvarType, jobject jData)
{
	jarray jAry = reinterpret_cast<jarray>(jData);
	if(jvarType)
	{
		for( Entry* e=entries; e->vt!=NULL; e++ ) {
			if(e->vt == jvarType)
				return reinterpret_cast<jlong>((e->toNative)(env,jAry));
		}
	}
	else
	{
		jclass clz = env->GetObjectClass(jData);
		for( Entry* e=entries; e->clazz!=NULL; e++ ) {
			if(env->IsSameObject(clz,*(e->clazz)))
				return reinterpret_cast<jlong>((e->toNative)(env,jAry));
		}
	}

	error(env,__FILE__,__LINE__,"failed to create array:  unknown data type");
	return 0;
}

JNIEXPORT jintArray JNICALL Java_com4j_SafeArray_getBounds0(JNIEnv* env, jobject jo)
{
	HRESULT hr;
	LONG bound;
	SAFEARRAY* psa = reinterpret_cast<SAFEARRAY*>(env->GetLongField(jo, com4jSafeArray_ptr));
	int nDim = SafeArrayGetDim(psa);
	jintArray ary = env->NewIntArray(nDim * 2);
	jint* aryData = env->GetIntArrayElements(ary, NULL);
	for(int i = 0; i < nDim; i++) {
		hr = SafeArrayGetLBound(psa, i + 1, &bound);
		if(FAILED(hr)) {
			error(env, __FILE__, __LINE__, hr, "Cannot get array lbound");
			return 0;
		}
		aryData[i * 2 + 0] = bound;

		hr = SafeArrayGetUBound(psa, i + 1, &bound);
		if(FAILED(hr)) {
			error(env, __FILE__, __LINE__, hr, "Cannot get array ubound");
			return 0;
		}
		aryData[i * 2 + 1] = bound;
	}
	env->ReleaseIntArrayElements(ary, aryData, 0);
	return ary;
}

JNIEXPORT jint JNICALL Java_com4j_SafeArray_getVarType0(JNIEnv* env, jobject jo)
{
	SAFEARRAY* psa = reinterpret_cast<SAFEARRAY*>(env->GetLongField(jo, com4jSafeArray_ptr));
	VARTYPE vt;
	HRESULT hr = SafeArrayGetVartype(psa, &vt);
	if(FAILED(hr))
		error(env, __FILE__, __LINE__, hr, "Cannot get var type");

	return static_cast<jint>(vt);
}

JNIEXPORT jobject JNICALL Java_com4j_SafeArray_get0(JNIEnv* env, jobject jo, jintArray jindexes)
{
	HRESULT hr;
	SAFEARRAY* psa = reinterpret_cast<SAFEARRAY*>(env->GetLongField(jo, com4jSafeArray_ptr));
	VARTYPE vt;
	hr = SafeArrayGetVartype(psa, &vt);
	if(FAILED(hr)) {
		error(env, __FILE__, __LINE__, hr, "Cannot get var type");
		return NULL;
	}
	
	for( Entry* e=entries; e->vt!=NULL; e++ ) {
		if(vt == e->vt)
			return e->getJavaElement(env, psa, jindexes);
	}

	error(env, __FILE__, __LINE__, "Cannot find conversion for vartype");

	return NULL;
}

JNIEXPORT void JNICALL Java_com4j_SafeArray_set0(JNIEnv* env, jobject jo, jobject jData, jintArray jindexes)
{
	HRESULT hr;
	SAFEARRAY* psa = reinterpret_cast<SAFEARRAY*>(env->GetLongField(jo, com4jSafeArray_ptr));
	VARTYPE vt;
	hr = SafeArrayGetVartype(psa, &vt);
	if(FAILED(hr)) {
		error(env, __FILE__, __LINE__, hr, "Cannot get var type");
		return;
	}
	
	for( Entry* e=entries; e->vt!=NULL; e++ ) {
		if(vt == e->vt)
			e->setNativeElement(env, psa, jindexes, jData);
	}
}

