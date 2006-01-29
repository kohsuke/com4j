package com4j;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;


/**
 * Thread managed by com4j.
 *
 * <p>
 * For each user thread that works with COM objects,
 * one {@link ComThread} is created to manage those objects.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
final class ComThread extends Thread {

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

    static void detach() {
        map.get().kill();
        map.remove();
    }



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
     * {@link Wrapper}s that are no longer referenced from anybody else
     * are kept in this linked list, so that this thread can dispose them.
     */
    private Wrapper freeList;

    /**
     * Tasks that need to be processed.
     */
    private Task<?> taskList;

    /**
     * Number of {@link Wrapper} objects that this thread manages.
     */
    private int liveObjects = 0;

    /**
     * Listeners attached to this thread.
     */
    private final List<ComObjectListener> listeners = new ArrayList<ComObjectListener>();

    /**
     * If set to true, this thread will commit suicide.
     */
    private boolean die = false;

    /**
     * Returns true if this thread can exit.
     */
    private boolean canExit() {
        // lhs:forcible death <->  rhs:natural death
        return die || (!peer.isAlive() && liveObjects==0);
    }

    public void kill() {
        new Task<Void>() {
            public Void call() {
                // this thread is going to shut down.
                // if the master thread needs a ComThread again,
                // we'll create a new thread.
                die = true;
                return null;
            }
        }.execute();

        // wait for it to die. if someone interrupts us, process that later.
        try {
            join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void run() {
        threads.add(this);
        try {
            run0();
        } finally {
            threads.remove(this);
        }
    }

    private synchronized void run0() {
        Native.coInitialize();

        // TODO: we need to run Windows message pump
        while(!canExit()) {
            try {
                wait(1000);
            } catch (InterruptedException e) {}

            // dispose unused objects if any
            while(freeList!=null) {
                if(freeList.dispose0())
                    liveObjects--;
                freeList = freeList.next;
            }

            // do any scheduled tasks that need to be done
            while(taskList!=null) {
                Task<?> task = taskList;
                taskList = task.next;
                task.invoke();
            }
        }

        Native.coUninitialize();
    }

    /**
     * Adds the given object to the free list
     */
    synchronized void addToFreeList(Wrapper wrapper) {
        assert wrapper.next==null;
        wrapper.next = freeList;
        freeList = wrapper;
    }

    /**
     * Executes a {link Task} in a {@link ComThread}
     * and returns its result.
     */
    public <T> T execute(Task<T> task) {
        synchronized(task) {
            synchronized(this) {
                // add it to the link
                task.next = taskList;
                taskList = task;

                // invoke the execution
                notify();
            }
            // wait for the completion
            try {
                task.wait();
            } catch (InterruptedException e) {}

            if(task.exception!=null) {
                RuntimeException e = task.exception;
                task.exception = null;
                throw new ExecutionException(e);
            } else {
                T r = task.result;
                task.result = null;
                return r;
            }
        }
    }

    public synchronized void addLiveObject( Com4jObject r ) {
        liveObjects++;
        if(!listeners.isEmpty()) {
            for( int i=listeners.size()-1; i>=0; i-- )
                listeners.get(i).onNewObject(r);
        }
    }

    /**
     * Checks if the current thread is a COM thread.
     */
    static boolean isComThread() {
        return Thread.currentThread() instanceof ComThread;
    }

    public void addListener(ComObjectListener listener) {
        if(listener==null)
            throw new IllegalArgumentException("listener is null");
        if(listeners.contains(listener))
            throw new IllegalArgumentException("can't register the same listener twice");
        listeners.add(listener);
    }

    public void removeListener(ComObjectListener listener) {
        if(!listeners.remove(listener))
            throw new IllegalArgumentException("listener isn't registered");
    }


    /**
     * Live {@link ComThread}s.
     */
    static final Set<ComThread> threads = Collections.synchronizedSet(new HashSet<ComThread>());

    static {
        // before shut-down clean up all ComThreads
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                // move threads to array to avoid concurrent modification
                for (ComThread thread : threads.toArray(new ComThread[threads.size()]) ) {
                    thread.kill();
                }
            }
        });
    }
}
