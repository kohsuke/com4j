#include "stdafx.h"
#include "com4j.h"
#include "thunkPool.h"
#include "jnitl.h"

static ThunkPool thunkPool;

struct FunctionTable;
struct ComObject;

HRESULT __stdcall generalThunk( FunctionTable* pTable, int vtid, byte* args );

static bool operator < (const GUID& lhs, const GUID& rhs) {
	_ASSERT(sizeof(GUID)==16);
	const long* lp = reinterpret_cast<const long*>(&lhs);
	const long* rp = reinterpret_cast<const long*>(&rhs);
	
	for( int i=0; i<4; i++ ) {
		if(lp[i]<rp[i])
			return true;
		if(lp[i]>rp[i])
			return false;
	}

	return false;
}

/* Thunk Image
	__asm push esp;	// this push the old esp value
	__asm push 0x12345678;
	__asm push 0x12345678;
	__asm call generalThunk;
	__asm ret 0x1234;

	COM object memory layout:
	ms-help://MS.VSCC.v80/MS.MSDN.v80/MS.WIN32COM.v10.en/dncomg/html/msdn_com_co.htm
*/


/*
  Code block that invokes generalThunk
*/
#pragma pack(1)
struct Thunk {
	byte pushEsp;
	byte pushOp1;
	DWORD vtid;
	byte pushOp2;
	DWORD pTable;
	byte callOp;
	DWORD offset;
	byte retOp;
	WORD stackSize;

	Thunk( DWORD _vtid, FunctionTable* _pTable, WORD _stackSize) {
		pushEsp = 0x54;
		pushOp1 = pushOp2 = 0x68;
		vtid = _vtid;
		pTable = reinterpret_cast<DWORD>(_pTable);
		callOp = 0xE8;
		offset = reinterpret_cast<byte*>(&generalThunk) - reinterpret_cast<byte*>(&(this->retOp));
		retOp = 0xC2;
		stackSize = _stackSize;
	}
};



/*
	A Java object wrapped as a COM object.
*/
class JComObject {

public: // forward declaration
	struct TearOff;

private:
	map<GUID,TearOff> tearOffs;
	long refCount;

	~JComObject() {}	// freed through the release method

public:
	// per-interface structure
	struct TearOff {
		Thunk** const  vtbl;
		JComObject* const	owner;
		
		TearOff( JComObject* owner, Thunk** vtbl ) : vtbl(vtbl), owner(owner) {}

		operator IUnknown* () {
			return reinterpret_cast<IUnknown*>(this);
		}
	};

//	GlobalRef<jobject> object;
	jobject object;

	JComObject(jobject obj) : object(obj), refCount(0) {
		// TODO: fill in tearOffs
	}
	
	IUnknown* queryInterface( REFGUID guid ) {
		map<GUID,TearOff>::iterator v = tearOffs.find(guid);
		if(v==tearOffs.end())
			return NULL;
		else {
			addRef(); // for the returned interface
			return v->second;
		}
	}

	LONG addRef() {
		return InterlockedIncrement(&refCount);
	}
	LONG release() {
		long v = InterlockedDecrement(&refCount);
		if(v==0)
			delete this;
		return v;
	}
};


/*
	VTable for an interface
*/
struct FunctionTable {
private:
	jclass javaInterface;
	const int funcSize;
	Thunk* functions[]; //TODO: This causes the comiler warning C4200

	FunctionTable(int funcSize, int stackSize[]) : funcSize(funcSize) {
		for( int i=0; i<funcSize; i++ ) {
			functions[i] = new (thunkPool.allocate()) Thunk(i,this,stackSize[i]);
		}
	}

public:
	~FunctionTable() {
		for( int i=0; i<funcSize; i++ )
			thunkPool.free(reinterpret_cast<byte*>(functions[i]));
	}

	static FunctionTable* create(int funcSize, int stackSize[]) {
		byte* p = new byte[sizeof(FunctionTable)+sizeof(Thunk*)*funcSize];
		return new(p) FunctionTable(funcSize,stackSize);
	}
};

HRESULT __stdcall generalThunk( FunctionTable* pTable, int vtid, byte* args ) {
	// args+0 is the return address
	JComObject::TearOff* pInterface = *reinterpret_cast<JComObject::TearOff**>(args+4);
	JComObject* pThis = pInterface->owner;

	switch(vtid) {
	case 0:	// QueryInterface
		{
			GUID* iid = *reinterpret_cast<GUID**>(args+8);
			IUnknown** pOut = *reinterpret_cast<IUnknown***>(args+12);
			*pOut = NULL;
			return E_NOINTERFACE;
		}
	case 1: // AddRef
		return pThis->addRef();
	case 2: // Release
		return pThis->release();
	}

	// TODO

	return 0;
}

void __declspec(dllexport) foo() {
	puts("Hello");
	int x[] = {12,4,4};

	FunctionTable* ft = FunctionTable::create(3,x);
	JComObject* pObj = new JComObject(reinterpret_cast<jobject>(0x12345678));
	IUnknown* pUnk = pObj->queryInterface(__uuidof(IUnknown));
	pUnk->QueryInterface(reinterpret_cast<IDispatch**>(0x11223344));
	delete ft;
}