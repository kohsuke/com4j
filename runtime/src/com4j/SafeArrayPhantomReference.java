package com4j;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * A phantom reference that owns the pointer to a native SAFEARRAY object.  When this
 * reference is enqueued, the ComThread will release the native reference.
 *
 * @author mpoindexter
 */
public class SafeArrayPhantomReference extends PhantomReference<SafeArray> implements NativeResourceReference {
	private long ptr;
	
	public SafeArrayPhantomReference(SafeArray array, ReferenceQueue<Object> queue, long ptr) {
		super(array, queue);
		this.ptr = ptr;
	}
	
	public void releaseNative() {
        if (ptr!=0) {
    		SafeArray.releaseArray(ptr);
            ptr = 0;
        }
	}
}
