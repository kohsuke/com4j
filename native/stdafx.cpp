#include "stdafx.h"

#include <atlimpl.cpp>

CComModule _Module;
JNIModule jniModule;

BOOL APIENTRY DllMain( HINSTANCE hModule, 
                       DWORD  dwReason, 
                       LPVOID lpReserved
					 )
{
	// sanity check
	_ASSERT( sizeof(void*)==sizeof(jint) );
	_Module.DllMain(hModule, dwReason, lpReserved, NULL, NULL );
//    _Module.Init(ObjectMap, m_hInstance, &LIBID_LIB2USRLib);

	return TRUE;
}
