#include "stdafx.h"
#include "com4j.h"
#include "unmarshaller.h"
#include "safearray.h"
#include "variant.h"
#include <stdio.h>
#include "../libffi/include/ffi.h"

/**  
 * Original auther  (C) Kohsuke Kawaguchi (kk@kohsuke.org)
 * Modified by      (C) Michael Schnell (scm, 2008, Michael-Schnell@gmx.de)
 * Modified by      (C) Mike Poindexter (staticsnow@gmail.com, 2009)
 */

#ifdef X86_WIN32
#define FFI_CALL_CONV FFI_STDCALL
#endif

#ifdef X86_WIN64
#define FFI_CALL_CONV FFI_WIN64
#endif

typedef union arg_value {
	INT64		v_int64;
	INT32		v_int32;
    INT16		v_int16;
    INT8		v_int8;
    float		v_float;
	double		v_double;
	void*		v_ptr;
} arg_value;

ffi_type *brecord_types[] = { &ffi_type_pointer, &ffi_type_pointer };
ffi_type ffi_type_brecord = {
	sizeof(void*) * 2,
	4,
	FFI_TYPE_STRUCT,
	brecord_types
};

ffi_type *decimal_types[] = { &ffi_type_uint16, &ffi_type_schar, &ffi_type_schar, &ffi_type_uint32, &ffi_type_uint64 };
ffi_type ffi_type_decimal = {
	sizeof(DECIMAL),
	4,
	FFI_TYPE_STRUCT,
	decimal_types
};

ffi_type *variant_types[] = { &ffi_type_uint16, &ffi_type_uint16, &ffi_type_uint16, &ffi_type_uint16, &ffi_type_brecord, &ffi_type_decimal };
ffi_type ffi_type_variant = {
	sizeof(VARIANT),
	4,
	FFI_TYPE_STRUCT,
	variant_types
};

jboolean
ffi_error(JNIEnv* env, const char* op, ffi_status status) {
  char msg[256];
  switch(status) {
  case FFI_BAD_ABI:
    _snprintf(msg, sizeof(msg), "Invalid calling convention");
    comexception_new(env, env->NewStringUTF(msg), env->NewStringUTF(__FILE__), (jint)__LINE__);
    return JNI_TRUE;
  case FFI_BAD_TYPEDEF:
    _snprintf(msg, sizeof(msg),
             "Invalid structure definition (native typedef error)");
	comexception_new(env, env->NewStringUTF(msg), env->NewStringUTF(__FILE__), (jint)__LINE__);
    return JNI_TRUE;
  default:
    _snprintf(msg, sizeof(msg), "%s failed (%d)", op, status);
    comexception_new(env, env->NewStringUTF(msg), env->NewStringUTF(__FILE__), (jint)__LINE__);
    return JNI_TRUE;
  case FFI_OK:
    return JNI_FALSE;
  }
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

#ifdef	_DEBUG
static int invocationCount = 0;
#endif

jobject Environment::invoke( void* pComObject, ComMethod method, jobjectArray args, jint* convs, int retIndex, bool retIsInOut, jint retConv ) {
	Unmarshaller* unm = NULL;
	Unmarshaller* retUnm = NULL;

	void* result;

	const int paramLen = env->GetArrayLength(args);

	ffi_type** ffi_types = (ffi_type**)alloca((paramLen + 2) * sizeof(ffi_type*));
	void** ffi_values = (void**)alloca((paramLen + 2) * sizeof(void*)); // +2: 1 for 'this' and 1 for return value
	arg_value* c_args = (arg_value*)alloca((paramLen + 1)* sizeof(arg_value)); // this is +1 because 'this' doesn't need its own arg_value

	VARIANT* pvar;
	SAFEARRAY* psa;

	ffi_types[0] = &ffi_type_pointer;
	ffi_values[0] = &pComObject;

	int ffi_arg_count = 1;

	int javaParamIndex = 0;
	int comParamIndex = 0;

	for(comParamIndex = 0; comParamIndex <= paramLen; comParamIndex++ ) {
		unm = NULL;
		jobject arg = NULL;

		if( comParamIndex == retIndex && !retIsInOut) {
			switch(retConv) {
			case cvBSTR:
				retUnm = new BSTRUnmarshaller(NULL);
				break;
			
			case cvHRESULT:
				// this is a special case which we handle later
				// It must be retIndex==-1 when retConv==cvHRESULT .
				// So we should not be here.
				// But if it happened, try to fix it to the correct way.
				retIndex = -1;
				comParamIndex--;
				continue;
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

			case cvSAFEARRAY:
				retUnm = new SafeArrayUnmarshaller<safearray::SafeArrayXducer>(env,NULL);
				break;

			default:
				error(env,__FILE__,__LINE__,"unexpected conversion type: %d",retConv);
				return NULL;
			}

			if(retUnm!=NULL) {
				ffi_arg_count++;
				c_args[comParamIndex].v_ptr = retUnm->addr();
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
			}
		}else if(javaParamIndex < paramLen) {
			ffi_arg_count++;
			arg = env->GetObjectArrayElement(args,javaParamIndex);
			switch( convs[javaParamIndex] ) {
			case cvBSTR:
				c_args[comParamIndex].v_ptr = toBSTR((jstring)arg);
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvBSTR_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else {
					unm = new BSTRUnmarshaller(toBSTR((jstring)jholder(arg)->get(env)));
					add( new OutParamHandler( jholder(arg), unm ) );
					c_args[comParamIndex].v_ptr = unm->addr();
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvLPCWSTR:
				c_args[comParamIndex].v_ptr = (void*)toLPCWSTR((jstring)arg);
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvLPCSTR:
				c_args[comParamIndex].v_ptr = (void*)toLPCSTR((jstring)arg);
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvINT8:
				c_args[comParamIndex].v_int8 = javaLangNumber_byteValue(env,arg);
				ffi_types[comParamIndex + 1] = &ffi_type_sint8;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_int8;
				break;

			case cvINT8_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else {
					unm = new ByteUnmarshaller(env,jholder(arg)->get(env));
					add(new OutParamHandler( jholder(arg), unm ) );
					c_args[comParamIndex].v_ptr = unm->addr();
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvINT16:
				_ASSERT( sizeof(INT16)==sizeof(jshort) );
				c_args[comParamIndex].v_int16 = javaLangNumber_shortValue(env,arg);
				ffi_types[comParamIndex + 1] = &ffi_type_sint16;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_int16;
				break;

			case cvINT16_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else {
					unm = new ShortUnmarshaller(env,jholder(arg)->get(env));
					add(new OutParamHandler( jholder(arg), unm ) );
					c_args[comParamIndex].v_ptr = unm->addr();
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvINT32:
				c_args[comParamIndex].v_int32 = javaLangNumber_intValue(env,arg);
				ffi_types[comParamIndex + 1] = &ffi_type_sint32;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_int32;
				break;

			case cvComObject:
			case cvDISPATCH:
				c_args[comParamIndex].v_ptr = (void *)javaLangNumber_longValue(env,arg);
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvINT64:
				c_args[comParamIndex].v_int64 = javaLangNumber_longValue(env,arg);
				ffi_types[comParamIndex + 1] = &ffi_type_sint64;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_int64;
				break;

			case cvPVOID:
				if(arg==NULL)
					c_args[comParamIndex].v_ptr = NULL;
				else
					c_args[comParamIndex].v_ptr = env->GetDirectBufferAddress(arg);
				if(c_args[comParamIndex].v_ptr==NULL) {
					error(env,__FILE__,__LINE__,"the given Buffer object is not a direct buffer");
					return NULL;
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvINT32_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else {
					unm = new IntUnmarshaller(env,jholder(arg)->get(env));
					add( new OutParamHandler( jholder(arg), unm ) );
					c_args[comParamIndex].v_ptr = unm->addr();
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvDATE:
			case cvDouble:
				// TODO: check if this is correct
				c_args[comParamIndex].v_double = javaLangNumber_doubleValue(env,arg);
				ffi_types[comParamIndex + 1] = &ffi_type_double;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_double;
				break;

			case cvDouble_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else {
					unm = new DoubleUnmarshaller(env,jholder(arg)->get(env));
					add(new OutParamHandler( jholder(arg), unm ));
					c_args[comParamIndex].v_ptr = unm->addr();
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvFloat:
				c_args[comParamIndex].v_float = javaLangNumber_floatValue(env,arg);
				ffi_types[comParamIndex + 1] = &ffi_type_float;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_float;
				break;

			case cvFloat_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else {
					unm = new FloatUnmarshaller(env,jholder(arg)->get(env));
					add(new OutParamHandler( jholder(arg), unm ));
					c_args[comParamIndex].v_ptr = unm->addr();
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvBool:
				if(javaLangBoolean_booleanValue(env,arg)) {
					c_args[comParamIndex].v_int32 = TRUE;
				} else {
					c_args[comParamIndex].v_int32 = FALSE;
				}
				ffi_types[comParamIndex + 1] = &ffi_type_sint;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_int32;
				break;

			case cvVariantBool:
				if(javaLangBoolean_booleanValue(env,arg)) {
					c_args[comParamIndex].v_int32 = VARIANT_TRUE;
				} else {
					c_args[comParamIndex].v_int32 = VARIANT_FALSE;
				}
				ffi_types[comParamIndex + 1] = &ffi_type_sint;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_int32;
				break;

			case cvVariantBool_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else {
					unm = new VariantBoolUnmarshaller(env,jholder(arg)->get(env));
					add( new OutParamHandler( jholder(arg), unm ) );
					c_args[comParamIndex].v_ptr = unm->addr();
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvGUID:
				_ASSERT( sizeof(GUID)==sizeof(jlong)*2 );
				c_args[comParamIndex].v_ptr = env->GetLongArrayElements( (jlongArray)arg, NULL );
				add(new LongArrayCleanUp((jlongArray)arg,c_args[comParamIndex].v_ptr));
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvCURRENCY:
				if(arg==NULL) {
					c_args[comParamIndex].v_int64 = 0;
					ffi_types[comParamIndex + 1] = &ffi_type_sint64;
					ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_int16;
				} else {
					jstring strRep = javaMathBigDecimal_toString(env,arg);
					CComCurrency cy((LPCSTR)JString(env,strRep));
					c_args[comParamIndex].v_int64 = cy.m_currency.int64;
					ffi_types[comParamIndex + 1] = &ffi_type_sint64;
					ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_int64;
				}
				break;

			case cvCURRENCY_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else {
					jstring strRep = javaMathBigDecimal_toString(env,jholder(arg)->get(env));
					CComCurrency cy((LPCSTR)JString(env,strRep));
					
					unm = new CurrencyUnmarshaller(cy);
					add(new OutParamHandler( jholder(arg), unm ));
					c_args[comParamIndex].v_ptr = unm->addr();
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvVARIANT:
				if(arg==NULL) {
					pvar = &vtMissing;
				} else {
					pvar = convertToVariant(env,arg);
					if(pvar==NULL) {
						jstring name = javaLangClass_getName(env,env->GetObjectClass(arg));
						error(env,__FILE__,__LINE__,E_FAIL,"Unable to convert %s to VARIANT",LPCSTR(JString(env,name)));
						return NULL;
					}
					add(new VARIANTCleanUp(pvar));
				}
				ffi_types[comParamIndex + 1] = &ffi_type_variant;
				ffi_values[comParamIndex + 1] = pvar;
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
				c_args[comParamIndex].v_ptr = pvar;
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvSAFEARRAY:
				psa = safearray::SafeArrayXducer::toNative(env,(jarray)arg);
				if(psa==NULL) {
					error(env,__FILE__,__LINE__,"unable to convert the given array to SAFEARRAY");
					return NULL;
				}
				add( new SAFEARRAYCleanUp(psa) );
				c_args[comParamIndex].v_ptr = psa;
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			case cvSAFEARRAY_byRef:
				if(arg==NULL) {
					c_args[comParamIndex].v_ptr = NULL;
				} else if(env->IsSameObject(env->GetObjectClass(arg),com4j_Holder)) {
					// if it's a holder, convert its value, and prepare the unmarshaller
					// we don't know the inner type of array,
					// if we know that by add a paramemnt to the function, we could use
					// umn = new SafeArrayUnmarshaller<safearray::BasicArrayXducer<short>>(env, NULL);
					jobject o = jholder(arg)->get(env);
					unm = new SafeArrayUnmarshaller<safearray::SafeArrayXducer>(env, static_cast<jarray>(o));
					add( new OutParamHandler( jholder(arg), unm ) );	// after the method call unmarshal it back to SAFEARRAY
					SAFEARRAY** ppsa = static_cast<SAFEARRAY**>(unm->addr());
					c_args[comParamIndex].v_ptr = ppsa;
				} else {
					error(env,__FILE__,__LINE__,"unable to convert the given object to SAFEARRAY*");
					return NULL;
				}
				ffi_types[comParamIndex + 1] = &ffi_type_pointer;
				ffi_values[comParamIndex + 1] = &c_args[comParamIndex].v_ptr;
				break;

			default:
				error(env,__FILE__,__LINE__,"unexpected conversion type: %d",convs[javaParamIndex]);
				return NULL;
			}

			javaParamIndex++;

			if( comParamIndex == retIndex && retIsInOut) {
				// reuse the current unmarshaller
				if(unm==NULL) {
					error(env,__FILE__,__LINE__,"in/out return value must be passed by ref");
					return NULL;
				}
				retUnm = unm;
			}
		}
	}

#ifdef	_DEBUG
	invocationCount++;	// for debugging. this makes it easier to set a break-point.
#endif

	ffi_cif cif;
	ffi_status status = ffi_prep_cif(&cif, FFI_CALL_CONV, ffi_arg_count, &ffi_type_uint32, ffi_types);
	if (ffi_error(env, "Native call setup", status)) {
		return NULL;
	}
	ffi_call(&cif, method, &result, ffi_values);

	HRESULT hr = (HRESULT)result;

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
				str = env->NewString(pmsg,(jsize)wcslen(pmsg));
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
