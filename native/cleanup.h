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
