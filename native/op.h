#pragma once
//
//
// Helper classes to invoke JNI methods in a more type-safe manner.
//
//

namespace op {

	// specialized for each return type.
	template <class JavaType>
	class Op {
	};

	// default implementation for Op
	template <class JT,
		JT (JNIEnv::* callMethod)( jobject, jmethodID, va_list ) >
	class Basic_Op {
	public:
		static JT invoke( JNIEnv* env, jobject o, jmethodID id, ... ) {
			va_list args;
			va_start(args,env);
			JT r = (env->*callMethod)(o,id,args);
			va_end(args);
			return r;
		}
		static JT invokeV( JNIEnv* env, jobject o, jmethodID id, va_list args ) {
			return (env->*callMethod)(o,id,args);
		}
	};

	class Op<jboolean> : public Basic_Op<jboolean, JNIEnv::CallBooleanMethodV> {};
	class Op<jint> : public Basic_Op<jint, JNIEnv::CallIntMethodV> {};
	class Op<jshort> : public Basic_Op<jshort, JNIEnv::CallShortMethodV> {};
	class Op<jbyte> : public Basic_Op<jbyte, JNIEnv::CallByteMethodV> {};
	class Op<jlong> : public Basic_Op<jlong, JNIEnv::CallLongMethodV> {};
	class Op<jfloat> : public Basic_Op<jfloat, JNIEnv::CallFloatMethodV> {};
	class Op<jdouble> : public Basic_Op<jdouble, JNIEnv::CallDoubleMethodV> {};
	class Op<jobject> : public Basic_Op<jobject, JNIEnv::CallObjectMethodV> {};

}