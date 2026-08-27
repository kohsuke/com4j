#include "stdafx.h"

CTestObjectModule _Module;
JNIModule jniModule;

BOOL APIENTRY DllMain( HINSTANCE hModule,
                       DWORD  dwReason,
                       LPVOID lpReserved
					 )
{
	AtlAxWinInit();
	return _Module.DllMain(dwReason, lpReserved);
}

STDAPI DllCanUnloadNow(void)
{
  return _Module.DllCanUnloadNow();
}

STDAPI DllGetClassObject(REFCLSID rclsid, REFIID riid, LPVOID* ppv)
{
  return _Module.DllGetClassObject(rclsid, riid, ppv);
}

STDAPI DllRegisterServer(void)
{
  return _Module.DllRegisterServer();
}

STDAPI DllUnregisterServer(void)
{
  return _Module.DllUnregisterServer();
}
