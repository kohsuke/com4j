#include "stdafx.h"
#include "eventReceiver.h"
#include "java_id.h"
#include "variant.h"

JClassID com4jEventProxy("com4j/EventProxy");
JMethodID<jintArray> com4jEventProxy_getDISPIDs(com4jEventProxy,"getDISPIDs","([Ljava/lang/String;)[I");
JMethodID<jobject> com4jEventProxy_invoke(com4jEventProxy,"invoke","(II[Lcom4j/Variant;)Ljava/lang/Object;");
JStaticMethodID<jstring> com4jEventProxy_getErrorSource(com4jEventProxy,"getErrorSource","(Ljava/lang/Throwable;)Ljava/lang/String;");
JStaticMethodID<jstring> com4jEventProxy_getErrorDetail(com4jEventProxy,"getErrorDetail","(Ljava/lang/Throwable;)Ljava/lang/String;");

STDMETHODIMP CEventReceiver::GetIDsOfNames( REFIID riid, LPOLESTR* rgszNames, UINT cNames, LCID lcid, DISPID* rgDispId ) {
	AttachThread jniScope(jniModule);
	JNIEnv* pEnv = jniScope;

	bool unknown = false;
	
	// Convert names to string array
	jobjectArray ar = pEnv->NewObjectArray(cNames,javaLangString, NULL);
	int len = cNames;
	for( int i=0; i < len; i++) {
		pEnv->SetObjectArrayElement( ar, i, pEnv->NewString( (jchar*)rgszNames[i], wcslen( rgszNames[i])));
	}

	LockedArray<jint> r(pEnv, com4jEventProxy_getDISPIDs( pEnv, eventProxy, ar));
	for(unsigned int i=0; i<cNames; i++ ) {
		*rgDispId++ = r[i];
		if(r[i]==DISPID_UNKNOWN)
			unknown = true;
	}

	return unknown? DISP_E_UNKNOWNNAME : S_OK;
}

STDMETHODIMP CEventReceiver::Invoke( DISPID dispid, REFIID riid, LCID lcid, WORD wFlags, DISPPARAMS* pDispParams, VARIANT* pResult, EXCEPINFO* pExcepInfo, UINT* puArgErr ) {
	AttachThread jniScope(jniModule);
	JNIEnv* pEnv = jniScope;
	
	jobjectArray ar = pEnv->NewObjectArray(pDispParams->cArgs,com4j_Variant,NULL);

	{// copy arguments into com4j Variant types, since we may later change their types
		LockedArray<jobject> data(pEnv,ar);
		int len = pDispParams->cArgs;
		for( int i=0; i<len; i++ ) {
			data[len-i-1] = com4j_Variant_new(pEnv);
			::VariantCopy(com4jVariantToVARIANT(pEnv,data[len-i-1]), &pDispParams->rgvarg[i]);
		}
	}

	jobject r = com4jEventProxy_invoke(pEnv,eventProxy, (jint)dispid,(jint)wFlags,ar);
	
	// check if there was any exception
	jthrowable t = pEnv->ExceptionOccurred();
	if(t!=NULL) {
		pEnv->ExceptionClear();
		if(pExcepInfo!=NULL) {
			pExcepInfo->wCode = 1000;	// Java doesn't have any notion of 'error code'
			pExcepInfo->wReserved = 0;
			pExcepInfo->bstrSource		= SysAllocString(JString(pEnv,com4jEventProxy_getErrorSource(pEnv,t)));
			pExcepInfo->bstrDescription = SysAllocString(JString(pEnv,com4jEventProxy_getErrorDetail(pEnv,t)));
			pExcepInfo->bstrHelpFile = NULL;
			pExcepInfo->dwHelpContext = 0;
			pExcepInfo->pvReserved = NULL;
			pExcepInfo->pfnDeferredFillIn = NULL;
			pExcepInfo->scode = 0;
		}

		return DISP_E_EXCEPTION;
	}

	if(r!=NULL && pResult!=NULL) {
		VARIANT* pSrc = convertToVariant(pEnv,r);
		::VariantCopy(pResult,pSrc);
		::VariantClear(pSrc);
		delete pSrc;
	}

	return S_OK;
}
