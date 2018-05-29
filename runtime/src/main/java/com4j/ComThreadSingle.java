package com4j;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ComThread that enforces all calls to a COM object are performed on the same
 * thread that created the object.
 *
 * <p>
 * Usually {@link ComThreadMulti} is used so that COM objects can be passed
 * between threads, but when a COM object is constructed outside of Com4j
 * it may not be possible to marshal it to one of the Com4j threads, and
 * so this class is used instead.
 *
 * @author Tony Roberts (tony@pyxll.com)
 */
public class ComThreadSingle implements ComThread {

    /**
     * Used to associate a {@link ComThreadSingle} for every thread using this class.
     */
    private static final ThreadLocal<ComThreadSingle> map = new ThreadLocal<ComThreadSingle>() {
        public ComThreadSingle initialValue() {
            return new ComThreadSingle(Thread.currentThread());
        }
    };

    /**
     * Gets the {@link ComThreadSingle} associated with the current thread.
     */
    static ComThreadSingle get() {
        return map.get();
    }

    /**
     * COM objects that this thread is managing. This thread needs to stick around until they are all gone,
     * even when the peer is dead, because other threads might still want to talk to these objects.
     */
    private Set<NativePointerPhantomReference> liveComObjects = new HashSet<NativePointerPhantomReference>();

    /**
     * Listeners attached to this thread.
     */
    private final List<ComObjectListener> listeners = new ArrayList<ComObjectListener>();

    /**
     * The actual thread.
     */
    private final Thread thread;

    private ComThreadSingle(Thread thread) {
        this.thread = thread;
    }

    public boolean isCurrentThread() {
        return Thread.currentThread() == thread;
    }

    /**
     * Keeps track of wrappers that should be IUnknown::release-d.
     */
    final ReferenceQueue<Wrapper> collectableObjects = new ReferenceQueue<Wrapper>();

    public ReferenceQueue<Wrapper> getCollectableObjects() {
        return collectableObjects;
    }

    /**
     * Runs the task in the current thread, and checks that current thread
     * is the one associated with this object.
     */
    public <T> T execute(Task<T> task) {
        // Check we can execute on the current thread
        if (!isCurrentThread()) {
            throw new RuntimeException("Attempted to call COM object from the wrong thread");
        }

        // Call the task
        T result = task.call();

        // Collect any garbage produced by the call
        // TODO call this periodically from a windows message loop for this thread
        collectGarbage();

        return result;
    }

    /**
     * Adds a {@link Com4jObject} to the live objects of this {@link ComThreadMulti}
     * <p>
     * This method increases the live object count of this thread and fires an
     * {@link ComObjectListener#onNewObject(Com4jObject)} event to all listeners.
     * </p>
     * @param r The new {@link Com4jObject}
     */
    public synchronized void addLiveObject( Com4jObject r ) {
        if(r instanceof Wrapper) {
            liveComObjects.add(((Wrapper)r).ref);
        }

        if(!listeners.isEmpty()) {
            for( int i=listeners.size()-1; i>=0; i-- )
                listeners.get(i).onNewObject(r);
        }
    }

    /**
     * Adds a {@link ComObjectListener} to this {@link ComThreadSingle}
     * @param listener the new listener
     * @throws IllegalArgumentException if the <code>listener</code> is <code>null</code> or if the listener is already registered.
     */
    public void addListener(ComObjectListener listener) {
        if(listener==null)
            throw new IllegalArgumentException("listener is null");
        if(listeners.contains(listener))
            throw new IllegalArgumentException("can't register the same listener twice");
        listeners.add(listener);
    }

    /**
     * Removes the {@link ComObjectListener} from this {@link ComThreadSingle}
     * @param listener The listener to remove
     * @throws IllegalArgumentException if the listener was not registered to this {@link ComThreadSingle}
     */
    public void removeListener(ComObjectListener listener) {
        if(!listeners.remove(listener))
            throw new IllegalArgumentException("listener isn't registered");
    }

    /**
     * Cleans up any left over references
     */
    private void collectGarbage() {
        // dispose unused objects if any
        NativePointerPhantomReference toCollect;
        while((toCollect = (NativePointerPhantomReference)collectableObjects.poll()) != null) {
            liveComObjects.remove(toCollect);
            toCollect.clear();
            toCollect.releaseNative();
        }
    }

    /**
     * Collects uncollected garbage and removes thread local instance.
     */
    static void detach() {
        get().collectGarbage();
        map.remove();
    }
}
