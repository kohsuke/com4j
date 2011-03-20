#include "stdafx.h"
#include "com4j_tlbimp_Native.h"
#include "com4j.h"

// HKEY wrapper that automates deallocation
class KeyHolder {
private:
	HKEY key;
public:
	KeyHolder() { key=NULL; }
	~KeyHolder() {
		if(key!=NULL)
			RegCloseKey(key);
		key=NULL;
	}

	HKEY* operator & () {
		return &key;
	}

	operator HKEY () {
		return key;
	}
};

JNIEXPORT jstring JNICALL Java_com4j_tlbimp_Native_readRegKey(
	JNIEnv* env, jclass clazz, jstring _key ) {

	JString key(env,_key);
	
	wchar_t w[256];
	long size = sizeof(w);
	LONG r = RegQueryValueW(HKEY_CLASSES_ROOT,key,w,&size);
	if(r!=0) {
		error(env,__FILE__,__LINE__,r,"incorrect key name \"%s\"",static_cast<LPCSTR>(key));
		return NULL;
	}
	return env->NewString(w,(jsize)wcslen(w));
}

JNIEXPORT jobjectArray JNICALL Java_com4j_tlbimp_Native_enumRegKeys(
	JNIEnv* env, jclass clazz, jstring _key ) {
	
	JString key(env,_key);

	KeyHolder hKey;
	LONG r = RegOpenKey(HKEY_CLASSES_ROOT,key,&hKey);
	if(r!=0) {
		error(env,__FILE__,__LINE__,r,"incorrect key name \"%s\"",static_cast<LPCSTR>(key));
		return NULL;
	}

	DWORD nSubKeys;
	RegQueryInfoKey(hKey,NULL,NULL,NULL,&nSubKeys,NULL,NULL,NULL,NULL,NULL,NULL,NULL);

	jobjectArray array = env->NewObjectArray( nSubKeys, javaLangString, NULL );

	for( DWORD i=0; i<nSubKeys; i++ ) {
		wchar_t w[256];
		RegEnumKeyW(hKey,i,w,sizeof(w));
		env->SetObjectArrayElement( array, i, 
			env->NewString(w,(jsize)wcslen(w)) );
	}

	return array;
}
/*
jstring foo( JNIEnv* env, jstring _guid, jstring _version, jstring localeId ) {
	KeyHolder libKey;
	LONG r;
	
	r = RegOpenKey(HKEY_CLASSES_ROOT,JString(env,_guid),&libKey);
	if(r!=0) {
		error(env,r,"incorrect LIBID");
		return NULL;
	}

	KeyHolder verKey;
	if(_version!=NULL) {
		r = RegOpenKey(libKey,JString(env,_version),&verKey);
		if(r!=0) {
			error(env,r,"incorrect version");
			return NULL;
		}
	} else {
		DWORD nSubKeys;
		RegQueryInfoKey(libKey,NULL,NULL,NULL,&nSubKeys,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
		for( DWORD i=0; i<nSubKeys; i++ ) {
			char w[32];
			RegEnumKey(libKey,i,w,sizeof(w));

		}
	}
}
*/