#pragma once
#include "com4j.h"

class ATL_NO_VTABLE CEventReceiver :
	public CComObjectRootEx<CComMultiThreadModel>,
	public CComCoClass<CEventReceiver>,
	public IDispatch
{
private:
	// peer in the Java world
	GlobalRef<jobject>  eventProxy;

	IConnectionPointPtr pSource;
	DWORD eventCookie;
	boolean connected;
	IID expectedIID;

	static HRESULT WINAPI CustomQueryInterface(void* pv, REFIID riid, LPVOID* ppv, DWORD_PTR pdw) {
		CComObject<CEventReceiver>* pThis = static_cast<CComObject<CEventReceiver>*>(pv);
		if( pThis->expectedIID==riid ) {
			*ppv = static_cast<IDispatch*>(pThis);
			pThis->AddRef();
			return S_OK;
		}
		*ppv = NULL;
		return E_FAIL;
	}

public:

	void init( JNIEnv* pEnv, const IConnectionPointPtr& pSource, jobject eventProxy, IID expectedIID ) {
		this->pSource = pSource;
		this->eventProxy.Attach(pEnv,eventProxy);
		this->expectedIID = expectedIID;
		eventCookie = 0;
		HRESULT hr = pSource->Advise(this,&eventCookie);
		if(FAILED(hr)) {
			error(pEnv,__FILE__,__LINE__,hr,"failed to subscribe to the event source");
			connected = false;
		} else {
			connected = true;
		}
	}

	void Disconnect( JNIEnv* pEnv ) {
		if(connected) {
			HRESULT hr = pSource->Unadvise(eventCookie);
			if(FAILED(hr)) {
				error(pEnv,__FILE__,__LINE__,hr,"failed to unsubscribe to the event source");
				// but assume it's disconnected anyway
			}
			connected = false;
			eventCookie = 0;
		}
	}

	static CEventReceiver* create( JNIEnv* pEnv, const IConnectionPointPtr& pSource, jobject eventProxy, IID expectedIID ) {
		CComObject<CEventReceiver>* pObj;
		CComObject<CEventReceiver>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pEnv,pSource,eventProxy,expectedIID);
		return pObj;
	}


DECLARE_PROTECT_FINAL_CONSTRUCT()
	
BEGIN_COM_MAP(CEventReceiver)
	COM_INTERFACE_ENTRY(IUnknown)
	COM_INTERFACE_ENTRY(IDispatch)
	COM_INTERFACE_ENTRY_FUNC_BLIND(0,CustomQueryInterface)
END_COM_MAP()

public:
	STDMETHOD(GetTypeInfoCount)(UINT *pctinfo) {
		*pctinfo = 0;
		return S_OK;
	}

	STDMETHOD(GetTypeInfo)( UINT iTInfo, LCID lcid, ITypeInfo** ppTInfo ) {
		*ppTInfo = NULL;
		return E_NOTIMPL;
	}
    
	STDMETHOD(GetIDsOfNames)( REFIID riid, LPOLESTR* rgszNames, UINT cNames, LCID lcid, DISPID* rgDispId );
   	STDMETHOD(Invoke)( DISPID dispid, REFIID riid, LCID lcid, WORD wFlags, DISPPARAMS* pDispParams, VARIANT* pResult, EXCEPINFO* pExcepInfo, UINT* puArgErr ); 
};
