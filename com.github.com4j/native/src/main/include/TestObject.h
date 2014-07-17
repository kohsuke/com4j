#pragma once
#ifdef _DEBUG
#include "resource.h"       // main symbols

#include "com4j.h"
#include "_ITestObjectEvents_CP.h"
#include <atlcur.h>




// this is used just for testing
class ATL_NO_VTABLE CTestObject :
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CTestObject, &__uuidof(TestObject)>,
	public ISupportErrorInfo,
	public IConnectionPointContainerImpl<CTestObject>,
	public CProxy_ITestObjectEvents<CTestObject>,
	public IDispatchImpl<ITestObject, &__uuidof(ITestObject), &__uuidof(__com4j), 1,0>
{
public:
	CTestObject()
	{
	}

DECLARE_REGISTRY_RESOURCEID(IDR_TESTOBJECT)


BEGIN_COM_MAP(CTestObject)
	COM_INTERFACE_ENTRY(ITestObject)
	COM_INTERFACE_ENTRY(IDispatch)
	COM_INTERFACE_ENTRY(ISupportErrorInfo)
	COM_INTERFACE_ENTRY(IConnectionPointContainer)
END_COM_MAP()

BEGIN_CONNECTION_POINT_MAP(CTestObject)
	CONNECTION_POINT_ENTRY(__uuidof(_ITestObjectEvents))
END_CONNECTION_POINT_MAP()
// ISupportsErrorInfo
	STDMETHOD(InterfaceSupportsErrorInfo)(REFIID riid);


	DECLARE_PROTECT_FINAL_CONSTRUCT()

	HRESULT FinalConstruct()
	{
		return S_OK;
	}

	void FinalRelease()
	{
	}

public:

public:
	STDMETHOD(raw_TestVariant)(VARIANT v1, VARIANT* v2, VARIANT* v3);
public:
	STDMETHOD(raw_outByteBuf)(BSTR bstrEncodedData, long* plSize, unsigned char** ppbData);
	STDMETHOD(raw_echoInterface)(IUnknown* arg, IUnknown** result) {
		*result = arg;
		if(arg!=NULL)
			arg->AddRef();	// for the return value
		return S_OK;
	}
public:
	STDMETHOD(raw_testUI8Conv)(VARIANT*,VARIANT*);
	STDMETHOD(raw_testUI1Conv)(VARIANT* in,VARIANT* out) {
		if(in!=NULL && in->vt!=VT_ERROR) {
			return VariantCopy(out,in);
		} else {
			VariantClear(out);
			out->vt = VT_UI1;
			out->intVal = 0xcdcd; // set corrupt values to other fields
			out->bVal = 1;
			return S_OK;
		}
	}
	STDMETHOD(raw_testCurrency)(CURRENCY* in1, CURRENCY in2, CURRENCY* out) {
		if(in2.int64!=19900)
			return E_INVALIDARG; // assert to $1.99

		if(in1==NULL) {
			out->int64 = 53000L; // $5.30
		} else
			*out = *in1;
		return S_OK;
	}
	STDMETHOD(raw_testInt64)(__int64 x, __int64* y) {
		if(x!=0x100000002L)
			return E_FAIL;
		*y = x;
		return S_OK;
	}
};

OBJECT_ENTRY_AUTO(__uuidof(TestObject), CTestObject)
#endif