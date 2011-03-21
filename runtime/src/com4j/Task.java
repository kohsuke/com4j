package com4j;

import java.util.concurrent.Callable;

/**
 * Used to execute a chunk of code from {@link ComThread}.
 *
 * @param <T> the type of the return value of the {@link #execute()} and {@link #execute(ComThread)} methods
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
abstract class Task<T> implements Callable<T> {
    public abstract T call();


    /**
     * Executes the task in a {@link ComThread}
     * @return the return value of the Task execution (returned by {@link #call()}).
     */
    public final T execute() {
        if( ComThread.isComThread() )
            // if invoked from within ComThread, execute it at once
            return call();
        else
            // otherwise schedule the execution and block
            return ComThread.get().execute(this);
    }

    /**
     * Executes the task in the given {@link ComThread}
     * @param t the ComThread to execute the task
     * @return the return value of the Task execution (returned by {@link #call()}).
     */
    public final T execute(ComThread t) {
        if(Thread.currentThread()==t)
            // if invoked from within ComThread, execute it at once
            return call();
        else
            // otherwise schedule the execution and block
            return t.execute(this);
    }

    /**
     * Called from {@link ComThread} to run the task.
     */
    final synchronized void invoke() {
        next = null;

        result = null;
        exception = null;
        try {
            result = call();
        } catch( Throwable e ) {
            exception = e;
        }

        // let the calling thread know that we are done.
        notify();
    }

    /**
     * Managed by {@link ComThread} to form a linked list from
     * {@link ComThread#taskListHead}.
     */
    Task<?> next;

    /**
     * Managed by {@link ComThread} to pass the return value
     * across threads.
     */
    T result;

    /**
     * Managed by {@link ComThread} to pass the exception
     * across threads.
     */
    Throwable exception;

    /**
     * TODO: do we need this field at all?
     */
    Error error;
}
