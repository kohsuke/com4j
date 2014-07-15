#pragma once

// obtains the VARIANT* that the given com4j.Variant object owns
inline VARIANT* com4jVariantToVARIANT( JNIEnv* pEnv, jobject var ) {
	return (VARIANT*)pEnv->GetDirectBufferAddress(com4j_Variant_image(pEnv,var));
}

// convert a java object to a VARIANT based on the actual type of the Java object.
// the caller should call VariantClear to clean up the data, then delete it.
//
// return NULL if fails to convert
VARIANT* convertToVariant( JNIEnv* env, jobject o );

// convert a VARIANT to a Java object based on its type.
//
// return -1 if it fails.
jobject variantToObject( JNIEnv* env, jclass retType, VARIANT& v );