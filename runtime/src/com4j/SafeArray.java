package com4j;


/**
 * General purpose wrapper for COM SAFEARRAY.
 *
 * <p>
 * This class is provided for rare circumstances where the Java code
 * needs to control SAFEARRAY more precisely.
 *
 * <p>
 * Users are encouraged to use plain Java arrays
 * as much as possible. For example, the following Java method:
 * <pre>
 * void foo( short[] args );
 * </pre>
 * would be bridged to the following COM method:
 * <pre>
 * HRESULT foo( [in] SAFEARRAY(short)* args );
 * </pre>
 *
 * <p>
 * This works for the most of the cases, and is much easier to use.
 * 
 * For arrays with multiple dimensions this class should be used instead.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author mpoindexter (staticsnow@gmail.com)
 */
public final class SafeArray {
    /**
     * Pointer to the allocated SAFEARRAY.
     */
    private long ptr;
    private ComThread thread;
    private boolean isDestroyed;
    SafeArrayPhantomReference ref;
    boolean isTemporary = false;
    
    /**
     * Called from native code to create a SafeArray wrapping a native
     * SAFEARRAY
     * 
     * @param ptr
     * @param varType
     */
    private SafeArray( long ptr ) {
    	//System.out.println("Allocated 1 " + System.identityHashCode(this));
    	if(ptr==0)   throw new IllegalArgumentException();
        assert ComThread.isComThread();
        
    	this.ptr = ptr;
    	thread = ComThread.get();
        ref = new SafeArrayPhantomReference(this, thread.collectableObjects, ptr);
        thread.addLiveObject(this);
    }
    
    /**
     * Internal usage only.  Creates a SafeArray that wraps a Java array.  This method
     * is declared to take an Object since we want to accept Object[] subclasses as well
     * as any primitive array type.  Should only be called on the COM thread.
     * @param data
     */
    SafeArray( Object data ) {
    	//System.out.println("Allocated 2 " + System.identityHashCode(this));
    	assert ComThread.isComThread();
    	Variant.Type varType = Variant.Type.NO_TYPE;
    	
    	Class<?> ct = data.getClass().getComponentType();
    	if(Com4jObject.class.isAssignableFrom(ct)) {
    		varType = Variant.Type.VT_UNKNOWN;
    	}
    	
    	final Variant.Type type = varType;
    	
    	thread = ComThread.get();
    	ptr = createAndInit0(type.comEnumValue(), data);
    	ref = new SafeArrayPhantomReference(SafeArray.this, thread.collectableObjects, ptr);
    	thread.addLiveObject(SafeArray.this);
    }
    
    /**
     * Creates a new empty SAFEARRAY with the supplied type and bounds.
     * 
     * @param type
     * @param bounds
     */
    public SafeArray( final Variant.Type type, final Bound... bounds ) {
    	//System.out.println("Allocated 3 " + System.identityHashCode(this));
    	final int[] bl = new int[bounds.length * 2];
    	int i = 0;
    	for(Bound b : bounds) {
    		bl[i + 0] = b.lbound;
    		bl[i + 1] = b.ubound;
    		i += 2;
    	}
    	thread = ComThread.get();
    	
    	thread.execute(new Task<Void>() {
    		@Override
    		public Void call() {
    			ptr = create0(type.comEnumValue(), bl);
    	        ref = new SafeArrayPhantomReference(SafeArray.this, thread.collectableObjects, ptr);
    	        thread.addLiveObject(SafeArray.this);
    			return null;
    		}
    	});
    }

    /**
     * Bound of an array index.
     */
    public static final class Bound {
        public int lbound;
        public int ubound;
        
        public Bound() {
        }
        
        public Bound(int lbound, int ubound) {
        	this.lbound = lbound;
        	this.ubound = ubound;
        }
    }

    /**
     * Gets the element at the given index
     * @param indices
     * @return
     */
	public Object get( int... indices ) {
		if(isDestroyed) {
    		throw new IllegalStateException("SAFEARRAY already destroyed");
    	}
    	return get0(indices);
    }
    
    /**
     * Sets the element at the given index to the supplied value.
     * 
     * @param object
     * @param indices
     */
    public void set( Object object, int... indices ) {
    	if(isDestroyed) {
    		throw new IllegalStateException("SAFEARRAY already destroyed");
    	}
    	set0(object, indices);
    }
    
    public Variant.Type getVarType() {
    	if(isDestroyed) {
    		throw new IllegalStateException("SAFEARRAY already destroyed");
    	}
    	int vt = getVarType0();
    	for(Variant.Type t : Variant.Type.values()) {
    		if(t.comEnumValue() == vt) {
    			return t;
    		}
    	}
    	return null;
    }
    
    /**
     * Gets the bounds of this SAFEARRAY
     * @return
     */
    public Bound[] getBounds() {
    	if(isDestroyed) {
    		throw new IllegalStateException("SAFEARRAY already destroyed");
    	}
    	int[] bounds = getBounds0();
    	Bound[] ret = new Bound[bounds.length / 2];
    	for(int i = 0; i < bounds.length / 2; i++) {
    		ret[i] = new Bound();
    		ret[i].lbound = bounds[i * 2 + 0];
    		ret[i].ubound = bounds[i * 2 + 1];
    	}
    	return ret;
    }
    
    /**
     * Destroys this SafeArray and any underlying native SAFEARRAY
     */
    public void destroy() {
    	//System.out.println("Destroyed " + System.identityHashCode(this));
    	if(!isDestroyed) {
            new Task<Void>() {
                public Void call() {
                	destroy0();
                    return null;
                }
            }.execute(thread);
        }
    }
    
    private void destroy0() {
        if (!isDestroyed) {
            ref.releaseNative();
            ref.clear();
            isDestroyed = true;
        }
    }
    
    /**
     * Called from native code to inform the Java side
     * that the underlying SAFEARRAY has been destroyed
     * by the native code.  In this case, we clear our 
     * resource tracking ref and mark this wrapper as 
     * destroyed.
     */
    private void markNativeDestroyed() { 
    	//System.out.println("Native destroyed " + System.identityHashCode(this));
    	ref.clear();
    	isDestroyed = true;
    }
    
    static native void releaseArray(long ptr);
    private static native long create0(int type, int[] bounds);
    private static native long createAndInit0(int varType, Object data);
    private native int[] getBounds0();
    private native int getVarType0();
    private native Object get0(int[] indices); 
    private native void set0(Object object, int[] indices);
}
