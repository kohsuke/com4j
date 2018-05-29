package com4j;

import java.util.concurrent.Callable;

/**
 * Used to execute a chunk of code from {@link ComThread}.
 *
 * @param <T> the type of the return value of the {@link #execute()} and {@link #execute(ComThread)} methods
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
abstract class Task<T> implements Callable<T> {
	private volatile boolean done = false;
    public abstract T call();

    private static ThreadLocal<ComThread> comThread = new ThreadLocal<ComThread>();

    /**
     * Returns the current ComThread the task is running in.
     * Only valid while running a Task.
     */
    public static ComThread getComThread() {
        return null == comThread ? null : comThread.get();
    }

    /**
     * Executes the task in a {@link ComThread}
     * @return the return value of the Task execution (returned by {@link #call()}).
     */
    public final T execute() {
        return execute(ComThreadMulti.get());
    }

    /**
     * Executes the task in the given {@link ComThread}
     * @param t the ComThread to execute the task
     * @return the return value of the Task execution (returned by {@link #call()}).
     */
    public final T execute(ComThread t) {
        comThread.set(t);
        try {
            T result;
            if(Thread.currentThread()==t)
                // if invoked from within ComThread, execute it at once
                result = call();
            else
                // otherwise schedule the execution and block
                result = t.execute(this);

            comThread.set(null);
            return result;
        }
        catch (RuntimeException e) {
            comThread.set(null);
            throw e;
        }
    }

    /**
     * Called from {@link ComThread} to run the task.
     */
    final synchronized void invoke() {
        result = null;
        exception = null;
        try {
            result = call();
        } catch( Throwable e ) {
            exception = e;
        } finally {
        	done = true;
        }

        // let the calling thread know that we are done.
        notify();
    }
    
    /**
     * Indicates whether this task is done executing
     * @return {@literal true} if execution of the task is finished
     */
    final boolean isDone() {
    	return done;
    }

    /**
     * Prepare for invocation.
     */
    final void reset() {
        done = false;
    }

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
