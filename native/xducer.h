#pragma once
#include "jstring.h"

/*
	Transducers are basic unit of conversion between Java and Native Types.

	Basic form of a transducer

	class XDUCER {
	public:
		typedef ... NativeType;	// the native type
		typedef ... JavaType;   // the java type
		
		static NativeType toNative( JNIEnv* env, JavaType value ) {}
		static JavaType toJava( JNIEnv* env, JavaType value ) {}
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
		static NativeType toNative( JNIEnv* env, JavaType i ) {
			return (env->*jnifunc)(i,*printMethod);
		}
	};

	// java.lang.Boolean <-> BOOL (int)
	class BoxedBoolXducer {
	public:
		typedef jobject JavaType;
		typedef BOOL NativeType;
		static JavaType toJava( JNIEnv* env, NativeType i ) {
			return env->CallStaticObjectMethod( javaLangBoolean, javaLangBoolean_valueOf, (i!=0)?JNI_TRUE:JNI_FALSE );
		}
		static NativeType toNative( JNIEnv* env, JavaType i ) {
			jboolean b = env->CallBooleanMethod(i,javaLangBoolean_booleanValue);
			if(b==0)	return 0;	// false
			else		return -1;	// true
		}
	};

	// java.lang.Boolean <-> VARIANT_BOOL (short) VARIANT_TRUE/VARIANT_FALSE
	class BoxedVariantBoolXducer {
	public:
		typedef jobject JavaType;
		typedef VARIANT_BOOL NativeType;
		static JavaType toJava( JNIEnv* env, NativeType i ) {
			return env->CallStaticObjectMethod( javaLangBoolean, javaLangBoolean_valueOf, (i!=0)?JNI_TRUE:JNI_FALSE );
		}
		static NativeType toNative( JNIEnv* env, JavaType i ) {
			jboolean b = env->CallBooleanMethod(i,javaLangBoolean_booleanValue);
			if(b==0)	return VARIANT_FALSE;
			else		return VARIANT_TRUE;
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

	typedef BoxXducer<float,&javaLangFloat_valueOf, &javaLangNumber_floatValue, JNIEnv::CallFloatMethod >
		BoxedFloatXducer;

	typedef BoxXducer<double,&javaLangDouble_valueOf, &javaLangNumber_doubleValue, JNIEnv::CallDoubleMethod >
		BoxedDoubleXducer;

	typedef BoxXducer<short, &javaLangShort_valueOf, &javaLangNumber_shortValue, JNIEnv::CallShortMethod >
		BoxedShortXducer;

	typedef BoxXducer< long/*32bit*/, &javaLangInteger_valueOf, &javaLangNumber_intValue, JNIEnv::CallIntMethod>
		BoxedIntXducer;

	typedef BoxXducer< INT64, &javaLangLong_valueOf, &javaLangNumber_longValue, JNIEnv::CallLongMethod>
		BoxedLongXducer;
}