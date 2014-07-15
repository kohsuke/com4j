#pragma once
#define _CRT_SECURE_NO_DEPRECATE 1

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

// if the following line fails, you have to add
// $JAVA_HOME/include and $JAVA_HOME/include/win32
// to your include directory.
#include <jni.h>

#include <crtdbg.h>
#include <oleauto.h>

#include <atlcore.h>
#include <atlbase.h>
extern CComModule _Module;
#include <atlcom.h>
#include <atlwin.h>
#include <atlcur.h>

#include <map>
#include <set>
#include <list>
#include <stack>
#include <algorithm>
using namespace std;

#include "../jnitl/include/jnitl.h"
using namespace jnitl;
extern JNIModule jniModule;

// smart pointer definitions
#include <comdef.h>
_COM_SMARTPTR_TYPEDEF(ITypeInfo, __uuidof(ITypeInfo));
_COM_SMARTPTR_TYPEDEF(ITypeLib, __uuidof(ITypeLib));

template<class Cont, class Type>
inline typename Cont::iterator find( Cont& cont, const Type& _Val ) {
	return find(cont.begin(), cont.end(), _Val );
}
template<class Cont, class Type>
inline typename Cont::const_iterator find( const Cont& cont, const Type& _Val ) {
	return find(cont.begin(), cont.end(), _Val );
}