#pragma once

// wraps jstring and provides access to the LPSTR form and the LPCWSTR form
class JString {
private:
	JNIEnv* const	env;
	const jstring	jstr;
	const jchar*	ret;	// value returned from GetStringUTFChars
	LPSTR			psz;
	LPWSTR			pwsz;
public:
	JString( JNIEnv* _env, const jstring str ) : jstr(str),env(_env) {
		ret = env->GetStringChars(jstr,NULL);
		int len = env->GetStringLength(jstr);
		pwsz = new wchar_t[len+1];
		memcpy(pwsz,ret,len*sizeof(wchar_t));
		pwsz[len] = L'\0';
		psz = new char[len*2+1];
		wcstombs(psz,pwsz,len*2);
	}
	~JString() {
		env->ReleaseStringChars(jstr,ret);
		delete psz;
		delete pwsz;
	}

	operator LPCSTR () const {
		return psz;
	}
	operator LPCWSTR () const {
		return pwsz;
	}
};