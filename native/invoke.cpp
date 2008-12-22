#include "stdafx.h"
#include "com4j.h"
#include "unmarshaller.h"
#include "safearray.h"
#include "variant.h"

/**  
 * Original auther  (C) Kohsuke Kawaguchi (kk@kohsuke.org)
 * Modified by      (C) Michael Schnell (scm, 2008, Michael-Schnell@gmx.de)
 */

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

jobject Environment::invoke( void* pComObject, ComMethod method, jobjectArray args, jint* convs, int retIndex, bool retIsInOut, jint retConv ) {
	// list of clean up actions
	int i;
	DWORD spBefore,spAfter;
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
		INT64	int64;
		double d;
		float f;
		void*	pv;
		SAFEARRAY* psa;
		VARIANT* pvar;
		// VARIANT_BOOL vbool;
//	};

	HRESULT hr;

	const int paramLen = env->GetArrayLength(args);

	__asm mov [spBefore],ESP;

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
				int8 = javaLangNumber_byteValue(env,arg);
				// scm: Unlike push int16, pushing int8 will correctly push a dword (32bit) onto stack, but will issue an compiler waring (C4409)
				// _asm push int8; // See also compiler warning C4409! (scm)
				int32 = int8;
				_asm push int32;
				break;

			case cvINT8_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					unm = new ByteUnmarshaller(env,jholder(arg)->get(env));
					add(new OutParamHandler( jholder(arg), unm ) );
					pv = unm->addr();
				}
				_asm push pv;
				break;

			case cvINT16:
				_ASSERT( sizeof(INT16)==sizeof(jshort) );
				int16 = javaLangNumber_shortValue(env,arg);        
				// scm: 
				// We need to push 32 bit (4 byte) on the stack. 
				// the call   _asm push int16;   is NOT what we want.. (this would use an opcode and really pushing only a WORD (16 bit) onto stack.)
				int32 = int16;   // wrapping the 2 byte short into a 4 byte int
				_asm push int32; // pushing this onto the stack.
				break;

			case cvINT16_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					unm = new ShortUnmarshaller(env,jholder(arg)->get(env));
					add(new OutParamHandler( jholder(arg), unm ) );
					pv = unm->addr();
				}
				_asm push pv;
				break;

			case cvINT32:
			case cvComObject:
			case cvDISPATCH:
				_ASSERT( sizeof(INT32)==sizeof(jint) );
				int32 = javaLangNumber_intValue(env,arg);
				_asm push int32;
				break;

			case cvINT64:
				//_asm int 3;
				int64 = javaLangNumber_longValue(env,arg);
				// scm: pushing a 64 bit value (a QUAD WORD) is pushing two DWORDs
				_asm push dword ptr [int64 + 4]; // pushing the "upper" part first. (address of int64 + 4 bytes)
				_asm push dword ptr [int64];
				break;

			case cvPVOID:
				if(arg==NULL)
					pv = NULL;
				else
					pv = env->GetDirectBufferAddress(arg);
				if(pv==NULL) {
					error(env,__FILE__,__LINE__,"the given Buffer object is not a direct buffer");
					__asm mov ESP,[spBefore];
					return NULL;
				}
				_asm push pv;
				break;

			case cvINT32_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					unm = new IntUnmarshaller(env,jholder(arg)->get(env));
					add( new OutParamHandler( jholder(arg), unm ) );
					pv = unm->addr();
				}
				_asm push pv;
				break;

			case cvDATE:
			case cvDouble:
				d = javaLangNumber_doubleValue(env,arg);
				// scm: pushing a double (64 bit) onto stack is pushing two 32 bit values (DWORDs)
				_asm push dword ptr [d + 4]; // push the "upper" part first (adress of d + 4 bytes)
				_asm push dword ptr [d];
				break;

			case cvDouble_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					unm = new DoubleUnmarshaller(env,jholder(arg)->get(env));
					add(new OutParamHandler( jholder(arg), unm ));
					pv = unm->addr();
				}
				_asm push pv;
				break;

			case cvFloat:
				f = javaLangNumber_floatValue(env,arg);
				_asm push f;
				break;

			case cvFloat_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					unm = new FloatUnmarshaller(env,jholder(arg)->get(env));
					add(new OutParamHandler( jholder(arg), unm ));
					pv = unm->addr();
				}
				_asm push pv;
				break;

			case cvBool:
				if(javaLangBoolean_booleanValue(env,arg)) {
					int32 = TRUE;
				} else {
					int32 = FALSE;
				}
				_asm push int32;
				break;

			case cvVariantBool:
				if(javaLangBoolean_booleanValue(env,arg)) {
					int32 = VARIANT_TRUE;
				} else {
					int32 = VARIANT_FALSE;
				}
				_asm push int32;
				break;

			case cvVariantBool_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					unm = new VariantBoolUnmarshaller(env,jholder(arg)->get(env));
					add( new OutParamHandler( jholder(arg), unm ) );
					pv = unm->addr();
				}
				_asm push pv;
				break;

			case cvGUID:
				_ASSERT( sizeof(GUID)==sizeof(jlong)*2 );
				pv = env->GetLongArrayElements( (jlongArray)arg, NULL );
				add(new LongArrayCleanUp((jlongArray)arg,pv));
				_asm push pv;
				break;

			case cvCURRENCY:
				if(arg==NULL) {
					// push 0 for 8 bytes
					int32 = 0;
					_asm push int32;
					_asm push int32;
				} else {
					jstring strRep = javaMathBigDecimal_toString(env,arg);
					CComCurrency cy((LPCSTR)JString(env,strRep));
					_asm push cy.m_currency.Hi;
					_asm push cy.m_currency.Lo;
				}
				break;

			case cvCURRENCY_byRef:
				if(arg==NULL) {
					pv = NULL;
				} else {
					jstring strRep = javaMathBigDecimal_toString(env,jholder(arg)->get(env));
					CComCurrency cy((LPCSTR)JString(env,strRep));
					
					unm = new CurrencyUnmarshaller(cy);
					add(new OutParamHandler( jholder(arg), unm ));
					pv = unm->addr();
				}
				_asm push pv;
				break;

			case cvVARIANT:
				_ASSERT(sizeof(VARIANT)==0x10);
				_asm sub ESP,0x10;
				_asm mov [pvar],ESP;

				if(arg==NULL) {
					*pvar = vtMissing;
				} else {
					// otherwise convert a value to a VARIANT, and simply use that for the stack var.
					// since we aren't using VariantCopy, there's no need to VariantClear pSrc.
					// hence just 'delete'
					VARIANT* pSrc = convertToVariant(env,arg);
					if(pSrc==NULL) {
						jstring name = javaLangClass_getName(env,env->GetObjectClass(arg));
						error(env,__FILE__,__LINE__,E_FAIL,"Unable to convert %s to VARIANT",LPCSTR(JString(env,name)));
						__asm mov ESP,[spBefore];
						return NULL;
					}
					*reinterpret_cast<VARIANT*>(pvar) = *pSrc;
					delete pSrc;
				}
				break;

			case cvVARIANT_byRef:
				if(arg==NULL) {
					pvar = new VARIANT();
					VariantInit(pvar);
					pvar->vt = VT_ERROR;
					add(new VARIANTCleanUp(pvar));
				} else
				if(env->IsSameObject(env->GetObjectClass(arg),com4j_Variant)) {
					// if we got a com4j.Variant object, pass its image.
					// we can't rely on convertToVariant, which would lose
					// the 'byRef' semantics
					pvar = com4jVariantToVARIANT(env,arg);
					// no post-unmarshalling necessary in this case
				} else
				if(env->IsSameObject(env->GetObjectClass(arg),com4j_Holder)) {
					// if it's a holder, convert its value, and prepare the unmarshaller
					unm = new VariantUnmarshaller();
					pvar = convertToVariant(env,jholder(arg)->get(env));
					*static_cast<VARIANT*>(unm->addr()) = *pvar;	// transfer the ownership to unm
					delete pvar;
					add( new OutParamHandler( jholder(arg), unm ) );	// after the method call unmarshal it back to Variant
					pvar = static_cast<VARIANT*>(unm->addr());
				} else {
					// otherwise convert a value to a VARIANT, and just assume it's an [in] only semantics
					pvar = convertToVariant(env,arg);
					add(new VARIANTCleanUp(pvar));
				}
				_asm push pvar;
				break;

			case cvSAFEARRAY:
				psa = safearray::SafeArrayXducer::toNative(env,(jarray)arg);
				if(psa==NULL) {
					error(env,__FILE__,__LINE__,"unable to convert the given array to SAFEARRAY");
					return NULL;
				}
				add( new SAFEARRAYCleanUp(psa) );
				_asm push psa;
				break;

			// Not supported, yet.
			//case cvSAFEARRAY_byRef:
 			//	if(arg==NULL) {
			//		pv = NULL;
			//	} else {
			//		unm = new SaveArrayUnmarshaller(env,jholder(arg)->get(env));
			//		add(new OutParamHandler( jholder(arg), unm ));
			//		pv = unm->addr();
			//	}
			//	_asm push pv;
			//break;

			default:
				error(env,__FILE__,__LINE__,"unexpected conversion type: %d",convs[i]);
				return NULL;
			}
		}

		if(i==retIndex) {
			if(retIsInOut) {
				// reuse the current unmarshaller
				if(unm==NULL) {
					error(env,__FILE__,__LINE__,"in/out return value must be passed by ref");
					return NULL;
				}
				retUnm = unm;
			} else {
				switch(retConv) {
				case cvBSTR:
					retUnm = new BSTRUnmarshaller(NULL);
					break;
				
				case cvHRESULT:
					// this is a special case which we handle later
					break;

				case cvComObject:
				case cvDISPATCH:
					retUnm = new ComObjectUnmarshaller();
					break;

				case cvINT8:
					retUnm = new ByteUnmarshaller(env,NULL);
					break;

				case cvINT16:
					retUnm = new ShortUnmarshaller(env,NULL);
					break;

				case cvINT32:
					retUnm = new IntUnmarshaller(env,NULL);
					break;

				case cvINT64:
					retUnm = new LongUnmarshaller(env,NULL);
					break;

				case cvBool:
				case cvVariantBool:
					retUnm = new BoolUnmarshaller(env,NULL);
					break;

				case cvFloat:
					retUnm = new FloatUnmarshaller(env,NULL);
					break;

				case cvDouble:
				case cvDATE:
					retUnm = new DoubleUnmarshaller(env,NULL);
					break;

				case cvCURRENCY:
					retUnm = new CurrencyUnmarshaller();
					break;

				case cvGUID:
					retUnm = new GUIDUnmarshaller();
					break;

				case cvVARIANT:
					retUnm = new VariantUnmarshaller();
					break;

				default:
					error(env,__FILE__,__LINE__,"unexpected conversion type: %d",retConv);
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

	__asm mov [spAfter],ESP;

	// once the call returns, stack should have been cleaned up,
	// and the return value should be in EAX.
	__asm mov hr,EAX;

	// check that the stack size is correct
	if(spBefore!=spAfter) {
		__asm mov ESP, [spBefore];
		error(env,__FILE__,__LINE__,"Unexpected stack corruption. Is the method definition correct?");
		return NULL;
	}


	// if the caller wants the HRESULT as the return value,
	// don't throw ComException
	if(retConv==cvHRESULT) {
		jclass javaLangInteger = env->FindClass("java/lang/Integer");
		return env->NewObject(
			javaLangInteger,
			env->GetMethodID(javaLangInteger,"<init>","(I)V"),
			hr );
	}

	// otherwise check the HRESULT first
	if(FAILED(hr)) {
		wchar_t* pmsg = NULL;
		jobject str = NULL;
		if(!FAILED(FormatMessageW(FORMAT_MESSAGE_ALLOCATE_BUFFER|FORMAT_MESSAGE_FROM_SYSTEM, NULL, hr, 0, (LPWSTR)&pmsg, 0, NULL ))) {
			if(pmsg!=NULL) {
				str = env->NewString(pmsg,wcslen(pmsg));
				LocalFree(pmsg);
			}
		}
		env->Throw( (jthrowable)comexception_new_hr(env, str, (jint)hr, env->NewStringUTF(__FILE__), (jint)__LINE__ ) );
		if(retUnm!=NULL)	delete retUnm;
		return NULL;
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
