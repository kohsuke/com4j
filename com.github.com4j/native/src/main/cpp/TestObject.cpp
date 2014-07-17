#include "stdafx.h"
#ifdef	_DEBUG
#include "TestObject.h"


// CTestObject

STDMETHODIMP CTestObject::InterfaceSupportsErrorInfo(REFIID riid)
{
	static const IID* arr[] = 
	{
		&__uuidof(ITestObject)
	};

	for (int i=0; i < sizeof(arr) / sizeof(arr[0]); i++)
	{
		if (InlineIsEqualGUID(*arr[i],riid))
			return S_OK;
	}
	return S_FALSE;
}

STDMETHODIMP CTestObject::raw_TestVariant(VARIANT v1, VARIANT* v2, VARIANT* v3)
{
	VariantClear(v3);
	VariantCopy(v3,v2);
	VariantClear(v2);
	VariantCopy(v2,&v1);
	return S_OK;
}

STDMETHODIMP CTestObject::raw_outByteBuf(BSTR bstrEncodedData, long* plSize, unsigned char** ppbData)
{
	*plSize = 30;
	*ppbData = (BYTE*)"Hello, World!";

	return S_OK;
}

STDMETHODIMP CTestObject::raw_testUI8Conv(VARIANT* in, VARIANT* out)
{
	if(in->vt!=VT_UI8 || in->ullVal!=((ULONGLONG)LONG_MAX)+1)
		return E_INVALIDARG;

	return ::VariantCopy(out,in);
}

#endif