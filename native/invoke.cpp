#include "stdafx.h"
#include "com4j.h"
#include "unmarshaller.h"
#include "safearray.h"





Environment::~Environment() {
	// run post actions
	while( postActions!=NULL ) {
		postActions->act(env);
		PostAction* old = postActions;
		postActions = postActions->next;
		delete old;
	}
}

#ifdef	_DEBUG
static int invocationCount = 0;
#endif

jobject Environment::invoke( void* pComObject, ComMethod method, jobjectArray args, ConvSpec* convs,
	jclass retType, int retIndex, bool retIsInOut, ConvSpec retConv ) {
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
		double d;
		float f;
		void*	pv;
		SAFEARRAY* psa;
		VARIANT_BOOL vbool;
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
			switch( convs[i].major ) {
			case cvBSTR:
				bstr = toBSTR((jstring)arg);
				_asm push bstr;
				break;

			case cvBSTR_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					unm = new BSTRUnmarshaller(toBSTR((jstring)jholder(arg)->get(env)));
					add( new OutParamHandler( jholder(arg), unm ) );
					pv = unm->addr();
				}
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
			case cvComObject:
				_ASSERT( sizeof(INT32)==sizeof(jint) );
				int32 = env->CallIntMethod(arg,javaLangNumber_intValue);
				_asm push int32;
				break;

			case cvINT32_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					unm = new IntUnmarshaller(env,arg);
					add( new OutParamHandler( jholder(arg), unm ) );
					pv = unm->addr();
				}
				_asm push pv;
				break;

			case cvDATE:
			case cvDouble:
				// TODO: check if this is correct
				d = env->CallDoubleMethod( arg, javaLangNumber_doubleValue );
				f = reinterpret_cast<float*>(&d)[1];
				_asm push f;
				f = reinterpret_cast<float*>(&d)[0];
				_asm push f;
				break;

			case cvFloat:
				f = env->CallFloatMethod( arg, javaLangNumber_floatValue );
				_asm push f;
				break;

			case cvBool:
				if(env->CallBooleanMethod( arg, javaLangBoolean_booleanValue )) {
					int32 = TRUE;
				} else {
					int32 = FALSE;
				}
				_asm push int32;
				break;

			case cvVariantBool:
				if(env->CallBooleanMethod( arg, javaLangBoolean_booleanValue )) {
					vbool = VARIANT_TRUE;
				} else {
					vbool = VARIANT_FALSE;
				}
				_asm push vbool;
				break;

			case cvGUID:
				_ASSERT( sizeof(GUID)==sizeof(jlong)*2 );
				pv = env->GetLongArrayElements( (jlongArray)arg, NULL );
				add(new LongArrayCleanUp((jlongArray)arg,pv));
				_asm push pv;
				break;

			case cvSAFEARRAY:
				psa = NULL;
				switch(convs[i].minor) {
				case cvsaBoolean:
					psa = safearray::PrimitiveArrayConverter<VT_BOOL,VARIANT_BOOL,jboolean>::toNative(env,(jarray)arg);
					break;
				case cvsaByte:
					psa = safearray::PrimitiveArrayConverter<VT_UI1,byte,jbyte>::toNative(env,(jarray)arg);
					break;
				case cvsaChar:
					psa = safearray::PrimitiveArrayConverter<VT_UI2,unsigned short,jchar>::toNative(env,(jarray)arg);
					break;
				case cvsaDouble:
					psa = safearray::PrimitiveArrayConverter<VT_R8,double,jdouble>::toNative(env,(jarray)arg);
					break;
				case cvsaFloat:
					psa = safearray::PrimitiveArrayConverter<VT_R4,float,jfloat>::toNative(env,(jarray)arg);
					break;
				case cvsaInt:
					psa = safearray::PrimitiveArrayConverter<VT_I4,INT32,jint>::toNative(env,(jarray)arg);
					break;
				case cvsaLong:
					psa = safearray::PrimitiveArrayConverter<VT_I8,INT64,jlong>::toNative(env,(jarray)arg);
					break;
				case cvsaShort:
					psa = safearray::PrimitiveArrayConverter<VT_I2,short,jshort>::toNative(env,(jarray)arg);
					break;
				case cvsaString:
					psa = safearray::BasicArrayConverter<VT_BSTR,xducer::StringXducer>::toNative(env,(jarray)arg);
					break;
				}
				_ASSERT(psa!=NULL);
				add( new SAFEARRAYCleanUp(psa) );
				_asm push psa;
				break;
				

			default:
				error(env,"unexpected conversion type: %d",convs[i]);
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
				switch(retConv.major) {
				case cvBSTR:
					retUnm = new BSTRUnmarshaller(NULL);
					break;
				
				case cvHRESULT:
					// this is a special case which we handle later
					break;

				case cvComObject:
					retUnm = new ComObjectUnmarshaller();
					break;

				case cvINT32:
					retUnm = new IntUnmarshaller(env,NULL);
					break;

				case cvBool:
				case cvVariantBool:
					retUnm = new BoolUnmarshaller(env,NULL);
					break;

				case cvFloat:
					retUnm = new FloatUnmarshaller(env,NULL);
					break;

				case cvDouble:
					retUnm = new DoubleUnmarshaller(env,NULL);
					break;

				case cvGUID:
					retUnm = new GUIDUnmarshaller();
					break;

				default:
					error(env,"unexpected conversion type: %d",retConv);
					return NULL;
				}

				if(retUnm!=NULL) {
					pv = retUnm->addr();
					_asm push pv;
				}
			}
		}
	}

#ifdef	_DEBUG
	invocationCount++;	// for debugging. this makes it easier to set a break-point.
#endif

	// push the 'this' pointer
	__asm push pComObject;

	// invoke the method.
	__asm call method;

	// once the call returns, stack should have been cleaned up,
	// and the return value should be in EAX.
	__asm mov hr,EAX;

	// if the caller wants the HRESULT as the return value,
	// don't throw ComException
	if(retConv.major==cvHRESULT) {
		jclass javaLangInteger = env->FindClass("java/lang/Integer");
		return env->NewObject(
			javaLangInteger,
			env->GetMethodID(javaLangInteger,"<init>","(I)V"),
			hr );
	}

	// otherwise check the HRESULT first
	if(FAILED(hr)) {
		wchar_t* pmsg;
		jobject str = NULL;
		if(!FAILED(FormatMessageW(FORMAT_MESSAGE_ALLOCATE_BUFFER|FORMAT_MESSAGE_FROM_SYSTEM, NULL, hr, 0, (LPWSTR)&pmsg, 0, NULL ))) {
			str = env->NewString(pmsg,wcslen(pmsg));
			LocalFree(pmsg);
		}
		
		env->Throw( (jthrowable)env->NewObject( comexception, comexception_new, str, (jint)hr ) );
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
