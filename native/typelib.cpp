#include "stdafx.h"
#include "typelib.h"


IType* createType( CWTypeDecl* containingType, TYPEDESC& t ) {
	switch(t.vt) {
	case VT_USERDEFINED:
	{
		ITypeInfoPtr pRefType;
		if(FAILED(containingType->m_pType->GetRefTypeInfo( t.hreftype, &pRefType )))
			return NULL;

		ITypeLibPtr unused;
		UINT index;
		if(FAILED(pRefType->GetContainingTypeLib(&unused,&index)))
			return NULL;
		
		CWTypeLib* pLib = containingType->m_pParent;
		if( unused==pLib->m_pTypeLib ) {
			IWTypeDecl* r;
			if(FAILED(pLib->raw_getTypeDecl(index,&r)))
				return NULL;
			return r;
		} else {
			// TODO: the interface is in another type library.
			// I don't know what's the expected behavior here is.
			return NULL;
		}
	}
	case VT_PTR:
		return CComObject<CPtrType>::create(containingType,*t.lptdesc);
	
	case VT_SAFEARRAY:
		// TODO: support array
		return NULL;

	default:
	{
		IType* p = CPrimitiveType::get(t.vt);
		_ASSERT(p!=NULL);
		return p;
	}
	}
}





map<VARTYPE, IPrimitiveType*> CPrimitiveType::singletons;

CPrimitiveTypeImpl vti2(VT_I2,L"short");
CPrimitiveTypeImpl vti4(VT_I4,L"int");
CPrimitiveTypeImpl vtr4(VT_R4,L"float");
CPrimitiveTypeImpl vtr8(VT_R8,L"double");
CPrimitiveTypeImpl vtbstr(VT_BSTR,L"BSTR");
CPrimitiveTypeImpl vtbool(VT_BOOL,L"bool");
CPrimitiveTypeImpl vtvoid(VT_VOID,L"void");
CPrimitiveTypeImpl vtui4(VT_UI4,L"uint");
CPrimitiveTypeImpl vtint(VT_INT,L"int");
CPrimitiveTypeImpl vtuint(VT_UINT,L"uint");
CPrimitiveTypeImpl vtdispatch(VT_DISPATCH,L"(IDISPATCH)");
CPrimitiveTypeImpl vtunknown(VT_UNKNOWN,L"(IUNKNOWN)");
CPrimitiveTypeImpl vtvariant(VT_VARIANT,L"Variant");
CPrimitiveTypeImpl vtdate(VT_DATE,L"Date");


CWMethod::~CWMethod() {
	m_pParent->m_pType->ReleaseFuncDesc(m_pDesc);
	m_pDesc=NULL;
}


void CWMethod::init( CWTypeDecl* pParent, int idx ) {
	m_pParent = pParent;
	HRESULT hr = m_pParent->m_pType->GetFuncDesc(idx,&m_pDesc);
	_ASSERT(SUCCEEDED(hr));
}

STDMETHODIMP CWMethod::raw_getName(BSTR* pName) {
	UINT unused;
	return m_pParent->m_pType->GetNames(m_pDesc->memid, pName, 1, &unused );
}
STDMETHODIMP CWMethod::raw_getKind(INVOKEKIND* pKind) {
	*pKind = m_pDesc->invkind;
	return S_OK;
}
STDMETHODIMP CWMethod::raw_getHelpString(BSTR* pHelpString) {
	return m_pParent->m_pType->GetDocumentation(m_pDesc->memid, NULL, pHelpString, NULL, NULL );
}
STDMETHODIMP CWMethod::raw_getReturnType(IType** ppType) {
	*ppType = createType(m_pParent, m_pDesc->elemdescFunc.tdesc);
	return S_OK;
}





CWTypeDecl::~CWTypeDecl() {
	m_pType->ReleaseTypeAttr(m_pAttr);
	int r = m_pParent->children.erase(m_pType);
	_ASSERT(r==1);
}

void CWTypeDecl::init( CWTypeLib* pParent, ITypeInfo* pType ) {
	m_pParent = pParent;
	m_pType = pType;
	pType->GetTypeAttr(&m_pAttr);

	// register this to the parent
	_ASSERT( pParent->children.find(pType)==pParent->children.end() );
	pParent->children[pType] = this;
}
