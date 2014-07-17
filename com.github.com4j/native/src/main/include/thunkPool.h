/*
	Manages memory for thunks.
*/
#pragma once


class ThunkPool {
	class MemoryBlock;

	// blocks that are allocated.
	// in practice the size of this will be very small
	list<MemoryBlock*> blocks;

	// number of thunks in one block
	int totalThunks;
	// page size 
	int pageSize;

	// size of one thunk
	const static int THUNK_SIZE = 28;

	// unit of memory allocation.
	// VirtualAlloc can only grab memory in a large block,
	// so we need two-level of free block control.
	class MemoryBlock {
		struct Thunk;

		ThunkPool& parent;
		
		// allocated memory
		void* ptr;
		// top of the free Thunk list
		Thunk* freeThunk;


		struct Thunk {
			MemoryBlock* const owner;
			union {
				// when memory is not in use, they form a linked list.
				struct {
					DWORD	stopper;
					Thunk*	nextFreeThunk;
				};
				byte space[THUNK_SIZE];
			};

			Thunk(MemoryBlock* owner) : owner(owner) {
				reclaim();
			}

			void release() {
				// 'CC' instruction is breakpoint.
				_ASSERT(stopper!=0xCCCCCCCC);
			}

			void reclaim() {
				stopper = 0xCCCCCCCC;
				nextFreeThunk = owner->freeThunk;
				owner->freeThunk = this;
			}

			Thunk* allocate() {
				_ASSERT(stopper==0xCCCCCCCC);
				stopper = 0;
				owner->freeThunk = nextFreeThunk;
				nextFreeThunk = NULL;
				return this;
			}
		};
	
		MemoryBlock(ThunkPool& _parent) : parent(_parent) {
			ptr = VirtualAlloc(NULL,parent.pageSize,MEM_COMMIT|MEM_RESERVE,PAGE_EXECUTE_READWRITE);
			
			freeThunk = NULL;

			Thunk* p = reinterpret_cast<Thunk*>(ptr);
			for( int i=parent.pageSize; i>0; i-=sizeof(Thunk) ) {
				new(p) Thunk(this);
				p++;
			}

			parent.blocks.push_front(this);
		}

		~MemoryBlock() {
			VirtualFree(ptr,0,MEM_RELEASE);
			ptr = NULL;
			list<MemoryBlock*>::iterator itr = find(parent.blocks,this);
			_ASSERT(itr!=parent.blocks.end());
			parent.blocks.erase(itr);
		}

		byte* allocate() {
			if(freeThunk==NULL)
				return NULL;	// nothing to allocate
			return freeThunk->allocate()->space;
		}

		friend ThunkPool;
	};

public:
	ThunkPool() {
		SYSTEM_INFO si;
		GetSystemInfo(&si);
		pageSize = si.dwPageSize;
		totalThunks = si.dwPageSize/sizeof(MemoryBlock::Thunk);
	}

	byte* allocate() {
		list<MemoryBlock*>::iterator itr = blocks.begin();
		while(itr!=blocks.end()) {
			byte* p = (*itr)->allocate();
			if(p!=NULL)
				return p;
			itr++;
		}

		// no free block. allocate new one
		MemoryBlock* b = new MemoryBlock(*this);
		byte* p = b->allocate();

		_ASSERT(p!=NULL);	// since it's a new block, allocation must succeed
		_ASSERT(find(blocks,b)==blocks.begin());	// the newly allocated block should register itself

		return p;
	}

	void free(byte* p) {
		typedef MemoryBlock::Thunk Thunk;

		p -= DWORD(reinterpret_cast<Thunk*>(NULL)->space);
		reinterpret_cast<Thunk*>(p)->release();
	}
};
