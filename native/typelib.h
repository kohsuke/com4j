#pragma once
//#include "com4j_h.h"
#import "com4j.tlb" no_namespace


class CWTypeLib;
class CWTypeDecl;

_COM_SMARTPTR_TYPEDEF(CWTypeLib, __uuidof(IWTypeLib));
_COM_SMARTPTR_TYPEDEF(CWTypeDecl, __uuidof(IWTypeDecl));


// creates a type object from a descriptor
// return an addref-ed pointer
IType* createType( CWTypeDecl* containingType, TYPEDESC& t );


class ATL_NO_VTABLE CPrimitiveType :
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CPrimitiveType, &__uuidof(IPrimitiveType)>,
	public IPrimitiveType
{
private:
	const wchar_t* name;
	const VARTYPE vt;
public:
	CPrimitiveType( VARTYPE _vt, const wchar_t* _name ) : vt(_vt) {
		name = _name;
		singletons[vt] = this;
	}

protected:
	static map<VARTYPE, IPrimitiveType*> singletons;

public:
	static IPrimitiveType* get( VARTYPE t ) {
		return singletons[t];
	}


DECLARE_PROTECT_FINAL_CONSTRUCT()
	
BEGIN_COM_MAP(CPrimitiveType)
	COM_INTERFACE_ENTRY(IUnknown)
	COM_INTERFACE_ENTRY(IType)
	COM_INTERFACE_ENTRY(IPrimitiveType)
END_COM_MAP()

public:
	STDMETHOD(raw_getName)(BSTR* pOut) {
		*pOut = SysAllocString(name);
		return S_OK;
	}
};

class CPrimitiveTypeImpl : public CPrimitiveType {
public:
	CPrimitiveTypeImpl( VARTYPE _vt, const wchar_t* _name ) : CPrimitiveType(_vt,_name) {}

	// this object remains in memory all the time.
	STDMETHOD_(ULONG, AddRef)() {
		return 0;
	}

	STDMETHOD_(ULONG, Release)() {
		return 0;
	}

	STDMETHOD(QueryInterface)(REFIID iid, void** ppvObject) {
		return _InternalQueryInterface(iid, ppvObject);
	}
};





class ATL_NO_VTABLE CPtrType :
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CPtrType, &__uuidof(IPtrType)>,
	public IPtrType
{
private:
	ITypePtr m_pType;

public:
	void init( CWTypeDecl* containingType, TYPEDESC& t ) {
		m_pType = createType(containingType,t);
	}

	static CComObject<CPtrType>* create( CWTypeDecl* containingType, TYPEDESC& t ) {
		CComObject<CPtrType>* pObj = NULL;
		CComObject<CPtrType>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(containingType,t);
		return pObj;
	}


DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CPtrType)
	COM_INTERFACE_ENTRY(IUnknown)
	COM_INTERFACE_ENTRY(IType)
	COM_INTERFACE_ENTRY(IPtrType)
END_COM_MAP()

public:
	STDMETHOD(raw_getPointedAtType)( IType** ppType ) {
		*ppType = m_pType;
		m_pType.AddRef();	// for the pointer returned
		return S_OK;
	}
};



class ATL_NO_VTABLE CWMethod : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CWMethod, &__uuidof(IWMethod)>,
	public IWMethod
{
	CWTypeDeclPtr	m_pParent;
	FUNCDESC*	m_pDesc;
public:
	CWMethod() {}

	void init( CWTypeDecl* pParent, int idx );

	static CComObject<CWMethod>* create( CWTypeDecl* pParent, int idx ) {
		CComObject<CWMethod>* pObj = NULL;
		CComObject<CWMethod>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pParent,idx);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CWMethod)
	COM_INTERFACE_ENTRY(IWMethod)
	COM_INTERFACE_ENTRY(IUnknown)
END_COM_MAP()

public:
	STDMETHOD(raw_getName)(BSTR* pName);
	STDMETHOD(raw_getKind)(INVOKEKIND* pKind);
	STDMETHOD(raw_getHelpString)(BSTR* pHelpString);
	STDMETHOD(raw_getReturnType)(IType** ppType);
};




class ATL_NO_VTABLE CWTypeDecl : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CWTypeDecl, &__uuidof(IWTypeDecl)>,
	public IWDispInterfaceDecl
{
public:
	CWTypeLibPtr m_pParent;
	ITypeInfoPtr m_pType;
	TYPEATTR* m_pAttr;

	CWTypeDecl() {}
	~CWTypeDecl();

	void init( CWTypeLib* pParent, ITypeInfo* pType );

	static CComObject<CWTypeDecl>* create( CWTypeLib* pParent, ITypeInfo* pType ) {
		CComObject<CWTypeDecl>* pObj = NULL;
		CComObject<CWTypeDecl>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pParent,pType);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CWTypeDecl)
	COM_INTERFACE_ENTRY(IUnknown)
	COM_INTERFACE_ENTRY(IType)
	COM_INTERFACE_ENTRY(IWTypeDecl)
	COM_INTERFACE_ENTRY(IWDispInterfaceDecl)
END_COM_MAP()

public:
	STDMETHOD(raw_getName)(BSTR* pName) {
		*pName = NULL;
		return m_pType->GetDocumentation( -1, pName, NULL, NULL, NULL );
	}
	STDMETHOD(raw_getHelpString)(BSTR* pHelpString) {
		*pHelpString = NULL;
		return m_pType->GetDocumentation( -1, NULL, pHelpString, NULL, NULL );
	}
	STDMETHOD(raw_getKind)( TypeKind* out ) {
		*reinterpret_cast<TYPEKIND*>(out) = m_pAttr->typekind;
		return S_OK;
	}
	STDMETHOD(raw_getGUID)( GUID* out ) {
		*out = m_pAttr->guid;
		return S_OK;
	}
	STDMETHOD(raw_countMethods)(int* pValue) {
		*pValue = m_pAttr->cFuncs;
		return S_OK;
	}

	STDMETHOD(raw_getMethod)( int index, IWMethod** ppMethod ) {
		if(index<0 || m_pAttr->cFuncs<=index) {
			*ppMethod = NULL;
			return E_INVALIDARG;
		} else {
			*ppMethod = CWMethod::create(this,index);
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
	ITypeLibPtr	m_pTypeLib;
	TLIBATTR* m_pAttr;
	// child objects.
	typedef map<ITypeInfo*,CWTypeDecl*> childrenT;
	childrenT children;

	CWTypeLib() {}
	~CWTypeLib() {
		m_pTypeLib->ReleaseTLibAttr(m_pAttr);
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
	STDMETHOD(raw_getCount)(int* pCount) {
		*pCount = m_pTypeLib->GetTypeInfoCount();
		return S_OK;
	}
	STDMETHOD(raw_getName)(BSTR* pName) {
		*pName = NULL;
		return m_pTypeLib->GetDocumentation( -1, pName, NULL, NULL, NULL );
	}
	STDMETHOD(raw_getHelpString)(BSTR* pHelpString) {
		*pHelpString = NULL;
		return m_pTypeLib->GetDocumentation( -1, NULL, pHelpString, NULL, NULL );
	}

	STDMETHOD(raw_getGUID)(GUID* pGuid) {
		*pGuid = m_pAttr->guid;
		return S_OK;
	}
	STDMETHOD(raw_getTypeDecl)(int nIndex, IWTypeDecl** ppType) {
		HRESULT hr;
		ITypeInfo* p=NULL;
		hr = m_pTypeLib->GetTypeInfo(nIndex,&p);
		if(FAILED(hr))		return hr;

		childrenT::const_iterator itr = children.find(p);
		if( itr!=children.end() ) {
			*ppType = (*itr).second;
			(*ppType)->AddRef();
		} else {
			*ppType = CWTypeDecl::create(this,p);
		}
		return hr;
	}
};
