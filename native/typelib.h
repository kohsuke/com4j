#pragma once
//#include "com4j_h.h"
#import "com4j.tlb" no_namespace


class CTypeLib;
class CTypeDecl;
class CMethod;

_COM_SMARTPTR_TYPEDEF(CTypeLib, __uuidof(ITypeLibrary));
_COM_SMARTPTR_TYPEDEF(CTypeDecl, __uuidof(ITypeDecl));
_COM_SMARTPTR_TYPEDEF(CMethod, __uuidof(IMethod));


// creates a type object from a descriptor
// return an addref-ed pointer
IType* createType( CTypeDecl* containingType, TYPEDESC& t );


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
	STDMETHOD(raw_getVarType)( VARTYPE* pOut ) {
		*pOut = vt;
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
	void init( CTypeDecl* containingType, TYPEDESC& t ) {
		m_pType = createType(containingType,t);
		_ASSERT(m_pType!=NULL);
	}

	static CComObject<CPtrType>* create( CTypeDecl* containingType, TYPEDESC& t ) {
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



class ATL_NO_VTABLE CMethod : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CMethod, &__uuidof(IMethod)>,
	public IMethod
{
	CTypeDeclPtr	m_pParent;
	FUNCDESC*	m_pDesc;
	BSTR*		m_pNames;
	int			m_nameCount;

	friend class CParam;

public:
	CMethod() {}
	~CMethod();

	void init( CTypeDecl* pParent, int idx );

	static CComObject<CMethod>* create( CTypeDecl* pParent, int idx ) {
		CComObject<CMethod>* pObj = NULL;
		CComObject<CMethod>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pParent,idx);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CMethod)
	COM_INTERFACE_ENTRY(IMethod)
	COM_INTERFACE_ENTRY(IUnknown)
END_COM_MAP()

public:
	MEMBERID memid() const {
		return m_pDesc->memid;
	}

	STDMETHOD(raw_getName)(BSTR* pName);
	STDMETHOD(raw_getKind)(INVOKEKIND* pKind);
	STDMETHOD(raw_getHelpString)(BSTR* pHelpString);
	STDMETHOD(raw_getReturnType)(IType** ppType);
	STDMETHOD(raw_getParamCount)(int* pOut);
	STDMETHOD(raw_getParam)(int index, IParam** pOut);
	STDMETHOD(raw_getVtableIndex)(int* pOut) {
		*pOut = m_pDesc->oVft/sizeof(void*);
		return S_OK;
	}
};




class ATL_NO_VTABLE CParam : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CParam, &__uuidof(IParam)>,
	public IParam
{
	CMethodPtr	m_pParent;
	int			m_index;
public:
	CParam() {}

	void init( CMethod* pParent, int idx ) {
		m_pParent = pParent;
		m_index = idx;
	}

	static CComObject<CParam>* create( CMethod* pParent, int idx ) {
		CComObject<CParam>* pObj = NULL;
		CComObject<CParam>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pParent,idx);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CParam)
	COM_INTERFACE_ENTRY(IParam)
	COM_INTERFACE_ENTRY(IUnknown)
END_COM_MAP()
	
	ELEMDESC& desc() {
		return m_pParent->m_pDesc->lprgelemdescParam[m_index];
	}
public:
	STDMETHOD(raw_getName)(BSTR* pName) {
		*pName = SysAllocString( m_pParent->m_pNames[m_index+1] );
		return S_OK;
	}
	STDMETHOD(raw_getType)(IType** ppType) {
		*ppType = createType(m_pParent->m_pParent, desc().tdesc);
		return S_OK;
	}
	HRESULT getFlag( int mask, VARIANT_BOOL* pValue ) {
		*pValue = ( desc().paramdesc.wParamFlags & mask )?VARIANT_TRUE:VARIANT_FALSE;
		return S_OK;
	}
	STDMETHOD(raw_isIn)(VARIANT_BOOL* pValue) {
		return getFlag( PARAMFLAG_FIN, pValue );
	}
	STDMETHOD(raw_isOut)(VARIANT_BOOL* pValue) {
		return getFlag( PARAMFLAG_FOUT, pValue );
	}
	STDMETHOD(raw_isRetval)(VARIANT_BOOL* pValue) {
		return getFlag( PARAMFLAG_FRETVAL, pValue );
	}
};




class ATL_NO_VTABLE CConstant : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CConstant, &__uuidof(IConstant)>,
	public IConstant
{
	CTypeDeclPtr	m_pParent;
	VARDESC*		m_pDesc;
public:
	CConstant() {}
	~CConstant() {
		getTypeInfo()->ReleaseVarDesc(m_pDesc);
		m_pDesc = NULL;
	}

	void init( CTypeDecl* pParent, int idx ) {
		m_pParent = pParent;
		getTypeInfo()->GetVarDesc(idx,&m_pDesc);
	}

	static CComObject<CConstant>* create( CTypeDecl* pParent, int idx ) {
		CComObject<CConstant>* pObj = NULL;
		CComObject<CConstant>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pParent,idx);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CConstant)
	COM_INTERFACE_ENTRY(IConstant)
	COM_INTERFACE_ENTRY(IUnknown)
END_COM_MAP()

public:
	ITypeInfo* getTypeInfo();

	STDMETHOD(raw_getName)(BSTR* pName) {
		UINT unused;
		return getTypeInfo()->GetNames( m_pDesc->memid, pName, 1, &unused );
	}
	STDMETHOD(raw_getType)(IType** ppType) {
		*ppType = createType(m_pParent, m_pDesc->elemdescVar.tdesc);
		return S_OK;
	}
	STDMETHOD(raw_getHelpString)(BSTR* pStr) {
		return getTypeInfo()->GetDocumentation( m_pDesc->memid, NULL, pStr, NULL, NULL );
	}
	STDMETHOD(raw_getValue)(int* pValue) {
		VARIANT r;
		VariantInit(&r);
		HRESULT hr = VariantChangeType(&r, m_pDesc->lpvarValue,0,VT_I4);
		if(FAILED(hr))	return hr;
		*pValue = r.intVal;
		return S_OK;
	}
};






class ATL_NO_VTABLE CTypeDecl : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CTypeDecl, &__uuidof(ITypeDecl)>,
	public IDispInterfaceDecl,
	public IInterfaceDecl,
	public IEnumDecl,
	public ITypedefDecl
{
public:
	CTypeLibPtr m_pParent;
	ITypeInfoPtr m_pType;
	TYPEATTR* m_pAttr;

	CTypeDecl() {}
	~CTypeDecl();

	void init( CTypeLib* pParent, ITypeInfo* pType );

	static CComObject<CTypeDecl>* create( CTypeLib* pParent, ITypeInfo* pType ) {
		CComObject<CTypeDecl>* pObj = NULL;
		CComObject<CTypeDecl>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pParent,pType);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()
	

//
// used to enable additional interfaces based on the TYPEKIND.
//
	#define	DYNAMIC_CAST_TEST(CONST,INTF) \
		if(riid==__uuidof(INTF) && kind==CONST) { *ppv = static_cast<INTF*>(pThis); pThis->AddRef(); return S_OK; }

	static HRESULT WINAPI castDynamic(void* pv, REFIID riid, LPVOID* ppv, DWORD dw) {
		CTypeDecl* pThis = static_cast<CTypeDecl*>(pv);
		TYPEKIND kind = pThis->m_pAttr->typekind;

		DYNAMIC_CAST_TEST(TypeKind_DISPATCH,		IDispInterfaceDecl)
		DYNAMIC_CAST_TEST(TypeKind_INTERFACE,		IInterfaceDecl)
		DYNAMIC_CAST_TEST(TypeKind_ENUM,			IEnumDecl)
		DYNAMIC_CAST_TEST(TypeKind_ALIAS,			ITypedefDecl)
		return E_NOINTERFACE;
	}


BEGIN_COM_MAP(CTypeDecl)
	COM_INTERFACE_ENTRY2(IType,IInterfaceDecl)
	COM_INTERFACE_ENTRY2(ITypeDecl,IInterfaceDecl)
	COM_INTERFACE_ENTRY_FUNC_BLIND(0,castDynamic)
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

	STDMETHOD(raw_getMethod)( int index, IMethod** ppMethod ) {
		if(index<0 || m_pAttr->cFuncs<=index) {
			*ppMethod = NULL;
			return E_INVALIDARG;
		} else {
			*ppMethod = CMethod::create(this,index);
			return S_OK;
		}
	}

	// IEnumDecl
	STDMETHOD(raw_countConstants)( int* pNum ) {
		*pNum = m_pAttr->cVars;
		return S_OK;
	}
	STDMETHOD(raw_getConstant)( int index, IConstant** ppConstant ) {
		if(index<0 || m_pAttr->cVars<=index) {
			*ppConstant = NULL;
			return E_INVALIDARG;
		} else {
			*ppConstant = CConstant::create(this,index);
			return S_OK;
		}
	}

	// ITypedefDecl
	STDMETHOD(raw_getDefinition)( IType** ppType ) {
		*ppType = createType(this,m_pAttr->tdescAlias);
		return S_OK;
	}
};








class ATL_NO_VTABLE CTypeLib : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CTypeLib, &__uuidof(ITypeLib)>,
	public ITypeLibrary
{
public:
	ITypeLibPtr	m_pTypeLib;
	TLIBATTR* m_pAttr;
	// child objects.
	typedef map<ITypeInfo*,CTypeDecl*> childrenT;
	childrenT children;

	CTypeLib() {}
	~CTypeLib() {
		m_pTypeLib->ReleaseTLibAttr(m_pAttr);
		m_pTypeLib=NULL;
	}

	void init( ITypeLib* pTypeLib ) {
		m_pTypeLib = pTypeLib;
		pTypeLib->GetLibAttr(&m_pAttr);
	}

	static CComObject<CTypeLib>* create( ITypeLib* pTypeLib ) {
		CComObject<CTypeLib>* pObj = NULL;
		CComObject<CTypeLib>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pTypeLib);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CTypeLib)
	COM_INTERFACE_ENTRY(ITypeLibrary)
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
	STDMETHOD(raw_getTypeDecl)(int nIndex, ITypeDecl** ppType) {
		HRESULT hr;
		ITypeInfo* p=NULL;
		hr = m_pTypeLib->GetTypeInfo(nIndex,&p);
		if(FAILED(hr))		return hr;

		childrenT::const_iterator itr = children.find(p);
		CTypeDecl* pT;
		if( itr!=children.end() ) {
			pT = (*itr).second;
			pT->AddRef();
		} else {
			pT = CTypeDecl::create(this,p);
		}
		*ppType = static_cast<IInterfaceDecl*>(pT);
		return hr;
	}
};
