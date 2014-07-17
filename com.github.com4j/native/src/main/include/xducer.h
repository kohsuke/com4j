// transducer shared between nlink and com4j
#pragma once
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

#include "java_id.h"

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
	template < class NT, class JT, JStaticMethodID<jobject>* parseMethod, JMethodID<JT>* printMethod >
	class BoxXducer {
	public:
		typedef jobject JavaType;
		typedef NT NativeType;

		static JavaType toJava( JNIEnv* env, NativeType i ) {
			return (*parseMethod)(env,static_cast<JT>(i));
		}
		static NativeType toNative( JNIEnv* env, JavaType i ) {
			return static_cast<NT>((*printMethod)(env,i));
		}
	};

	// java.lang.Boolean <-> BOOL (int)
	class BoxedBoolXducer {
	public:
		typedef jobject JavaType;
		typedef BOOL NativeType;
		static JavaType toJava( JNIEnv* env, NativeType i ) {
			return javaLangBoolean_valueOf(env, (i!=0)?JNI_TRUE:JNI_FALSE );
		}
		static NativeType toNative( JNIEnv* env, JavaType i ) {
			jboolean b = javaLangBoolean_booleanValue(env,i);
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
			return javaLangBoolean_valueOf(env, (i!=0)?JNI_TRUE:JNI_FALSE );
		}
		static NativeType toNative( JNIEnv* env, JavaType i ) {
			jboolean b = javaLangBoolean_booleanValue(env,i);
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
			return env->NewString((jchar*)value,SysStringLen(value));
		}
	};

	// ULONGLONG <-> java.math.BigInteger
	class BigIntegerXducer {
	public:
		typedef ULONGLONG NativeType;
		typedef jobject JavaType;

		static inline NativeType toNative( JNIEnv* env, JavaType value ) {
			if(value==NULL)
				return 0;

			ULONGLONG val = 0;
			sscanf(JString(env,javaMathBigInteger_toString(env,value)),"%Lu",&val);
			return val;
		}

		static inline JavaType toJava( JNIEnv* env, NativeType value ) {
			char w[128];
			sprintf(w,"%Lu",value);
			return javaMathBigInteger_new(env,env->NewStringUTF(w));
		}
	};


	typedef BoxXducer<byte,jbyte,&javaLangByte_valueOf, &javaLangNumber_byteValue >
		BoxedByteXducer;

	typedef BoxXducer<float,jfloat,&javaLangFloat_valueOf, &javaLangNumber_floatValue >
		BoxedFloatXducer;

	typedef BoxXducer<double,jdouble,&javaLangDouble_valueOf, &javaLangNumber_doubleValue >
		BoxedDoubleXducer;

	typedef BoxXducer<short,jshort, &javaLangShort_valueOf, &javaLangNumber_shortValue >
		BoxedShortXducer;

	typedef BoxXducer<long,jint, &javaLangInteger_valueOf, &javaLangNumber_intValue >
		BoxedIntXducer;

	typedef BoxXducer<INT64,jlong, &javaLangLong_valueOf, &javaLangNumber_longValue >
		BoxedLongXducer;
}