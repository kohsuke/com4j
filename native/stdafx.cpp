#include "stdafx.h"



BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
					 )
{
	// sanity check
	_ASSERT( sizeof(void*)==sizeof(jint) );
	return TRUE;
}
