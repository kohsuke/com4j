#pragma once
#include "com4j_h.h"








class ATL_NO_VTABLE CWMethod : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CWMethod, &__uuidof(IWMethod)>,
	public IWMethod
{
	ITypeInfo*	m_pTypeInfo;
	FUNCDESC*	m_pDesc;
public:
	CWMethod() {}
	~CWMethod() {
		m_pTypeInfo->ReleaseFuncDesc(m_pDesc);
		m_pDesc=NULL;
		m_pTypeInfo->Release();
		m_pTypeInfo=NULL;
	}

	void init( ITypeInfo* pTypeInfo, int idx ) {
		m_pTypeInfo = pTypeInfo;
		pTypeInfo->AddRef();
		HRESULT hr = m_pTypeInfo->GetFuncDesc(idx,&m_pDesc);
		_ASSERT(SUCCEEDED(hr));
	}

	static CComObject<CWMethod>* create( ITypeInfo* pTypeInfo, int idx ) {
		CComObject<CWMethod>* pObj = NULL;
		CComObject<CWMethod>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pTypeInfo,idx);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CWMethod)
	COM_INTERFACE_ENTRY(IWMethod)
	COM_INTERFACE_ENTRY(IUnknown)
END_COM_MAP()

public:
	STDMETHOD(getName)(BSTR* pName) {
		UINT unused;
		return m_pTypeInfo->GetNames(m_pDesc->memid, pName, 1, &unused );
	}
};




class ATL_NO_VTABLE CWType : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CWType, &__uuidof(IWType)>,
	public IWDispInterface
{
public:
	ITypeInfo* m_pType;
	TYPEATTR* m_pAttr;

	CWType() {}
	~CWType() {
		m_pType->ReleaseTypeAttr(m_pAttr);
		m_pType->Release();
		m_pType=NULL;
	}

	void init( ITypeInfo* pType ) {
		m_pType = pType;
		pType->GetTypeAttr(&m_pAttr);
	}

	static CComObject<CWType>* create( ITypeInfo* pType ) {
		CComObject<CWType>* pObj = NULL;
		CComObject<CWType>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pType);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CWType)
	COM_INTERFACE_ENTRY(IUnknown)
	COM_INTERFACE_ENTRY(IWType)
	COM_INTERFACE_ENTRY(IWDispInterface)
END_COM_MAP()

public:
	STDMETHOD(getName)(BSTR* pName) {
		*pName = NULL;
		return m_pType->GetDocumentation( -1, pName, NULL, NULL, NULL );
	}
	STDMETHOD(getHelpString)(BSTR* pHelpString) {
		*pHelpString = NULL;
		return m_pType->GetDocumentation( -1, NULL, pHelpString, NULL, NULL );
	}
	STDMETHOD(getKind)( TypeKind* out ) {
		*reinterpret_cast<TYPEKIND*>(out) = m_pAttr->typekind;
		return S_OK;
	}
	STDMETHOD(getGUID)( GUID* out ) {
		*out = m_pAttr->guid;
		return S_OK;
	}
	STDMETHOD(countMethods)(int* pValue) {
		*pValue = m_pAttr->cFuncs;
		return S_OK;
	}

	STDMETHOD(getMethod)( int index, IWMethod** ppMethod ) {
		if(index<0 || m_pAttr->cFuncs<=index) {
			*ppMethod = NULL;
			return E_INVALIDARG;
		} else {
			*ppMethod = CWMethod::create(m_pType,index);
			return S_OK;
		}
	}

};








class ATL_NO_VTABLE CWTypeLib : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CWTypeLib, &__uuidof(IWTypeLib)>,
	public IWTypeLib
{
public:
	ITypeLib* m_pTypeLib;
	TLIBATTR* m_pAttr;

	CWTypeLib() {}
	~CWTypeLib() {
		m_pTypeLib->ReleaseTLibAttr(m_pAttr);
		m_pTypeLib->Release();
		m_pTypeLib=NULL;
	}

	void init( ITypeLib* pTypeLib ) {
		m_pTypeLib = pTypeLib;
		pTypeLib->GetLibAttr(&m_pAttr);
	}

	static CComObject<CWTypeLib>* create( ITypeLib* pTypeLib ) {
		CComObject<CWTypeLib>* pObj = NULL;
		CComObject<CWTypeLib>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pTypeLib);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CWTypeLib)
	COM_INTERFACE_ENTRY(IWTypeLib)
	COM_INTERFACE_ENTRY(IUnknown)
END_COM_MAP()

public:
	STDMETHOD(getCount)(int* pCount) {
		*pCount = m_pTypeLib->GetTypeInfoCount();
		return S_OK;
	}
	STDMETHOD(getName)(BSTR* pName) {
		*pName = NULL;
		return m_pTypeLib->GetDocumentation( -1, pName, NULL, NULL, NULL );
	}
	STDMETHOD(getHelpString)(BSTR* pHelpString) {
		*pHelpString = NULL;
		return m_pTypeLib->GetDocumentation( -1, NULL, pHelpString, NULL, NULL );
	}

	STDMETHOD(getGUID)(GUID* pGuid) {
		*pGuid = m_pAttr->guid;
		return S_OK;
	}
	STDMETHOD(getType)(int nIndex, IWType** ppType) {
		HRESULT hr;
		ITypeInfo* p=NULL;
		hr = m_pTypeLib->GetTypeInfo(nIndex,&p);
		if(FAILED(hr))		return hr;

		*ppType = CWType::create(p);
		return hr;
	}
};
