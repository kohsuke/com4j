package com4j;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * A phantom reference that owns the pointer to a native com object.  When this
 * reference is enqueued, the ComThread will release the native reference.
 * 
 * @author mpoindexter
 *
 */
public class NativePointerPhantomReference extends PhantomReference<Wrapper> {
	private final long ptr;
	
	public NativePointerPhantomReference(Wrapper wrapper, ReferenceQueue<Wrapper> queue, long ptr) {
		super(wrapper, queue);
		this.ptr = ptr;
	}
	
	void releaseNative() {
		Native.release(ptr);
	}
}
