#include "stdafx.h"
#include "java_id.h"
#include "com4j.h"
#include "com4j_native.h"
#include "typelib.h"
#include "safearray.h"
#include "eventReceiver.h"

// MsgWaitForMultipleObjects

JavaVM* jvm;

JNIEXPORT jobject JNICALL Java_com4j_Native_invoke(JNIEnv* env,
	jclass __unused,
	jint pComObject,		// pointer to the COM object
	jint pFuncIndex,		// which function are we trying to call?
	jobjectArray args,		// arguments
	jintArray _convs,		// conversions
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
		returnIndex, returnIsInOut!=0,
		returnConv );
	env->ReleaseIntArrayElements(_convs,convs,0);
	return r;
}

JNIEXPORT jobject JNICALL Java_com4j_Native_invokeDispatch( JNIEnv* env, jclass _, jint pComObject, jint dispId, jint flag, jobjectArray args) {
	
	DISPPARAMS params;
	DISPID dispIdPropertyPut = DISPID_PROPERTYPUT;
	
	params.cArgs = env->GetArrayLength(args);
	params.cNamedArgs = 0;
	params.rgdispidNamedArgs = NULL;
	VARIANT* p = new VARIANT[params.cArgs];
	params.rgvarg = p;

	for( int i=0; i<params.cArgs; i++ ) {
		VARIANT* v = convertToVariant(env,env->GetObjectArrayElement(args,i));
		if(v==NULL) {
			// VariantInit(&p[i]);
			p[i] = vtMissing;
		} else {
			p[i] = *v;	// just transfer the ownership
			delete v;
		}
	}

	if(flag==DISPATCH_PROPERTYPUT || flag==DISPATCH_PROPERTYPUTREF) {
		params.cNamedArgs = 1;
		params.rgdispidNamedArgs = &dispIdPropertyPut;
	}

	EXCEPINFO excepInfo;
	memset(&excepInfo,0,sizeof(EXCEPINFO));

	jobject retVal = com4j_Variant_new(env);

	HRESULT hr = reinterpret_cast<IDispatch*>(pComObject)->Invoke(
		dispId, IID_NULL, 0, flag, &params, com4jVariantToVARIANT(env,retVal), &excepInfo, NULL );

	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"Invocation failed: %s",(LPCSTR)_bstr_t(excepInfo.bstrDescription));
	}

	delete p;

	return retVal;
}

JNIEXPORT void JNICALL Java_com4j_Native_init( JNIEnv* env, jclass __unused__ ) {
	HRESULT hr = CoInitialize(NULL);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"failed to initialize COM");
		return;
	}
}

extern "C"
static IUnknown* toComObject( jint pComObject ) {
	return reinterpret_cast<IUnknown*>(pComObject);
}

JNIEXPORT void JNICALL Java_com4j_Native_addRef( JNIEnv* env, jclass __unused__, jint pComObject ) {
	toComObject(pComObject)->AddRef();
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
		return 0;
	}
	return reinterpret_cast<jint>(p);
}

JNIEXPORT jint JNICALL Java_com4j_Native_createInstance(
	JNIEnv* env, jclass __unused__, jstring _progId, jint clsctx, jlong iid1, jlong iid2 ) {
	
	MyGUID iid(iid1,iid2);
	CLSID clsid;
	HRESULT hr;
	JString progId(env,_progId);

	hr = CLSIDFromProgID(progId,&clsid);
	if(FAILED(hr)) {
		if(FAILED(CLSIDFromString( const_cast<LPOLESTR>(LPCOLESTR(progId)),&clsid))) {
			error(env,__FILE__,__LINE__,hr,"Unrecognized CLSID");
			return 0;
		}
	}

	void* p;

	if(clsctx&(CLSCTX_LOCAL_SERVER|CLSCTX_REMOTE_SERVER)) {
		IUnknown* pUnk = NULL;
		hr = CoCreateInstance(clsid,NULL,clsctx,__uuidof(IUnknown),(void**)&pUnk);
		if(FAILED(hr)) {
			error(env,__FILE__,__LINE__,hr,"CoCreateInstance failed");
			return 0;
		}
		hr = OleRun(pUnk);
		if(FAILED(hr)) {
			error(env,__FILE__,__LINE__,hr,"OleRun failed");
			return 0;
		}
		hr = pUnk->QueryInterface(iid,&p);
		if(FAILED(hr)) {
			error(env,__FILE__,__LINE__,hr,"QueryInterface failed");
			return 0;
		}

	} else {
		// just the plain CoCreateInstance
		hr = CoCreateInstance(clsid,NULL,clsctx,iid,&p);
		if(FAILED(hr)) {
			error(env,__FILE__,__LINE__,hr,"CoCreateInstance failed");
			return 0;
		}
	}
	return reinterpret_cast<jint>(p);
}

JNIEXPORT jint JNICALL Java_com4j_Native_getActiveObject(
	JNIEnv* env, jclass __unused__, jlong clsid1, jlong clsid2) {

	MyGUID clsid(clsid1,clsid2);
	HRESULT hr;
	
	IUnknown* pUnk = NULL;

	hr = ::GetActiveObject(clsid,NULL,&pUnk);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"GetActiveObject failed");
		return 0;
	}

	return reinterpret_cast<jint>(pUnk);
}

JNIEXPORT jstring JNICALL Java_com4j_Native_getErrorMessage(
	JNIEnv* env, jclass __unused__, jint hresult) {

	LPWSTR p;

	DWORD r = FormatMessageW(
		FORMAT_MESSAGE_ALLOCATE_BUFFER|
		FORMAT_MESSAGE_FROM_SYSTEM,
		NULL, hresult,
		0, reinterpret_cast<LPWSTR>(&p), 0, NULL );

	if(r==0)
		return NULL;	// failed

	// trim off the trailing NL
	int len = wcslen(p);
	while(len>0 && (p[len-1]==L'\r' || p[len-1]==L'\n'))
		len--;

	jstring result = env->NewString(p,len);
	LocalFree(p);

	return result;
}

JNIEXPORT jint JNICALL Java_com4j_Native_getErrorInfo(
	JNIEnv* env, jclass __unused__, jint pComObject, jlong iid1, jlong iid2) {

	MyGUID iid(iid1,iid2);

	ISupportErrorInfoPtr p(reinterpret_cast<IUnknown*>(pComObject));
	if(p==NULL)
		return 0;	// not supported

	HRESULT hr = p->InterfaceSupportsErrorInfo(iid);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"ISupportErrorInfo::InterfaceSupportsErrorInfo failed");
		return 0;
	}

	if(hr!=S_OK)	return 0; // not supported

	IErrorInfo* pError;
	hr = GetErrorInfo(0,&pError);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"GetErrorInfo failed");
		return 0;
	}

	// return the pointer
	return reinterpret_cast<jint>(pError);
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
		error(env,__FILE__,__LINE__,hr,"LoadTypeLib failed");
		return 0;
	}
	
	return reinterpret_cast<jint>(typelib::CTypeLib::get(pLib));
}

JNIEXPORT void JNICALL Java_com4j_Native_coInitialize(
	JNIEnv* env, jclass _) {

	HRESULT hr = CoInitialize(NULL);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"CoInitialize failed");
	}	
}

JNIEXPORT void JNICALL Java_com4j_Native_coUninitialize(
	JNIEnv* env, jclass _) {

	CoUninitialize();
}

JNIEXPORT jint JNICALL Java_com4j_Native_advise( JNIEnv* env, jclass _, jint ptr, jobject proxy,jlong iid1, jlong iid2) {
	CEventReceiver* p = CEventReceiver::create( env, reinterpret_cast<IConnectionPoint*>(ptr), proxy, MyGUID(iid1,iid2) );
	return reinterpret_cast<jint>(p);
}

JNIEXPORT void JNICALL Java_com4j_Native_unadvise( JNIEnv* env, jclass _, jint p ) {
	CEventReceiver* er = reinterpret_cast<CEventReceiver*>(p);
	er->Disconnect(env);
	delete er;
}

JNIEXPORT jobject JNICALL Java_com4j_Native_createBuffer(JNIEnv* env, jclass _, jint ptr, jint size) {
	return env->NewDirectByteBuffer(reinterpret_cast<void*>(ptr),size);
}
