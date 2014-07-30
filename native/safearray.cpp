#include "stdafx.h"
#include "safearray.h"

using namespace safearray;

/**  
 * Original auther  (C) Kohsuke Kawaguchi (kk@kohsuke.org)
 * Modified by      (C) Michael Schnell (scm, 2008, Michael-Schnell@gmx.de)
 */

struct Entry {
	JClassID* clazz;
	VARTYPE vt;
	SAFEARRAY* (* toNative)( JNIEnv* env, jarray javaArray );
	jarray (* toJava)( JNIEnv* env, SAFEARRAY* nativeArray );
};

JClassID variantArray("[Lcom4j/Variant;");

#define TABLE_ENTRY_PRIMITIVE(type, VariantType, NativeType) \
{  &type##Array, VariantType, PrimitiveArrayXducer<VariantType,NativeType,j##type>::toNative, PrimitiveArrayXducer<VariantType,NativeType,j##type>::toJava }

// conversion table
static Entry entries[] = {
	TABLE_ENTRY_PRIMITIVE(boolean,	VT_BOOL,	VARIANT_BOOL),
	TABLE_ENTRY_PRIMITIVE(byte,		VT_UI1,		byte),
	TABLE_ENTRY_PRIMITIVE(char,		VT_UI2,		unsigned short),
	TABLE_ENTRY_PRIMITIVE(double,	VT_R8,		double),
	TABLE_ENTRY_PRIMITIVE(float,	VT_R4,		float),
	TABLE_ENTRY_PRIMITIVE(int,		VT_I4,		INT32),
	TABLE_ENTRY_PRIMITIVE(long,		VT_I8,		INT64),
	TABLE_ENTRY_PRIMITIVE(short,	VT_I2,		short),
	{ &stringArray,		VT_BSTR,	BasicArrayXducer<VT_BSTR,xducer::StringXducer>::toNative,		BasicArrayXducer<VT_BSTR,xducer::StringXducer>::toJava},
	{ &objectArray,		VT_VARIANT,	BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toNative,	BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toJava},
	{ &variantArray,	VT_VARIANT,	BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toNative,	BasicArrayXducer<VT_VARIANT,xducer::VariantXducer>::toJava},
	{ NULL, NULL, NULL }
};


pair<SafeArrayXducer::NativeType,VARTYPE> SafeArrayXducer::toNative2(
	JNIEnv* env, SafeArrayXducer::JavaType a ) {


	jclass clz = env->GetObjectClass(a);

	for( Entry* e=entries; e->clazz!=NULL; e++ ) {
		if(env->IsSameObject(clz,*(e->clazz)))
			return pair<SAFEARRAY*,VARTYPE>( (e->toNative)(env,a), e->vt );
	}

	// Try as multi dimensinal array
	if (env->IsInstanceOf(a, objectArray)) { 
		return pair<SAFEARRAY*,VARTYPE>(
			MultiDimArrayXducer<xducer::VariantXducer>::toNative(env, a),
			VT_VARIANT);
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
		VARTYPE elemType;
		SafeArrayGetVartype(value, &elemType);
		for( Entry* e=entries; e->clazz!=NULL; e++ ) {
			if(elemType==e->vt)
				return e->toJava(env,value);
		}
	}
	return NULL;
}
