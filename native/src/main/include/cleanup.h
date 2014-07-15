#pragma once

// executes something after the method call is completed.
class PostAction {
public:
	PostAction* next;
	virtual void act( JNIEnv* env )=0;
	virtual ~PostAction() {}
};



//
//
// CleanUp implementations
//
//

class BSTRCleanUp : public PostAction {
	BSTR bstr;
public:
	BSTRCleanUp( BSTR _bstr ) {
		bstr = _bstr;
	}
	void act( JNIEnv* env ) {
		SysFreeString(bstr);
		bstr=NULL;
	}
};

// deallocate VARIANT by using VariantClear & delete
class VARIANTCleanUp : public PostAction {
	VARIANT* pvar;
public:
	VARIANTCleanUp( VARIANT* _pvar ) : pvar(_pvar) {
		_ASSERT(pvar!=NULL);
	}
	void act( JNIEnv* env ) {
		VariantClear(pvar);
		delete pvar;
		pvar = NULL;
	}
};

class SAFEARRAYCleanUp : public PostAction {
	SAFEARRAY* psa;
public:
	SAFEARRAYCleanUp( SAFEARRAY* _psa ) {
		psa = _psa;
	}
	void act( JNIEnv* env ) {
		SafeArrayDestroy(psa);
		psa = NULL;
	}
};

template <class T>
class DeleteCleanUp : public PostAction {
	T*	p;

public:
	DeleteCleanUp( T* _p ) {
		p = _p;
	}
	void act( JNIEnv* env ) {
		if(p!=NULL)
			delete p;
		p = NULL;
	}
};

class LongArrayCleanUp : public PostAction {
	jlongArray array;
	jlong* buf;
public:
	LongArrayCleanUp( jlongArray _array, void* _buf ) {
		array = _array;
		buf = static_cast<jlong*>(_buf);
	}
	void act( JNIEnv* env ) {
		env->ReleaseLongArrayElements( array, buf, 0 );
		array = NULL;
		buf = NULL;
	}
};