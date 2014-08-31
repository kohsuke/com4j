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
			ToJavaMultiDimlArrayMarshaller<itemType, XDUCER> m(env, psa);
			return m.getJava();
		}

	};

	// convert between SAFEARRAY and Java primitive array
	template < VARTYPE vt, class NT, class JT >
	class PrimitiveArrayXducer : public BasicArrayXducer< vt, xducer::IdentityXducer<NT,JT> > {
	};



	// Class to marshal SAFEARRAY to Java multi dimensional array
	//
	// itemType : array item type
	// XDUCER : converter for each array item
	template < VARTYPE itemType, class XDUCER >
	class ToJavaMultiDimlArrayMarshaller {
		typedef array::Array<typename XDUCER::JavaType> JARRAY;
		typedef SAFEARRAY* NativeType;
		typedef jarray JavaType;


	private:
		JNIEnv* env;
		NativeType psa;
		JavaType javaArray;
		int dim;
		int* dimSizes;
		typename XDUCER::NativeType* pSrc;

	public:
		ToJavaMultiDimlArrayMarshaller(JNIEnv* _env, NativeType _psa)
			: env(_env)
			, psa(_psa)
			, dim(0)
			, dimSizes(NULL)
			, pSrc(NULL)
		{
			dim = SafeArrayGetDim(psa);
			dimSizes = new int[dim];
			fillDimSizes(psa, dim, dimSizes);
			SafeArrayAccessData( psa, reinterpret_cast<void**>(&pSrc) );
		}

		~ToJavaMultiDimlArrayMarshaller() {
			SafeArrayUnaccessData( psa );
			delete[] dimSizes;
		}

		JavaType getJava() {
			return toJavaRec(pSrc, dim - 1);
		}

	private:

		JavaType toJavaRec(typename XDUCER::NativeType* &pSrc, int curDim) {
			if (curDim == 0) {
				JARRAY::ARRAY a = JARRAY::newArray(env, dimSizes[curDim]);
				XDUCER::JavaType* const pDst = JARRAY::lock(env, a);

				for( int i=0; i < dimSizes[curDim]; i++ ) {
					pDst[i] = XDUCER::toJava(env, *(pSrc++));
				}
				JARRAY::unlock(env, a, pDst);
				return a;
			}
			else {
				jobjectArray a = array::Array<jobject>::newArray(env, dimSizes[curDim]);
				jobject* const pDst = array::Array<jobject>::lock(env, a);

				for( int i = 0; i < dimSizes[curDim]; i++ ) {
					pDst[i] = toJavaRec(pSrc, curDim - 1);
				}
				array::Array<jobject>::unlock(env, a, pDst);
				return a;
			}

		}

		static void fillDimSizes(NativeType psa, int dim, int *dimSizes) {
			for (int i = 1; i <= dim; ++i) {
				long lbound,ubound;
				SafeArrayGetLBound(psa,i,&lbound);
				SafeArrayGetUBound(psa,i,&ubound);
				// sometimes SafeArrayGetUBound returns -1 with S_OK. I haven't figured out what that means
				dimSizes[i-1] = max(0,ubound-lbound+1);	// the range of index is [lbound,ubound]
			}
		}


	};




	// Transducer that turns a Java multi dimensinal array into SAFEARRAY
	//
	// itemType : array item type
	// XDUCER : converter for each array item
	template < class XDUCER >
	class MultiDimArrayXducer {
	public:
		typedef SAFEARRAY* NativeType;
		typedef jarray JavaType;

		static NativeType toNative( JNIEnv* env, JavaType javaArray ) {
			ToNativeMultiDimlArrayMarshaller<VT_VARIANT, XDUCER> m(env, javaArray);
			return m.getNative();
		}

	};



	// Class to marshall Java multi dimensional arrays to SAFEARRAY
	//
	// itemType : array item type
	// XDUCER : converter for each array item
	template < VARTYPE itemType, class XDUCER >
	class ToNativeMultiDimlArrayMarshaller {
		typedef array::Array<typename XDUCER::JavaType> JARRAY;
		typedef SAFEARRAY* NativeType;
		typedef jarray JavaType;


	private:
		JNIEnv* env;
		JavaType javaArray;
		int dim;
		SAFEARRAYBOUND* bounds;
		SAFEARRAY* psa;
		typename XDUCER::NativeType* pDst;

	public:
		ToNativeMultiDimlArrayMarshaller(JNIEnv* _env, JavaType _javaArray)
			: env(_env)
			, javaArray(_javaArray)
			, dim(0)
			, bounds(NULL)
			, psa(NULL)
			, pDst(NULL)
		{
			dim = getArrayDimension(env, javaArray);
			bounds = new SAFEARRAYBOUND[dim];
			fillBounds(env, javaArray, dim, bounds);
			psa = SafeArrayCreate(itemType, dim, bounds);
			SafeArrayAccessData( psa, reinterpret_cast<void**>(&pDst) );
		}

		~ToNativeMultiDimlArrayMarshaller() {
			SafeArrayUnaccessData( psa );
			delete[] bounds;
		}

		SAFEARRAY* getNative() {
			toNativeRec(javaArray, dim - 1);
			return psa;
		}

	private:
		void toNativeRec(JavaType _array, int curDim) {

			if (curDim == 0) {
				XDUCER::JavaType* pSrc = JARRAY::lock(env,static_cast<JARRAY::ARRAY>(_array));

				for(size_t i = 0; i < bounds[curDim].cElements; i++)
					*(pDst++) = XDUCER::toNative(env, pSrc[i]);

				JARRAY::unlock(env,static_cast<JARRAY::ARRAY>(_array), pSrc);

			}
			else {
				JavaType* pSrc = array::Array<JavaType>::lock(env, static_cast<JARRAY::ARRAY>(_array));

				for(size_t i = 0; i < bounds[curDim].cElements; i++)
					toNativeRec(pSrc[i], curDim - 1);

				array::Array<JavaType>::unlock(env,static_cast<JARRAY::ARRAY>(_array), pSrc);
			}
		}

		// gets dimensions of java multi index array
		static int getArrayDimension(JNIEnv* env, jobject a ) {
			int dim = 0;
			while(env->IsInstanceOf(a, objectArray)) {
				dim++;

				if (env->GetArrayLength(static_cast<jobjectArray>(a)) > 0)
					a = env->GetObjectArrayElement(static_cast<jobjectArray>(a), 0);
				else
					return dim;
			};

			return dim;
		}

		// fills SAFEARRAYBOUND structure base on java array
		static void fillBounds(JNIEnv* env, jobject a, int n, SAFEARRAYBOUND* bounds) {
			int i = n - 1;
			while(true) {
				bounds[i].lLbound = 0;
				bounds[i].cElements = env->GetArrayLength(static_cast<jobjectArray>(a));

				if (i <= 0)
					break;

				a = env->GetObjectArrayElement(static_cast<jobjectArray>(a), 0);
				i--;
			}
		}

	};

	
}