#pragma once
#include "array.h"
#include "xducer.h"

namespace safearray {
	

	template < VARTYPE vt, class XDUCER >
	class BasicArrayConverter {
	public:
		typedef array::Array<XDUCER::JavaType> JARRAY;

		static SAFEARRAY* toNative( JNIEnv* env, jarray javaArray ) {

			const int length = env->GetArrayLength(javaArray);

			// allocate SAFEARRAY
			SAFEARRAYBOUND bounds;
			bounds.cElements=length;
			bounds.lLbound=0;
			SAFEARRAY* psa = SafeArrayCreate(vt,1,&bounds);
			

			XDUCER::JavaType* pSrc = JARRAY::lock(env,static_cast<JARRAY::ARRAY>(javaArray));
			XDUCER::NativeType* pDst;
			SafeArrayAccessData( psa, reinterpret_cast<void**>(&pDst) );

			for( int i=0; i<length; i++ )
				pDst[i] = XDUCER::toNative(env,pSrc[i]);

			JARRAY::unlock(env,static_cast<JARRAY::ARRAY>(javaArray),pSrc);
			SafeArrayUnaccessData( psa );

			return psa;
		}

		static jarray toJava( JNIEnv* env, SAFEARRAY* psa ) {
			XDUCER::NativeType* pSrc;

			long lbound,ubound;
			SafeArrayGetLBound(psa,1,&lbound);
			SafeArrayGetUBound(psa,1,&ubound);
			int size = ubound-lbound;

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
	class PrimitiveArrayConverter : public BasicArrayConverter< vt, xducer::IdentityXducer<NT,JT> > {
	};

}