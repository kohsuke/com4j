#include "stdafx.h"
#include "java_id.h"
#include "com4j.h"
#include "com4j_native.h"
#include "typelib.h"
#include "safearray.h"
#include "eventReceiver.h"

/**  
 * Original auther  (C) Kohsuke Kawaguchi (kk@kohsuke.org)
 * Modified by      (C) Michael Schnell (scm, 2008, Michael-Schnell@gmx.de)
 */

// MsgWaitForMultipleObjects

JavaVM* jvm;

JNIEXPORT jobject JNICALL Java_com4j_Native_invoke(JNIEnv* env,
	jclass __unused,
	jlong pComObject,		// pointer to the COM object
	jlong pFuncIndex,		// which function are we trying to call?
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

JNIEXPORT jobject JNICALL Java_com4j_Native_invokeDispatch( JNIEnv* env, jclass _, jlong pComObject, jint dispId, jint flag, jobjectArray args) {
	
	DISPPARAMS params;
	DISPID dispIdPropertyPut = DISPID_PROPERTYPUT;
	
	params.cArgs = env->GetArrayLength(args);
	params.cNamedArgs = 0;
	params.rgdispidNamedArgs = NULL;
	VARIANT* p = new VARIANT[params.cArgs];
	params.rgvarg = p;

	for(unsigned int i=0; i<params.cArgs; i++ ) {
    // we have to store the params in reverse order! (scm)
    int destIndex = params.cArgs - 1 - i; 
		VARIANT* v = convertToVariant(env,env->GetObjectArrayElement(args,i));
		if(v==NULL) {
			// VariantInit(&p[i]);
			p[destIndex] = vtMissing;
		} else {
			p[destIndex] = *v;	// just transfer the ownership
			delete v;
		}
	}

  // see MSDN IDispatch::Invoke
  // "When you use IDispatch::Invoke() with DISPATCH_PROPERTYPUT or DISPATCH_PROPERTYPUTREF, you have to specially
  //  initialize the cNamedArgs and rgdispidNamedArgs elements of your DISPPARAMS structure with the following:"
	if(flag==DISPATCH_PROPERTYPUT || flag==DISPATCH_PROPERTYPUTREF) {
		params.cNamedArgs = 1;
		params.rgdispidNamedArgs = &dispIdPropertyPut;
	}

	EXCEPINFO excepInfo;
	memset(&excepInfo,0,sizeof(EXCEPINFO));

	jobject retVal = com4j_Variant_new(env);

	HRESULT hr = reinterpret_cast<IDispatch*>(pComObject)->Invoke(
		dispId, IID_NULL, 0, (WORD) flag, &params, com4jVariantToVARIANT(env,retVal), &excepInfo, NULL );

	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"Invocation failed: %s",(LPCSTR)_bstr_t(excepInfo.bstrDescription));
	}

	delete[] p;

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
static IUnknown* toComObject( jlong pComObject ) {
	return reinterpret_cast<IUnknown*>(pComObject);
}

JNIEXPORT jint JNICALL Java_com4j_Native_addRef( JNIEnv* env, jclass __unused__, jlong pComObject ) {
	return toComObject(pComObject)->AddRef();
}

JNIEXPORT jint JNICALL Java_com4j_Native_release( JNIEnv* env, jclass __unused__, jlong pComObject ) {
	IUnknown *ptr = toComObject(pComObject);
	if(ptr != NULL) {
		return ptr->Release();
	} else {
        // Throw a NullPointerException!
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "IUnknown pointer is NULL");
        return 0; // doesn't matter what we return
  }
}

JNIEXPORT jlong JNICALL Java_com4j_Native_queryInterface( JNIEnv* env, jclass __unused__,
	jlong pComObject, jlong iid1, jlong iid2 ) {
	
	MyGUID iid(iid1,iid2);
	void* p;

	HRESULT hr = toComObject(pComObject)->QueryInterface(iid,&p);
	if(FAILED(hr)) {
		return 0;
	}
	return reinterpret_cast<jlong>(p);
}

JNIEXPORT jlong JNICALL Java_com4j_Native_createInstance(
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
			pUnk->Release();
			error(env,__FILE__,__LINE__,hr,"OleRun failed");
			return 0;
		}
		hr = pUnk->QueryInterface(iid,&p);
		pUnk->Release();

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
	return reinterpret_cast<jlong>(p);
}

JNIEXPORT jlong JNICALL Java_com4j_Native_getActiveObject(
	JNIEnv* env, jclass __unused__, jlong clsid1, jlong clsid2) {

	MyGUID clsid(clsid1,clsid2);
	HRESULT hr;
	
	IUnknown* pUnk = NULL;

	hr = ::GetActiveObject(clsid,NULL,&pUnk);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"GetActiveObject failed");
		return 0;
	}

	return reinterpret_cast<jlong>(pUnk);
}

JNIEXPORT jlong JNICALL Java_com4j_Native_getObject(
	JNIEnv* env, jclass __unused__, jstring _fileName, jstring _progId) {

	HRESULT hr;

	if(_progId==NULL) {
		// case 1: just file name
		IBindCtxPtr pbc;
		ULONG cEaten;
		IMonikerPtr pmk;
		IDispatch* pDisp;

		hr = CreateBindCtx(NULL,&pbc);
		if(FAILED(hr)) {
			error(env,__FILE__,__LINE__,hr,"Failed to CreateBindCtx");
			return 0;
		}
		hr = MkParseDisplayName(pbc,JString(env,_fileName),&cEaten,&pmk);
		if(FAILED(hr)) {
			error(env,__FILE__,__LINE__,hr,"Failed to MkParseDisplayName");
			return 0;
		}
		hr = BindMoniker(pmk,0,__uuidof(IDispatch),(LPVOID*)&pDisp);
		if(FAILED(hr)) {
			error(env,__FILE__,__LINE__,hr,"Failed to bind moniker");
			return 0;
		}

		return reinterpret_cast<jlong>(pDisp);
	}

	JString progId(env,_progId);
	CLSID clsid;
	IUnknown* pUnk=NULL;

	hr = CLSIDFromProgID(progId,&clsid);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"Unrecognized progID");
		return 0;
	}

	if(_fileName==NULL) {
		// case 2: just progId
		hr = GetActiveObject(clsid,NULL,&pUnk);
		if(FAILED(hr)) {
			error(env,__FILE__,__LINE__,hr,"Failed to GetActiveObject");
			return 0;
		}
		return reinterpret_cast<jlong>(pUnk);
	}

	// case 3: both file name and progID
	hr = CoCreateInstance(clsid,NULL,CLSCTX_SERVER,__uuidof(IUnknown),(LPVOID*)&pUnk);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"Failed to create CoCreateInstance");
		return 0;
	}

	IPersistFilePtr ppf(pUnk);
	hr = ppf->Load(JString(env,_fileName),0);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"Failed to load from file");
		pUnk->Release();
		return 0;
	}

	return reinterpret_cast<jlong>(pUnk);
}

//JNIEXPORT jobject JNICALL Java_com4j_Native_getROTSnapshot(JNIEnv *, jclass){
//  IRunningObjectTablePtr rot;
//  rot->Register
//    IRunningObjectTable
//#include "objidl.h"
//}

JNIEXPORT jlong JNICALL Java_com4j_Native_getRunningObjectTable(JNIEnv* env, jclass __unused__){
  IRunningObjectTable *rot;
  HRESULT hr = ::GetRunningObjectTable(0, &rot);
  if(hr != S_OK){
    error(env,__FILE__,__LINE__,hr,"GetRunningObjectTable failed");
		return 0;
  }
  return reinterpret_cast<jlong>(rot);
}

JNIEXPORT jlong JNICALL Java_com4j_Native_getEnumMoniker(JNIEnv* env, jclass __unused__, jlong rotPointer){
  IRunningObjectTable *rot = reinterpret_cast<IRunningObjectTable*>(rotPointer);
  IEnumMoniker *moniker;
  HRESULT hr = rot->EnumRunning(&moniker);
  if(hr != S_OK){
    error(env, __FILE__, __LINE__, hr, "IRunningObjectTable::EnumRunning failed");
    return 0;
  }
  moniker->Reset();
  return reinterpret_cast<jlong>(moniker);
}

JNIEXPORT jlong JNICALL Java_com4j_Native_getNextRunningObject(JNIEnv *env, jclass __unused__, jlong rotPointer, jlong enumMonikerPointer){
  IRunningObjectTable *rot = reinterpret_cast<IRunningObjectTable*>(rotPointer);
  IEnumMoniker *enumMoniker = reinterpret_cast<IEnumMoniker*>(enumMonikerPointer);
  IMoniker *moniker;
  HRESULT hr = enumMoniker->Next(1, &moniker, NULL);
  if(hr == S_FALSE) {
    // This value indicates that there are no more elements, so do not report an error but return 0;
    // The Java part is responsible to call the Release on the rot and enumMoniker pointers!
    return 0;
  } else if(hr != S_OK){    
    error(env, __FILE__, __LINE__, hr, "IEnumMoniker:Next failed");
    return 0;
  }
  IUnknown *unknown;
  rot->GetObject(moniker, &unknown);
  return reinterpret_cast<jlong>(unknown);
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
	jsize len = (jsize)wcslen(p);
	while(len>0 && (p[len-1]==L'\r' || p[len-1]==L'\n'))
		len--;

	jstring result = env->NewString(p,len);
	LocalFree(p);

	return result;
}

JNIEXPORT jlong JNICALL Java_com4j_Native_getErrorInfo(
	JNIEnv* env, jclass __unused__, jlong pComObject, jlong iid1, jlong iid2) {

	MyGUID iid(iid1,iid2);

  try {
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
	  return reinterpret_cast<jlong>(pError);
  } catch (...) {
    // an exception occured. This might happen, if the automation server is not available due to a crash.
    return 0;
  }
  return 0;
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

JNIEXPORT jlong JNICALL Java_com4j_Native_loadTypeLibrary(
	JNIEnv* env, jclass __unused__, jstring _name ) {

	JString name(env,_name);
	ITypeLib* pLib=NULL;

	HRESULT hr = LoadTypeLib(name,&pLib);
	if(FAILED(hr)) {
		error(env,__FILE__,__LINE__,hr,"LoadTypeLib failed");
		return 0;
	}
	
	return reinterpret_cast<jlong>(typelib::CTypeLib::get(pLib));
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

JNIEXPORT jlong JNICALL Java_com4j_Native_advise( JNIEnv* env, jclass _, jlong ptr, jobject proxy,jlong iid1, jlong iid2) {
	CEventReceiver* p = CEventReceiver::create( env, reinterpret_cast<IConnectionPoint*>(ptr), proxy, MyGUID(iid1,iid2) );
	return reinterpret_cast<jlong>(p);
}

JNIEXPORT void JNICALL Java_com4j_Native_unadvise( JNIEnv* env, jclass _, jlong p ) {
	CEventReceiver* er = reinterpret_cast<CEventReceiver*>(p);
	er->Disconnect(env);
	delete er;
}

JNIEXPORT jobject JNICALL Java_com4j_Native_createBuffer(JNIEnv* env, jclass _, jlong ptr, jint size) {
	return env->NewDirectByteBuffer(reinterpret_cast<void*>(ptr),size);
}

