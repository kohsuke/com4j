#pragma once
//
//
// Type definitions that allow polymorhpic operations to Java arrays.
//
//

namespace array {


template <class Primitive>
class Array {};



class Array<jboolean> {
public:
	typedef jbooleanArray ARRAY;
	static ARRAY newArray(JNIEnv* env,jsize len) {
		return env->NewBooleanArray(len);
	}
	static jboolean* lock(JNIEnv* env,ARRAY a) {
		return env->GetBooleanArrayElements(a,NULL);
	}
	static void unlock(JNIEnv* env,ARRAY a,jboolean* buf) {
		env->ReleaseBooleanArrayElements(a,buf,0);
	}
};

class Array<jbyte> {
public:
	typedef jbyteArray ARRAY;
	static ARRAY newArray(JNIEnv* env,jsize len) {
		return env->NewByteArray(len);
	}
	static jbyte* lock(JNIEnv* env,ARRAY a) {
		return env->GetByteArrayElements(a,NULL);
	}
	static void unlock(JNIEnv* env,ARRAY a,jbyte* buf) {
		env->ReleaseByteArrayElements(a,buf,0);
	}
};

class Array<jchar> {
public:
	typedef jcharArray ARRAY;
	static ARRAY newArray(JNIEnv* env,jsize len) {
		return env->NewCharArray(len);
	}
	static jchar* lock(JNIEnv* env,ARRAY a) {
		return env->GetCharArrayElements(a,NULL);
	}
	static void unlock(JNIEnv* env,ARRAY a,jchar* buf) {
		env->ReleaseCharArrayElements(a,buf,0);
	}
};

class Array<jshort> {
public:
	typedef jshortArray ARRAY;
	static ARRAY newArray(JNIEnv* env,jsize len) {
		return env->NewShortArray(len);
	}
	static jshort* lock(JNIEnv* env,ARRAY a) {
		return env->GetShortArrayElements(a,NULL);
	}
	static void unlock(JNIEnv* env,ARRAY a,jshort* buf) {
		env->ReleaseShortArrayElements(a,buf,0);
	}
};

class Array<jint> {
public:
	typedef jintArray ARRAY;
	static ARRAY newArray(JNIEnv* env,jsize len) {
		return env->NewIntArray(len);
	}
	static jint* lock(JNIEnv* env,ARRAY a) {
		return env->GetIntArrayElements(a,NULL);
	}
	static void unlock(JNIEnv* env,ARRAY a,jint* buf) {
		env->ReleaseIntArrayElements(a,buf,0);
	}
};

class Array<jlong> {
public:
	typedef jlongArray ARRAY;
	static ARRAY newArray(JNIEnv* env,jsize len) {
		return env->NewLongArray(len);
	}
	static jlong* lock(JNIEnv* env,ARRAY a) {
		return env->GetLongArrayElements(a,NULL);
	}
	static void unlock(JNIEnv* env,ARRAY a,jlong* buf) {
		env->ReleaseLongArrayElements(a,buf,0);
	}
};

class Array<jfloat> {
public:
	typedef jfloatArray ARRAY;
	static ARRAY newArray(JNIEnv* env,jsize len) {
		return env->NewFloatArray(len);
	}
	static jfloat* lock(JNIEnv* env,ARRAY a) {
		return env->GetFloatArrayElements(a,NULL);
	}
	static void unlock(JNIEnv* env,ARRAY a,jfloat* buf) {
		env->ReleaseFloatArrayElements(a,buf,0);
	}
};

class Array<jdouble> {
public:
	typedef jdoubleArray ARRAY;
	static ARRAY newArray(JNIEnv* env,jsize len) {
		return env->NewDoubleArray(len);
	}
	static jdouble* lock(JNIEnv* env,ARRAY a) {
		return env->GetDoubleArrayElements(a,NULL);
	}
	static void unlock(JNIEnv* env,ARRAY a,jdouble* buf) {
		env->ReleaseDoubleArrayElements(a,buf,0);
	}
};



}