#pragma once
#ifdef _DEBUG
#include "resource.h"       // main symbols

#include "com4j.h"
#include "_ITestObjectEvents_CP.h"





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
};

OBJECT_ENTRY_AUTO(__uuidof(TestObject), CTestObject)
#endif