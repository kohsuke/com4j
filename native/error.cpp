#include "stdafx.h"
#include "com4j.h"

void error( JNIEnv* env, const char* file, int line, HRESULT hr, const char* msg ... ) {
	// format the message
	va_list va;
	va_start(va,msg);

	int len = _vscprintf(msg,va);
	char* w = reinterpret_cast<char*>(alloca(len+1)); // +1 for '\0'
	vsprintf(w,msg,va);

	env->ExceptionClear();
	env->Throw( (jthrowable)comexception_new_hr( env, env->NewStringUTF(w), hr, env->NewStringUTF(file), line ) );
}

void error( JNIEnv* env, const char* file, int line, const char* msg ... ) {
	// format the message
	va_list va;
	va_start(va,msg);

	int len = _vscprintf(msg,va);
	char* w = reinterpret_cast<char*>(alloca(len+1)); // +1 for '\0'
	vsprintf(w,msg,va);

	env->ExceptionClear();
	env->Throw( (jthrowable)comexception_new( env, env->NewStringUTF(w), env->NewStringUTF(file), line ) );
}
