package com4j;

import java.lang.*;

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

    private static final ThreadLocal<ComThread> map = new ThreadLocal<ComThread>() {
        public ComThread initialValue() {
            return new ComThread(Thread.currentThread());
        }
    };

    /**
     * Gets the {@link ComThread} associated with the current thread.
     */
    static ComThread get() {
        Thread t = Thread.currentThread();
        if(t instanceof ComThread)
            return (ComThread)t;
        else
            return map.get();
    }



    private ComThread(Thread peer) {
        this.peer = peer;
        start();
    }

    /**
     * The peer thread.
     */
    private final Thread peer;

    /**
     * {@link Wrapper}s that are no longer referenced from anybody else
     * are kept in this linked list, so that this thread can release them.
     */
    private Wrapper freeList;

    /**
     * Tasks that need to be processed.
     */
    private Task taskList;

    /**
     * Number of {@link Wrapper} objects that this thread manages.
     */
    private int liveObjects = 0;

    /**
     * Returns true if this thread can exit.
     */
    private boolean canExit() {
        return !peer.isAlive() && liveObjects==0;
    }

    public synchronized void run() {
        while(!canExit()) {
            try {
                wait(10*1000);
            } catch (InterruptedException e) {}

            // release unused objects if any
            while(freeList!=null) {
                freeList.dispose0();
                freeList = freeList.next;
                liveObjects--;
            }

            // do any scheduled tasks that need to be done
            while(taskList!=null) {
                Task task = taskList;
                taskList = task.next;
                synchronized(task) {
                    task.next = null;
                    task.run();
                    task.notify();
                }
            }
        }
    }

    /**
     * Adds the given object to the free list
     */
    synchronized void addToFreeList(Wrapper wrapper) {
        assert wrapper.next==null;
        wrapper.next = freeList;
        freeList = wrapper;
    }

    public void execute(Task task) {
        synchronized(task) {
            synchronized(this) {
                // add it to the link
                task.next = taskList;
                taskList = task;

                // invoke the execution
                notify();
            }
            try {
                task.wait();
            } catch (InterruptedException e) {}
        }
    }

    public synchronized void addLiveObject() {
        liveObjects++;
    }
}
