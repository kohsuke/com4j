#pragma once

extern JNIEnv* jniEnv;

template <typename T/*something that derives from jobject*/>
class GlobalRef {
private:
	T obj;

public:
	GlobalRef(T obj) : obj((T)jniEnv->NewGlobalRef(obj)) {
	}
	~GlobalRef() {
		jniEnv->DeleteGlobalRef(obj);
		obj = NULL;
	}
	operator T () {
		return obj;
	}
};