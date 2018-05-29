package com4j;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * Thread managed by com4j.
 *
 * <p>
 * For each user thread that works with COM objects,
 * one {@link ComThreadMulti} is created to manage those objects.
 *
 * <p>
 * This is because COM objects are inherently tied to the thread that created it,
 * and therefore all the invocations must be routed through the creator thread.
 * See http://msdn.microsoft.com/en-us/library/ms809971.aspx for more discussions.
 *
 * <p>
 * This model is rather alien to Java developers, where objects can be passed between
 * threads more freely. (This is a separate issue from whether those objects can be
 * safely accessed concurrently.)
 *
 * <p>
 * To bridge these gaps, we don't let application threads touch COM objects at all,
 * and instead create {@link ComThreadMulti} as a shadow thread for each application thread who wants to
 * create a COM object.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (ScM, (C) 2008, 2009, Michael-Schnell@gmx.de)
 * @author mpoindexter (staticsnow@gmail.com)
 */
public final class ComThreadMulti extends Thread implements ComThread {

	public static int GARBAGE_COLLECTION_INTERVAL = 10;
    
	/**
     * Used to associate a {@link ComThreadMulti} for every thread.
     */
    private static final ThreadLocal<ComThreadMulti> map = new ThreadLocal<ComThreadMulti>() {
        public ComThreadMulti initialValue() {
            if( Thread.currentThread() instanceof ComThreadMulti)
                return (ComThreadMulti)Thread.currentThread();
            else
                return new ComThreadMulti(Thread.currentThread());
        }
    };

    /**
     * Gets the {@link ComThreadMulti} associated with the current thread.
     */
    static ComThreadMulti get() {
        return map.get();
    }

    /**
     * Detaches the {@link ComThreadMulti} for the current thread (peer) by calling {@link #kill()}
     */
    static void detach() {
        map.get().kill();
        try {
          map.get().join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        map.remove();
    }

    /**
     * Constructs a new ComThread for the given peer and starts it.
     * @param peer The peer thread.
     */
    private ComThreadMulti(Thread peer) {
        super("ComThread for "+peer.getName());
        this.peer = peer;
        setDaemon(true);    // we don't want to block the JVM from exiting
        start();
    }

    /**
     * The peer thread.
     */
    private final Thread peer;

    /**
     * Tasks that need to be processed.
     */
    private final List<Task<?>> taskList = Collections.synchronizedList((new LinkedList<Task<?>>()));// com4j issue 70

    /**
     * COM objects that this thread is managing. This thread needs to stick around until they are all gone,
     * even when the peer is dead, because other threads might still want to talk to these objects.
     */
    private Set<NativePointerPhantomReference> liveComObjects = new HashSet<NativePointerPhantomReference>();

    /**
     * Keeps track of wrappers that should be IUnknown::release-d.
     */
    final ReferenceQueue<Wrapper> collectableObjects = new ReferenceQueue<Wrapper>();

    public ReferenceQueue<Wrapper> getCollectableObjects() {
        return collectableObjects;
    }

    /**
     * Listeners attached to this thread.
     */
    private final List<ComObjectListener> listeners = new ArrayList<ComObjectListener>();

    /**
     * If set to true, this thread will commit suicide.
     */
    private volatile boolean die = false;

    /**
     * Used instead of the monitor of an object, so that we can run
     * a message loop while waiting.
     */
    private final Win32Lock lock = new Win32Lock();

    /**
     * Returns true if this thread can exit.
     * <p>
     * If die is true, this method returns true. Otherwise it returns true, if the peer thread is not
     * alive any more and all liveObjects have been removed.
     * </p>
     */
    private boolean canExit() {
        // lhs:forcible death <->  rhs:natural death
        return die || (!peer.isAlive() && liveComObjects.isEmpty());
    }

    /**
     * Kills this {@link ComThreadMulti} gracefully
     * and blocks until a thread dies.
     */
    public void kill() {
        die = true;
        lock.activate(); // wake up the sleeping thread.

        // wait for it to die. if someone interrupts us, process that later.
        try {
            join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void run() {
        threads.add(this);
        try {
            run0();
        } finally {
            threads.remove(this);
        }
    }

    private void run0() {
        Native.coInitialize();

        while(!canExit()) {
            lock.suspend(GARBAGE_COLLECTION_INTERVAL);

            //Clean up any com objects that need releasing
            collectGarbage();

            // do any scheduled tasks that need to be done
            while (!taskList.isEmpty()) {
                Task<?> task = taskList.get(0);
                taskList.remove(0);
                task.invoke();

                // wake up the waiting thread
                lock.activate();
                
                //Maybe the task produced some garbage...clean that up
                collectGarbage();
            }
        }

        collectGarbage();
        
        //And clobber any live COM objects that have not been dispose()'d to avoid
        //leaking these objects on die
        for(NativePointerPhantomReference ref : liveComObjects) {
        	ref.clear();
        	ref.releaseNative();
        }
        liveComObjects.clear();
        
        //Kill the event handle we are holding in the lock.
        lock.dispose();

        Native.coUninitialize();
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
     * Executes a {@link Task} in a {@link ComThreadMulti}
     * and returns its result.
     * @param task The task to be executed
     * @param <T> The type of the return value.
     * @return The result of the Task
     */
    public <T> T execute(Task<T> task) {
        synchronized(task) {
            task.reset();
            // add it to the tail
            taskList.add(task);

            // invoke the execution
            lock.activate();

            // wait for the completion
            try {
                while (!task.isDone()) {
                    //Native.pumpWaitingMessages();
                    task.wait();
                }
            }
            catch (InterruptedException e) {
                task.exception = e;
            }

            if(task.exception!=null) {
                Throwable e = task.exception;
                task.exception = null;
                throw new ExecutionException(e);
            } else {
                T r = task.result;
                task.result = null;
                return r;
            }
        }
    }

    /**
     * Adds a {@link Com4jObject} to the live objects of this {@link ComThreadMulti}
     * <p>
     * This method increases the live object count of this thread and fires an
     * {@link ComObjectListener#onNewObject(Com4jObject)} event to all listeners.
     * </p>
     * @param r The new {@link Com4jObject}
     */
    public synchronized void addLiveObject( Com4jObject r ) {// TODO: why is this public?
    	if(r instanceof Wrapper) {
    		liveComObjects.add(((Wrapper)r).ref);
    	}
        
        if(!listeners.isEmpty()) {
            for( int i=listeners.size()-1; i>=0; i-- )
                listeners.get(i).onNewObject(r);
        }
    }
    
    /**
     * Checks if the current thread is this instance of ComThreadSafe;
     */
    public boolean isCurrentThread() {
        return Thread.currentThread() == this;
    }

    /**
     * Adds a {@link ComObjectListener} to this {@link ComThreadMulti}
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
     * Removes the {@link ComObjectListener} from this {@link ComThreadMulti}
     * @param listener The listener to remove
     * @throws IllegalArgumentException if the listener was not registered to this {@link ComThreadMulti}
     */
    public void removeListener(ComObjectListener listener) {
        if(!listeners.remove(listener))
            throw new IllegalArgumentException("listener isn't registered");
    }


    /**
     * All living and running {@link ComThreadMulti}s.
     */
    static final Set<ComThreadMulti> threads = Collections.synchronizedSet(new HashSet<ComThreadMulti>());

    static {
        // before shut-down clean up all ComThreads
        COM4J.addCom4JShutdownTask(new Runnable() {
            public void run() {
              // we need to synchronize the access to threads.
              // Not just to avoid concurrent modification, but also to make sure, that a kill-task is not waiting forever for the
              // thread to execute the task. (The thread might want to shut down itself concurrently, because the liveObjects dropped to zero.)
              ComThreadMulti[] threadsSnapshot;
              synchronized(threads) {
                threadsSnapshot = threads.toArray(new ComThreadMulti[threads.size()]);
              }

              for (ComThreadMulti thread : threadsSnapshot) {
                  thread.kill();
              }
            }
        });
    }

    /**
     * This method calls System.gc() and executes a dummy task to initiate the corresponding
     * ComThread to call dispose0() on all waiting objects.
     *
     * This method is mainly for debug purposes.
     */
    public static void flushFreeList() {
        System.gc();
        ComThreadMulti.get().lock.activate();
    }
}
