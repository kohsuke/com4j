package com4j;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Thread managed by com4j.
 *
 * <p>
 * For each user thread that works with COM objects,
 * one {@link ComThread} is created to manage those objects.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (ScM, (C) 2008, 2009, Michael-Schnell@gmx.de)
 */
public final class ComThread extends Thread {

    /**
     * Used to associate a {@link ComThread} for every thread.
     */
    private static final ThreadLocal<ComThread> map = new ThreadLocal<ComThread>() {
        public ComThread initialValue() {
            if( isComThread() )
                return (ComThread)Thread.currentThread();
            else
                return new ComThread(Thread.currentThread());
        }
    };

    /**
     * Gets the {@link ComThread} associated with the current thread.
     */
    static ComThread get() {
        return map.get();
    }

    /**
     * Detaches the {@link ComThread} for the current thread (peer) by calling {@link #kill()}
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
    private ComThread(Thread peer) {
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
    private Task<?> taskListHead; // com4j issue 40
    private Task<?> taskListTail;

    /**
     * Collection of {@link Com4jObject}s that are managed by this thread.
     * <p>
     * This collection keeps track of all "living" COM objects. This is necessary to release all COM resources when the java process terminates.
     * </p>
     */
    private LiveObjectCollection liveObjects = new LiveObjectCollection();
    
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
        return die || (!peer.isAlive() && liveObjects.isEmpty());
    }

    /**
     * Kills this {@link ComThread} gracefully
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
        synchronized (threads) {
          threads.add(this);
        }
        try {
            run0();
        } finally {
          synchronized (threads) {
            threads.remove(this);
          }
        }
    }

    private void run0() {
        Native.coInitialize();

        while(!canExit()) {
            lock.suspend();

            synchronized(this) {
                // do any scheduled tasks that need to be done
                while(taskListHead != null) {
                    Task<?> task = taskListHead;
                    taskListHead = task.next;
                    task.invoke();
                }
                taskListTail = null; // taskListHead is null after the loop, so the tail should be null as well.
            }
        }

        // dispose all the live objects before we leave
        for (WeakReference<Com4jObject> object : liveObjects.getSnapshot()) {
          Com4jObject liveObject = object.get();
          if(liveObject != null) {
            liveObject.dispose();
          }
        }

        Native.coUninitialize();
    }

    /**
     * Executes a {@link Task} in a {@link ComThread}
     * and returns its result.
     * @param task The task to be executed
     * @param <T> The type of the return value.
     * @return The result of the Task
     */
    public <T> T execute(Task<T> task) {
        synchronized(task) {
            synchronized(this) {
                // add it to the tail
                if(taskListTail != null){
                    taskListTail.next = task;
                }
                taskListTail = task;
                if(taskListHead == null){
                    taskListHead = task;
                }
            }

            // invoke the execution
            lock.activate();

            // wait for the completion
            try {
                task.wait();
            } catch (InterruptedException e) {
                task.exception = e; // we got interrupted, so task.result will be invalid! 
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
     * Adds a {@link Com4jObject} to the live objects of this {@link ComThread}
     * <p>
     * This method increases the live object count of this thread and fires an
     * {@link ComObjectListener#onNewObject(Com4jObject)} event to all listeners.
     * </p>
     * @param r The new {@link Com4jObject}
     */
    public synchronized void addLiveObject( Com4jObject r ) {
        liveObjects.add(r);
        if(!listeners.isEmpty()) {
            for( int i=listeners.size()-1; i>=0; i-- )
                listeners.get(i).onNewObject(r);
        }
    }

    /**
     * Decrements the live object count of this {@link ComThread}
     */
    synchronized void removeLiveObject(Com4jObject object){
        liveObjects.remove(object);
    }
    
    /**
     * Checks if the current thread is a {@link ComThread}.
     */
    static boolean isComThread() {
        return Thread.currentThread() instanceof ComThread;
    }

    /**
     * Adds a {@link ComObjectListener} to this {@link ComThread}
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
     * Removes the {@link ComObjectListener} from this {@link ComThread}
     * @param listener The listener to remove
     * @throws IllegalArgumentException if the listener was not registered to this {@link ComThread}
     */
    public void removeListener(ComObjectListener listener) {
        if(!listeners.remove(listener))
            throw new IllegalArgumentException("listener isn't registered");
    }


    /**
     * All living and running {@link ComThread}s.
     */
    static final Set<ComThread> threads = Collections.synchronizedSet(new HashSet<ComThread>());

    static {
        // before shut-down clean up all ComThreads
        COM4J.addCom4JShutdownTask(new Runnable() {
            public void run() {
              // we need to synchronize the access to threads.
              // Not just to avoid concurrent modification, but also to make sure, that a kill-task is not waiting forever for the
              // thread to execute the task. (The thread might want to shut down itself concurrently, because the liveObjects dropped to zero.)
              ComThread[] threadsSnapshot;
              synchronized(threads) {
                threadsSnapshot = threads.toArray(new ComThread[threads.size()]);
              }

              for (ComThread thread : threadsSnapshot) {
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
        new DummyTask().execute();
    }

    /**
     * A task doing nothing.
     * @author scm
     */
    private static class DummyTask extends Task<Void>
    {
        @Override
        public Void call() {
            return null;
        }
    }
}
