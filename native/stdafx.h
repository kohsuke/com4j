#pragma once

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

// if the following line fails, you have to add
// $JAVA_HOME/include and $JAVA_HOME/include/win32
// to your include directory.
#include <jni.h>

#include <crtdbg.h>
#include <oleauto.h>

#include <atlbase.h>
extern CComModule _Module;
#include <atlcom.h>
