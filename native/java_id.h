#pragma once
//
// helper classes to cache jclass/jmethodID/jfieldID
//

// obtains the global jclass objects
class JClassID {
private:
	jclass clazz;
	const char* const name;	// class name
	
	// all JClassID instances are linked by a chain
	JClassID* next;

	static JClassID* init;

	void setup( JNIEnv* env ) {
		_ASSERT(clazz==NULL);
		clazz = static_cast<jclass>(env->NewGlobalRef(env->FindClass(name)));
		_ASSERT(clazz!=NULL);
	}

public:
	JClassID( const char* _name ) : name(_name), next(init) {
		next = init;
		init = this;
	}

	operator jclass () const {
		_ASSERT(clazz!=NULL);
		return clazz;
	}

	// called once when the system is initialized.
	static void runInit( JNIEnv* env ) {
		for( ; init!=NULL; init=init->next )
			init->setup(env);
	}
};


class JMethodID {
	jmethodID id;
	JClassID& clazz;
	const char* name;
	const char* sig;

	void setup( JNIEnv* env ) {
		id = env->GetMethodID( clazz, name, sig );
	}

	// all JClassID instances are linked by a chain
	JMethodID* next;

	static JMethodID* init;

public:
	JMethodID( JClassID& _clazz, const char* _name, const char* _sig ) : clazz(_clazz) {
		name = _name;
		sig = _sig;

		next = init;
		init = this;
	}

	operator jmethodID () const {
		return id;
	}

	JClassID& getClazz() { return clazz; }

	// called once when the system is initialized.
	static void runInit( JNIEnv* env ) {
		for( ; init!=NULL; init=init->next )
			init->setup(env);
	}
};




extern JClassID javaLangNumber;
extern JMethodID javaLangNumber_intValue;
extern JClassID javaLangInteger;
extern JMethodID javaLangInteger_new;
// reference to org.kohsuke.com4j.comexception
extern JClassID comexception;
extern JMethodID comexception_new;
extern JClassID com4j_Holder;
extern jfieldID com4j_Holder_value;
