#include "stdafx.h"
#include "com4j.h"

void error( JNIEnv* env, HRESULT hr, const char* msg ... ) {
	// format the message
	char w[1024];
	va_list va;
	va_start(va,msg);
	vsprintf(w,msg,va);

	env->Throw( (jthrowable)
		env->NewObject( comexception,
			env->GetMethodID(comexception,"<init>","(Ljava/lang/String;I)V"),
			env->NewStringUTF(w),
			hr )
	);
}

void error( JNIEnv* env, const char* msg ... ) {
	// format the message
	char w[1024];
	va_list va;
	va_start(va,msg);
	vsprintf(w,msg,va);

	env->Throw( (jthrowable)
		env->NewObject( comexception,
			env->GetMethodID(comexception,"<init>","(Ljava/lang/String;)V"),
			env->NewStringUTF(w) )
	);
}
