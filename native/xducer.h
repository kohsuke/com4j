#pragma once
#include "jstring.h"

/*
	Transducers are basic unit of conversion between Java and Native Types.

	Basic form of a transducer

	class XDUCER {
	public:
		// used to support VARIANT and SAFEARRAY.
		// specifies the VARTYPE that this XDUCER works with.
		static const VARTYPE vt = VT_***;
	}
*/

namespace xducer {
	
	// transducer that doesn't invole any JNI method call.
	template < class NT, class JT >
	class IdentityXducer {
	public:
		typedef NT NativeType;
		typedef JT JavaType;

		static inline NativeType toNative( JNIEnv* env, JavaType value ) {
			return static_cast<NT>(value);
		}

		static inline JavaType toJava( JNIEnv* env, NativeType value ) {
			return static_cast<JT>(value);
		}
	};


	// transducer between Java boxed type and native primitive type
	template < class NT, JStaticMethodID* parseMethod, JMethodID* printMethod,
		NT (JNICALL JNIEnv::* jnifunc)(jobject,jmethodID,...) >
	class BoxXducer {
	public:
		typedef jobject JavaType;
		typedef NT NativeType;

		static JavaType toJava( JNIEnv* env, NativeType i ) {
			return env->CallStaticObjectMethod( parseMethod->getClazz(), *parseMethod, i );
		}
		static NativeType fromJava( JNIEnv* env, JavaType i ) {
			return (env->*jnifunc)(i,*printMethod);
		}
	};


	// String <-> BSTR.
	// Note that the toJava method doesn't clean up BSTR.
	class StringXducer {
	public:
		typedef BSTR NativeType;
		typedef jstring JavaType;

		static inline NativeType toNative( JNIEnv* env, JavaType value ) {
			JString v(env,value);
			return SysAllocString(v);
		}

		static inline JavaType toJava( JNIEnv* env, NativeType value ) {
			if(value==NULL)
				return NULL;
			return env->NewString(value,SysStringLen(value));
		}
	};


}