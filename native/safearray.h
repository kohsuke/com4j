/*
	Defines conversion between Java arrays and SAFEARRAY.

	SafeArrayXducer::toNative is probably the most useful public entry point.
*/
#pragma once
#include "xducer2.h"

namespace safearray {
	
	// looks at the runtime type of the Java array and determines the correct type
	// for SAFEARRAY.
	class SafeArrayXducer {
	public:
		typedef SAFEARRAY*	NativeType;
		typedef jarray		JavaType;

		static NativeType toNative( JNIEnv* env, JavaType a ) {
			return toNative2(env,a).first;
		}
		// also returns the item type
		static pair<NativeType,VARTYPE> toNative2( JNIEnv* env, JavaType a );

		static JavaType toJava( JNIEnv* env, NativeType value );
	};

	// Transducer that turns a Java array into SAFEARRAY
	//
	// itemType : array item type
	// XDUCER : converter for each array item
	template < VARTYPE itemType, class XDUCER >
	class BasicArrayXducer {
	public:
		typedef array::Array<typename XDUCER::JavaType> JARRAY;
		typedef SAFEARRAY* NativeType;
		typedef jarray JavaType;

		static NativeType toNative( JNIEnv* env, JavaType javaArray ) {

			const int length = env->GetArrayLength(javaArray);

			// allocate SAFEARRAY
			SAFEARRAYBOUND bounds;
			bounds.cElements=length;
			bounds.lLbound=0;
			SAFEARRAY* psa = SafeArrayCreate(itemType,1,&bounds);
			

			XDUCER::JavaType* pSrc = JARRAY::lock(env,static_cast<JARRAY::ARRAY>(javaArray));
			XDUCER::NativeType* pDst;
			SafeArrayAccessData( psa, reinterpret_cast<void**>(&pDst) );

			for( int i=0; i<length; i++ )
				pDst[i] = XDUCER::toNative(env,pSrc[i]);

			JARRAY::unlock(env,static_cast<JARRAY::ARRAY>(javaArray),pSrc);
			SafeArrayUnaccessData( psa );

			return psa;
		}

		static JavaType toJava( JNIEnv* env, NativeType psa ) {
			XDUCER::NativeType* pSrc;

			long lbound,ubound; HRESULT hr;
			hr = SafeArrayGetLBound(psa,1,&lbound);
			hr = SafeArrayGetUBound(psa,1,&ubound);
			// sometimes SafeArrayGetUBound returns -1 with S_OK. I haven't figured out what that means
			int size = max(0,ubound-lbound+1);	// the range of index is [lbound,ubound]

			JARRAY::ARRAY a = JARRAY::newArray(env,size);
			XDUCER::JavaType* pDst = JARRAY::lock(env,a);
			SafeArrayAccessData( psa, reinterpret_cast<void**>(&pSrc) );

			for( int i=0; i<size; i++ ) {
				pDst[i] = XDUCER::toJava(env,pSrc[i]);
			}

			SafeArrayUnaccessData( psa );
			JARRAY::unlock(env,a,pDst);

			return a;
		}
	};

	// convert between SAFEARRAY and Java primitive array
	template < VARTYPE vt, class NT, class JT >
	class PrimitiveArrayXducer : public BasicArrayXducer< vt, xducer::IdentityXducer<NT,JT> > {
	};
}