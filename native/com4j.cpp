#include "stdafx.h"
#include "java_id.h"
#include "com4j.h"
#include "com4j_native.h"
#include "jstring.h"
#include "typelib.h"
#include "safearray.h"

JNIEXPORT jobject JNICALL Java_com4j_Native_invoke(JNIEnv* env,
	jclass __unused,
	jint pComObject,		// pointer to the COM object
	jint pFuncIndex,		// which function are we trying to call?
	jobjectArray args,		// arguments
	jintArray _convs,		// conversions
	jclass returnType,		// expected Java return type
	jint returnIndex,		// index of the return value in the parameter list
	jboolean returnIsInOut,	// true if the return type also shows up in the paramter list
	jint returnConv			// conversion of the return type
) {
	Environment e(env);
	jint* convs =  env->GetIntArrayElements(_convs,NULL);
	jobject r = e.invoke(
		reinterpret_cast<void*>(pComObject),
		(*reinterpret_cast<VTable*>(pComObject))[pFuncIndex],
		args,
		convs,
		returnType, returnIndex, returnIsInOut!=0,
		returnConv );
	env->ReleaseIntArrayElements(_convs,convs,0);
	return r;
}

extern "C"
JNIEXPORT jint JNICALL JVM_OnLoad(JavaVM* jvm, const char* options, void* reserved) {
	return JNI_OK;
}

JNIEXPORT void JNICALL Java_com4j_Native_init( JNIEnv* env, jclass __unused__ ) {
	JClassID::runInit(env);
	JMethodID_Base::runInit(env);
	com4j_Holder_value = env->GetFieldID(com4j_Holder,"value","Ljava/lang/Object;");

	HRESULT hr = CoInitialize(NULL);
	if(FAILED(hr)) {
		error(env,hr,"failed to initialize COM");
		return;
	}
}

extern "C"
static IUnknown* toComObject( jint pComObject ) {
	return reinterpret_cast<IUnknown*>(pComObject);
}

JNIEXPORT void JNICALL Java_com4j_Native_release( JNIEnv* env, jclass __unused__, jint pComObject ) {
	toComObject(pComObject)->Release();
}

JNIEXPORT jint JNICALL Java_com4j_Native_queryInterface( JNIEnv* env, jclass __unused__,
	jint pComObject, jlong iid1, jlong iid2 ) {
	
	MyGUID iid(iid1,iid2);
	void* p;
	HRESULT hr = toComObject(pComObject)->QueryInterface(iid,&p);
	if(FAILED(hr)) {
		error(env,hr,"failed to query interface");
		return 0;
	}
	return reinterpret_cast<jint>(p);
}

JNIEXPORT jint JNICALL Java_com4j_Native_createInstance(
	JNIEnv* env, jclass __unused__, jstring _progId, jlong iid1, jlong iid2 ) {
	
	MyGUID iid(iid1,iid2);
	CLSID clsid;
	HRESULT hr;
	JString progId(env,_progId);

	hr = CLSIDFromProgID(progId,&clsid);
	if(FAILED(hr)) {
		if(FAILED(CLSIDFromString( const_cast<LPOLESTR>(LPCOLESTR(progId)),&clsid))) {
			error(env,hr,"Unrecognized CLSID");
			return 0;
		}
	}

	void* p;
	hr = CoCreateInstance(clsid,NULL,CLSCTX_ALL,iid,&p);
	if(FAILED(hr)) {
		error(env,hr,"CoCreateInstance failed");
		return 0;
	}
	return reinterpret_cast<jint>(p);
}

/*
JNIEXPORT jint JNICALL Java_com4j_Native_loadTypeLibrary(
	JNIEnv* env, jclass __unused__, jstring _name ) {

	JString name(env,_name);
	ITypeLib* pLib=NULL;

	HRESULT hr = LoadTypeLib(name,&pLib);
	if(FAILED(hr)) {
		error(env,"LoadTypeLib failed",hr);
		return 0;
	}

	return reinterpret_cast<jint>(pLib);
}
*/

JNIEXPORT jint JNICALL Java_com4j_Native_loadTypeLibrary(
	JNIEnv* env, jclass __unused__, jstring _name ) {

	JString name(env,_name);
	ITypeLib* pLib=NULL;

	HRESULT hr = LoadTypeLib(name,&pLib);
	if(FAILED(hr)) {
		error(env,hr,"LoadTypeLib failed");
		return 0;
	}
	
	return reinterpret_cast<jint>(CTypeLib::create(pLib));
}
