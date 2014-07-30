// com4j specific transducers
#pragma once
#include "xducer.h"
#include "com4j.h"
#include "java_id.h"
#include "variant.h"

namespace xducer {

	// object <-> VARIANT.
	class VariantXducer {
	public:
		typedef VARIANT NativeType;
		typedef jobject JavaType;

		static inline NativeType toNative( JNIEnv* env, JavaType value ) {
			std::auto_ptr<VARIANT> v(convertToVariant(env,value)); // need to be deleted after copy as return value
			return *v;
		}

		static inline JavaType toJava( JNIEnv* env, NativeType value ) {
			return variantToObject(env,javaLangObject,value);
		}
	};


	// Com4jObject <-> IUnknown*.
	class Com4jObjectXducer {
	public:
		typedef IUnknown* NativeType;
		typedef jobject JavaType;

		static inline NativeType toNative( JNIEnv* env, JavaType value ) {
			if(value==NULL)		return NULL;

			jlong p = com4j_COM4J_getPtr(env,value);
			NativeType ptr = reinterpret_cast<NativeType>(p);
			
			if(p==NULL)		return NULL;

			ptr->AddRef();
			return ptr;
		}

		static inline JavaType toJava( JNIEnv* env, NativeType value ) {
			if(value==NULL)	return NULL;
			value->AddRef();	// for the newly created object
			return com4jWrapper_new(env,reinterpret_cast<jlong>(value));
		}
	};
}
