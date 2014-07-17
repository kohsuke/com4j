#pragma once
//#include "com4j_h.h"
#import "com4j.tlb" no_namespace

/**  
 * Original auther  (C) Kohsuke Kawaguchi (kk@kohsuke.org)
 * Modified by      (C) Michael Schnell (scm, 2008, Michael-Schnell@gmx.de)
 */

namespace typelib {

class CTypeLib;
class CTypeDecl;
class CTypeInfo;
class CMethod;

_COM_SMARTPTR_TYPEDEF(CTypeLib, __uuidof(ITypeLibrary));
_COM_SMARTPTR_TYPEDEF(CTypeDecl, __uuidof(ITypeDecl));
_COM_SMARTPTR_TYPEDEF(CMethod, __uuidof(IMethod));


// creates a type object from a descriptor
// return an addref-ed pointer
IType* createType( CTypeDecl* containingType, TYPEDESC& t );

// resolve HREFTYPE to ITypeDecl under the context of pTypeInfo
ITypeDecl* getRef( CTypeDecl* pTypeInfo, HREFTYPE href );


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




// reused for both IPtrType and ISafeArrayType
class ATL_NO_VTABLE CPtrType :
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CPtrType, &__uuidof(IPtrType)>,
	public IPtrType,
	public ISafeArrayType
{
private:
	ITypePtr m_pType;
	bool isPtr;

public:
	HRESULT init( CTypeDecl* containingType, TYPEDESC& t, bool _isPtr ) {
		m_pType = createType(containingType,t);
		isPtr = _isPtr;
		if(m_pType==NULL)
			return E_FAIL;
		else
			return S_OK;
	}

	static CComObject<CPtrType>* create( CTypeDecl* containingType, TYPEDESC& t, bool _isPtr ) {
		CComObject<CPtrType>* pObj = NULL;
		CComObject<CPtrType>::CreateInstance(&pObj);
		pObj->AddRef();
		if(FAILED(pObj->init(containingType,t,_isPtr))) {
			pObj->Release();
			return NULL;
		}
		return pObj;
	}


DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CPtrType)
	COM_INTERFACE_ENTRY2(IUnknown,IPtrType)
	COM_INTERFACE_ENTRY2(IType,IPtrType)
	COM_INTERFACE_ENTRY_FUNC_BLIND(0,castDynamic)
END_COM_MAP()

	static HRESULT WINAPI castDynamic(void* pv, REFIID riid, LPVOID* ppv, DWORD_PTR dw) {
		CPtrType* pThis = static_cast<CPtrType*>(pv);
		if( pThis->isPtr && riid==__uuidof(IPtrType) ) {
			*ppv = static_cast<IPtrType*>(pThis);
			pThis->AddRef();
			return S_OK;
		}
		if( !pThis->isPtr && riid==__uuidof(ISafeArrayType) ) {
			*ppv = static_cast<ISafeArrayType*>(pThis);
			pThis->AddRef();
			return S_OK;
		}
		return E_NOINTERFACE;
	}

public:
	STDMETHOD(raw_getPointedAtType)( IType** ppType ) {
		*ppType = m_pType;
		m_pType.AddRef();	// for the pointer returned
		return S_OK;
	}
	STDMETHOD(raw_getComponentType)( IType** ppType ) {
		return raw_getPointedAtType(ppType);
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
	STDMETHOD(raw_isVarArg)(VARIANT_BOOL* pOut) {
		*pOut = (m_pDesc->cParamsOpt==-1)?VARIANT_TRUE:VARIANT_FALSE;
		return S_OK;
	}
	STDMETHOD(raw_getDispId)(int* pDispid) {
		*pDispid = m_pDesc->memid;
		return S_OK;
	}
	STDMETHOD(raw_getFlags)(int* pflags) {
		*pflags = m_pDesc->wFuncFlags;
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
	STDMETHOD(raw_isOptional)(VARIANT_BOOL *pValue){
		return getFlag( PARAMFLAG_FOPT, pValue);
	}
	STDMETHOD(raw_isLCID)(VARIANT_BOOL *pValue){
		return getFlag( PARAMFLAG_FLCID, pValue);
	}
	STDMETHOD(raw_getDefaultValue)(VARIANT* pValue) {
		ELEMDESC& d = desc();
		if(d.paramdesc.wParamFlags&PARAMFLAG_FHASDEFAULT && d.paramdesc.pparamdescex!=NULL)
			return VariantCopy(pValue, &(d.paramdesc.pparamdescex->varDefaultValue));
		else // no default value
			return VariantClear(pValue);
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



class ATL_NO_VTABLE CProperty : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CConstant, &__uuidof(IConstant)>,
	public IProperty
{
	CTypeDeclPtr	m_pParent;
	VARDESC*		m_pDesc;
public:
	CProperty() {}
	~CProperty() {
		getTypeInfo()->ReleaseVarDesc(m_pDesc);
		m_pDesc = NULL;
	}

	void init( CTypeDecl* pParent, int idx ) {
		m_pParent = pParent;
		getTypeInfo()->GetVarDesc(idx,&m_pDesc);
	}

	static CComObject<CProperty>* create( CTypeDecl* pParent, int idx ) {
		CComObject<CProperty>* pObj = NULL;
		CComObject<CProperty>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pParent,idx);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CProperty)
	COM_INTERFACE_ENTRY(IProperty)
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
	STDMETHOD(raw_getDispId)(int* pDispid) {
		*pDispid = m_pDesc->memid;
		return S_OK;
	}
};




class ATL_NO_VTABLE CImplInterface : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CImplInterface, &__uuidof(IImplementedInterfaceDecl)>,
	public IImplementedInterfaceDecl
{
	CTypeDeclPtr	m_pParent;
	int				m_index;
public:
	CImplInterface() {}

	void init( CTypeDecl* pParent, int idx ) {
		m_pParent = pParent;
		m_index = idx;
	}

	static CComObject<CImplInterface>* create( CTypeDecl* pParent, int idx ) {
		CComObject<CImplInterface>* pObj = NULL;
		CComObject<CImplInterface>::CreateInstance(&pObj);
		pObj->AddRef();
		pObj->init(pParent,idx);
		return pObj;
	}

// DECLARE_REGISTRY_RESOURCEID(...)

DECLARE_PROTECT_FINAL_CONSTRUCT()

BEGIN_COM_MAP(CImplInterface)
	COM_INTERFACE_ENTRY(IImplementedInterfaceDecl)
	COM_INTERFACE_ENTRY(IUnknown)
END_COM_MAP()
	
	ITypeInfo* pType();
	HRESULT getFlag( int mask, VARIANT_BOOL* pValue ) {
		int flags;
		HRESULT hr = pType()->GetImplTypeFlags(m_index,&flags);
		if(FAILED(hr))	return hr;
		if( flags&mask )	*pValue = VARIANT_TRUE;
		else				*pValue = VARIANT_FALSE;
		return S_OK;
	}
public:
	STDMETHOD(raw_isDefault)(VARIANT_BOOL* pValue) {
		return getFlag( IMPLTYPEFLAG_FDEFAULT, pValue );
	}
	STDMETHOD(raw_isSource)(VARIANT_BOOL* pValue) {
		return getFlag( IMPLTYPEFLAG_FSOURCE, pValue );
	}
	STDMETHOD(raw_isRestricted)(VARIANT_BOOL* pValue) {
		return getFlag( IMPLTYPEFLAG_FRESTRICTED, pValue );
	}
	STDMETHOD(raw_getType)(ITypeDecl** ppType ) {
		*ppType = NULL;
		HREFTYPE href;
		HRESULT hr = pType()->GetRefTypeOfImplType( m_index, &href );
		if(FAILED(hr))	return hr;

		*ppType = getRef(m_pParent,href);
		if(*ppType==NULL)
			return E_FAIL;
		return S_OK;
	}
};






class ATL_NO_VTABLE CTypeDecl : 
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CTypeDecl, &__uuidof(ITypeDecl)>,
	public IDispInterfaceDecl,
	public IInterfaceDecl,
	public IEnumDecl,
	public ITypedefDecl,
	public ICoClassDecl
{
public:
	CTypeLibPtr m_pParent;
	ITypeInfoPtr m_pType;
	TYPEATTR* m_pAttr;
#ifdef	_DEBUG
	DWORD	m_ThreadID;
#endif

	CTypeDecl() {
#ifdef	_DEBUG
		m_ThreadID = ::GetCurrentThreadId();
#endif
	}
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

	static HRESULT WINAPI castDynamic(void* pv, REFIID riid, LPVOID* ppv, DWORD_PTR dw) {
		CTypeDecl* pThis = static_cast<CTypeDecl*>(pv);
		TYPEKIND kind = pThis->m_pAttr->typekind;

		DYNAMIC_CAST_TEST(TypeKind_DISPATCH,		IDispInterfaceDecl)
		DYNAMIC_CAST_TEST(TypeKind_INTERFACE,		IInterfaceDecl)
		DYNAMIC_CAST_TEST(TypeKind_ENUM,			IEnumDecl)
		DYNAMIC_CAST_TEST(TypeKind_ALIAS,			ITypedefDecl)
		DYNAMIC_CAST_TEST(TypeKind_COCLASS,			ICoClassDecl)
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
	STDMETHOD(raw_getParent)(ITypeLibrary** ppParent );

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
	STDMETHOD(raw_countProperties)(int* pValue) {
		*pValue = m_pAttr->cVars;
		return S_OK;
	}
  STDMETHOD(raw_getProperty)( int index, IProperty** ppConstant ) {
		if(index<0 || m_pAttr->cVars<=index) {
			*ppConstant = NULL;
			return E_INVALIDARG;
		} else {
			*ppConstant = CProperty::create(this,index);
			return S_OK;
		}
	}

	STDMETHOD(raw_countBaseInterfaces)(int* pValue) {
		*pValue = m_pAttr->cImplTypes;
		return S_OK;
	}

	STDMETHOD(raw_getBaseInterface)(int index, ITypeDecl** ppType ) {
		*ppType = NULL;
		HREFTYPE href;
		HRESULT hr = m_pType->GetRefTypeOfImplType( index, &href );
		if(FAILED(hr))	return hr;

		*ppType = getRef(this,href);
		if(*ppType==NULL)
			return E_FAIL;
		return S_OK;
	}

	// IDispInterface
	STDMETHOD(raw_isDual)(VARIANT_BOOL* pOut) {
		HREFTYPE href;
		HRESULT hr = m_pType->GetRefTypeOfImplType(-1,&href);
		if(FAILED(hr)) {
			*pOut = VARIANT_FALSE;
		} else {
			*pOut = VARIANT_TRUE;
		}
		return S_OK;
	}
	STDMETHOD(raw_getVtblInterface)(IInterfaceDecl** ppInterface ) {
		HREFTYPE href;
		HRESULT hr = m_pType->GetRefTypeOfImplType(-1,&href);
		if(FAILED(hr))	return hr;

		ITypeDecl* r = getRef(this,href);
		TypeKind k = r->getKind();
		hr = r->QueryInterface(ppInterface);
		r->Release();
		return hr;
	}

  STDMETHOD(raw_getDispInterface)(IDispInterfaceDecl** ppInterface ) {
		HREFTYPE href;
		HRESULT hr = m_pType->GetRefTypeOfImplType(-1,&href);
		if(FAILED(hr))	return hr;

		ITypeDecl* r = getRef(this,href);
		TypeKind k = r->getKind();
		hr = r->QueryInterface(ppInterface);
		r->Release();
		return hr;
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

	// ICoClassDecl
	STDMETHOD(raw_countImplementedInterfaces)(int* pValue) {
		*pValue = m_pAttr->cImplTypes;
		return S_OK;
	}

	STDMETHOD(raw_getImplementedInterface)(int index, IImplementedInterfaceDecl** ppType ) {
		if(index<0 || m_pAttr->cImplTypes<=index) {
			*ppType = NULL;
			return E_INVALIDARG;
		} else {
			*ppType = CImplInterface::create(this,index);
			return S_OK;
		}
	}

	STDMETHOD(raw_isCreatable)(VARIANT_BOOL* pOut) {
		*pOut = (m_pAttr->wTypeFlags & TYPEFLAG_FCANCREATE )?VARIANT_TRUE:VARIANT_FALSE;
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
	// child CTypeDecls inside this library.
	typedef map<ITypeInfo*,CTypeDecl*> childrenT;
	childrenT children;

	// currently active CTypeLibs.
	typedef map<ITypeLib*,CTypeLib*> LibMap;
	static LibMap libraries;

	CTypeLib() {}
	~CTypeLib() {
		size_t cnt = libraries.erase(m_pTypeLib);
		_ASSERT(cnt==1);
		m_pTypeLib->ReleaseTLibAttr(m_pAttr);
		m_pTypeLib=NULL;
	}

	void init( ITypeLib* pTypeLib ) {
		m_pTypeLib = pTypeLib;
		pTypeLib->GetLibAttr(&m_pAttr);
		_ASSERT(libraries.find(m_pTypeLib)==libraries.end());
		libraries[m_pTypeLib] = this;
	}

	static CTypeLib* get( ITypeLib* pTypeLib ) {
		LibMap::const_iterator itr = libraries.find(pTypeLib);
		if(itr==libraries.end()) {
			// create a new one
			CComObject<CTypeLib>* pLib = NULL;
			CComObject<CTypeLib>::CreateInstance(&pLib);
			pLib->AddRef();
			pLib->init(pTypeLib);
			return pLib;
		} else {
			// get the existing one
			CTypeLib* pLib = itr->second;
			pLib->AddRef();
			return pLib;
		}
	}

	CTypeDecl* getChild( ITypeInfo* pType ) {
		childrenT::iterator itr = children.find(pType);
		if(itr!=children.end()) {
			// reuse
			CTypeDecl* r = itr->second;
			r->AddRef();
			return r;
		} else {
			// create a new one
			return CTypeDecl::create( this, pType );
		}
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

	STDMETHOD(raw_getLibid)(GUID* pGuid) {
		*pGuid = m_pAttr->guid;
		return S_OK;
	}
	STDMETHOD(raw_getTypeDecl)(int nIndex, ITypeDecl** ppType) {
		HRESULT hr;
		ITypeInfo* p=NULL;
		hr = m_pTypeLib->GetTypeInfo(nIndex,&p);
		if(FAILED(hr))		return hr;

		*ppType = static_cast<IInterfaceDecl*>(getChild(p));
		return hr;
	}
};


}