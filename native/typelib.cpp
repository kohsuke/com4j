#include "stdafx.h"
#include "typelib.h"

namespace typelib {

ITypeDecl* getRef( CTypeDecl* pTypeInfo, HREFTYPE href ) {
	ITypeInfoPtr pRefType;
	if(FAILED(pTypeInfo->m_pType->GetRefTypeInfo( href, &pRefType )))
		return NULL;

	ITypeLibPtr pLib;
	UINT index;
	if(FAILED(pRefType->GetContainingTypeLib(&pLib,&index)))
		return NULL;
	
	// if we already have CTypeDecl for this pRefType, return it.
	return static_cast<IInterfaceDecl*>(CTypeLib::get(pLib)->getChild(pRefType));
}

IType* createType( CTypeDecl* containingType, TYPEDESC& t ) {
	switch(t.vt) {
	case VT_USERDEFINED:
		return getRef( containingType, t.hreftype );

	case VT_PTR:
		return static_cast<IPtrType*>(CComObject<CPtrType>::create(containingType,*t.lptdesc,true));

	case VT_CARRAY:
		return static_cast<IPtrType*>(CComObject<CPtrType>::create(containingType,t.lpadesc->tdescElem,true)); // ???
	
	case VT_SAFEARRAY:
		return static_cast<ISafeArrayType*>(CComObject<CPtrType>::create(containingType,*t.lptdesc,false));

	default:
	{
		IType* p = CPrimitiveType::get(t.vt);
		_ASSERT(p!=NULL);
		return p;
	}
	}
}





map<VARTYPE, IPrimitiveType*> CPrimitiveType::singletons;

CPrimitiveTypeImpl vti1(VT_I1,L"char");
CPrimitiveTypeImpl vti2(VT_I2,L"short");
CPrimitiveTypeImpl vti4(VT_I4,L"int");
CPrimitiveTypeImpl vtr4(VT_R4,L"float");
CPrimitiveTypeImpl vtr8(VT_R8,L"double");
CPrimitiveTypeImpl vtcy(VT_CY,L"Currency");
CPrimitiveTypeImpl vtbstr(VT_BSTR,L"BSTR");
CPrimitiveTypeImpl vtlpstr(VT_LPSTR,L"LPSTR");
CPrimitiveTypeImpl vtlpwstr(VT_LPWSTR,L"LPWSTR");
CPrimitiveTypeImpl vtbool(VT_BOOL,L"bool");
CPrimitiveTypeImpl vtvoid(VT_VOID,L"void");
CPrimitiveTypeImpl vtui1(VT_UI1,L"byte");
CPrimitiveTypeImpl vtui2(VT_UI2,L"ushort");
CPrimitiveTypeImpl vtui4(VT_UI4,L"uint");
CPrimitiveTypeImpl vti8(VT_I8,L"long");
CPrimitiveTypeImpl vtui8(VT_UI8,L"ulong");
CPrimitiveTypeImpl vtint(VT_INT,L"int");
CPrimitiveTypeImpl vtuint(VT_UINT,L"uint");
CPrimitiveTypeImpl vtdispatch(VT_DISPATCH,L"(IDISPATCH)");
CPrimitiveTypeImpl vtunknown(VT_UNKNOWN,L"(IUNKNOWN)");
CPrimitiveTypeImpl vtvariant(VT_VARIANT,L"Variant");
CPrimitiveTypeImpl vtdate(VT_DATE,L"Date");
CPrimitiveTypeImpl vthresult(VT_HRESULT,L"HRESULT");

CTypeLib::LibMap CTypeLib::libraries;


CMethod::~CMethod() {
	m_pParent->m_pType->ReleaseFuncDesc(m_pDesc);
	m_pDesc=NULL;

	for( int i=0; i<m_nameCount; i++ )
		SysFreeString(m_pNames[i]);
	delete[] m_pNames;
}


void CMethod::init( CTypeDecl* pParent, int idx ) {
	m_pParent = pParent;
	HRESULT hr = m_pParent->m_pType->GetFuncDesc(idx,&m_pDesc);
	_ASSERT(SUCCEEDED(hr));

	m_nameCount = m_pDesc->cParams+1;
	m_pNames = new BSTR[m_nameCount];
	memset(m_pNames,0,sizeof(BSTR)*m_nameCount);
	UINT unused;
	m_pParent->m_pType->GetNames( memid(), m_pNames, m_nameCount, &unused );
}

STDMETHODIMP CMethod::raw_getName(BSTR* pName) {
	UINT unused;
	return m_pParent->m_pType->GetNames(m_pDesc->memid, pName, 1, &unused );
}
STDMETHODIMP CMethod::raw_getKind(INVOKEKIND* pKind) {
	*pKind = m_pDesc->invkind;
	return S_OK;
}
STDMETHODIMP CMethod::raw_getHelpString(BSTR* pHelpString) {
	return m_pParent->m_pType->GetDocumentation(m_pDesc->memid, NULL, pHelpString, NULL, NULL );
}
STDMETHODIMP CMethod::raw_getReturnType(IType** ppType) {
	*ppType = createType(m_pParent, m_pDesc->elemdescFunc.tdesc);
	return S_OK;
}
STDMETHODIMP CMethod::raw_getParamCount(int* pOut) {
	*pOut = m_pDesc->cParams;
	return S_OK;
}
STDMETHODIMP CMethod::raw_getParam(int index, IParam** pOut) {
	*pOut = NULL;
	if(index<0 || m_pDesc->cParams<=index)
		return E_INVALIDARG;
	
	*pOut = CParam::create( this, index );
	return S_OK;
}


ITypeInfo* CConstant::getTypeInfo() {
	return m_pParent->m_pType;
}

ITypeInfo* CProperty::getTypeInfo() {
	return m_pParent->m_pType;
}





CTypeDecl::~CTypeDecl() {
#ifdef	_DEBUG
	DWORD curThread = ::GetCurrentThreadId();
	_ASSERT(m_ThreadID == curThread);
#endif
	m_pType->ReleaseTypeAttr(m_pAttr);
	size_t r = m_pParent->children.erase(m_pType);
	_ASSERT(r==1);
}

void CTypeDecl::init( CTypeLib* pParent, ITypeInfo* pType ) {
	m_pParent = pParent;
	m_pType = pType;
	pType->GetTypeAttr(&m_pAttr);

	// register this to the parent
	_ASSERT( pParent->children.find(pType)==pParent->children.end() );
	pParent->children[pType] = this;
}

STDMETHODIMP CTypeDecl::raw_getParent(ITypeLibrary** ppParent ) {
	m_pParent->AddRef();
	*ppParent = m_pParent;
	return S_OK;
}


ITypeInfo* CImplInterface::pType() {
	return m_pParent->m_pType;
}


}