#include "stdafx.h"
#include "com4j.h"
#include "unmarshaller.h"

// throws an error
void error( JNIEnv* env, const char* msg ) {
	jclass iae = env->FindClass("java/lang/IllegalArgumentException");
	env->ThrowNew(iae,msg);
}





Environment::~Environment() {
	// run post actions
	while( postActions!=NULL ) {
		postActions->act(env);
		PostAction* old = postActions;
		postActions = postActions->next;
		delete old;
	}
}

jobject Environment::invoke( void* pComObject, ComMethod method, jobjectArray args, jint* convs,
	jclass retType, int retIndex, bool retIsInOut, jint retConv ) {
	// list of clean up actions

	int i;
	// the unmarshaller if this method should return an object.
	Unmarshaller* retUnm = NULL;

// somehow anonymous union here makes the compiler upset
// --- when we do "push pv" it pushes something different!
//	union {
		BSTR bstr;
		Unmarshaller* unm;
		const wchar_t*	lpcwstr;
		const char*		lpcstr;
		INT8	int8;
		INT16	int16;
		INT32	int32;
		void*	pv;
//	};

	HRESULT hr;

	const int paramLen = env->GetArrayLength(args);

	// push arguments to the stack
	// since we are accumlating things to the stack
	for( i=paramLen; i>=0; i-- ) {
		// to handle the case where the [retval] comes as the last in the parameter,
		// we start from i=length, not usual i=lengh-1
		unm = NULL;
		jobject arg = NULL;
		// TODO: check if we can safely use local variables
		
		if( i!=paramLen ) {
			arg = env->GetObjectArrayElement(args,i);
			switch( convs[i] ) {
			case cvBSTR:
				bstr = toBSTR((jstring)arg);
				_asm push bstr;
				break;

			case cvBSTR_byRef:
				unm = new BSTRUnmarshaller(toBSTR((jstring)jholder(arg)->get(env)));
				add( new OutParamHandler( jholder(arg), unm ) );
				pv = unm->addr();
				_asm push pv;
				break;

			case cvLPCWSTR:
				lpcwstr = toLPCWSTR((jstring)arg);
				_asm push lpcwstr;
				break;

			case cvLPCSTR:
				lpcstr = toLPCSTR((jstring)arg);
				_asm push lpcstr;
				break;

			case cvINT8:
				_ASSERT( sizeof(INT8)==sizeof(jbyte) );
				int8 = env->CallByteMethod(arg,
					env->GetMethodID(javaLangNumber,"byteValue","()B"));
				_asm push int8;
				break;

			case cvINT16:
				_ASSERT( sizeof(INT16)==sizeof(jshort) );
				int16 = env->CallByteMethod(arg,
					env->GetMethodID(javaLangNumber,"shortValue","()S"));
				_asm push int16;

			case cvINT32:
				_ASSERT( sizeof(INT32)==sizeof(jint) );
				int32 = env->CallByteMethod(arg,
					env->GetMethodID(javaLangNumber,"intValue","()I"));
				_asm push int16;

			default:
				error(env,"unexpected conversion type");
				return NULL;
			}
		}

		if(i==retIndex) {
			if(retIsInOut) {
				// reuse the current unmarshaller
				if(unm==NULL) {
					error(env,"in/out return value must be passed by ref");
					return NULL;
				}
				retUnm = unm;
			} else {
				switch(retConv) {
				case cvBSTR:
					retUnm = new BSTRUnmarshaller(NULL);
					pv = retUnm->addr();
					_asm push pv;
					break;
				
				case cvHRESULT:
					// this is a special case which we handle later
					break;

				default:
					error(env,"unexpected conversion type");
					return NULL;
				}
			}
		}
	}

	// push the 'this' pointer
	__asm push pComObject;

	// invoke the method.
	__asm call method;

	// once the call returns, stack should have been cleaned up,
	// and the return value should be in EAX.
	__asm mov hr,EAX;

	if(retConv==cvHRESULT) {
		jclass javaLangInteger = env->FindClass("java/lang/Integer");
		return env->NewObject(
			javaLangInteger,
			env->GetMethodID(javaLangInteger,"<init>","(I)V"),
			hr );
	}

	if(retUnm==NULL)	return NULL;
	jobject r = retUnm->unmarshal(env);
	if(!retIsInOut)
		// if retIsInOut, it will be cleaned up by a holder.
		delete retUnm;
	return r;
}

void Environment::add( PostAction* a ) {
	a->next = postActions;
	postActions = a;
}



const jchar* Environment::toChars( jstring s ) {
	if(s==NULL)		return NULL;

	const jchar* r = env->GetStringChars(s,NULL);

	class ReleaseStringCharsCleanUp : public PostAction {
		const jchar*	buf;
		jstring			str;
	public:
		ReleaseStringCharsCleanUp( jstring _str, const jchar* _buf ) {
			str = _str;
			buf = _buf;
		}
		void act( JNIEnv* env ) {
			env->ReleaseStringChars(str,buf);
		}
	};

	add( new ReleaseStringCharsCleanUp(s,r) );
	return r;
}

const wchar_t* Environment::toLPCWSTR( jstring s ) {
	if(s==NULL)		return NULL;

	_ASSERT( sizeof(jchar)==sizeof(wchar_t) );

	const jchar* r = toChars(s);
	jsize sz = env->GetStringLength(s);
	wchar_t* buf = new wchar_t[sz+1];
	memcpy(buf,r,sz*sizeof(wchar_t));
	buf[sz]=L'\0';
	add( new DeleteCleanUp<wchar_t>(buf) );
	return buf;
}

LPCSTR Environment::toLPCSTR( jstring s ) {
	if(s==NULL)		return NULL;

	const jchar* r = toChars(s);
	jsize sz = env->GetStringLength(s);
	char* buf = new char[sz*2+1];
	int len = WideCharToMultiByte( CP_ACP, 0, r, sz, buf, sz*2+1, NULL, NULL );
	buf[len] = '\0';
	add( new DeleteCleanUp<char>(buf) );
	return buf;
}

BSTR Environment::toBSTR( jstring s ) {
	if(s==NULL)		return NULL;

	BSTR bstr = SysAllocStringLen(toChars(s),env->GetStringLength(s));
	add(new BSTRCleanUp(bstr));
	return bstr;
}
