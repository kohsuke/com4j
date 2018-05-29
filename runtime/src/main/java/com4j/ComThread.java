package com4j;

import java.lang.ref.ReferenceQueue;

/**
 * Interface for threads managed by com4j.
 *
 * <p>
 * COM objects constructed via Com4j use {@link ComThreadMulti}, and
 * those objects can be accessed from all Java threads.
 *
 * COM objects constructed outside of Com4j and wrapped that can only be
 * accessed from the thread that created the object and use
 * {@link ComThreadSingle}.
 *
 * <p>
 * This is because COM objects are inherently tied to the thread that created it,
 * and therefore all the invocations must be routed through the creator thread.
 * See http://msdn.microsoft.com/en-us/library/ms809971.aspx for more discussions.
 */
public interface ComThread {
    /**
     * Executes a {@link Task} in a {@link ComThread}
     * and returns its result.
     * @param task The task to be executed
     * @param <T> The type of the return value.
     * @return The result of the Task
     */
    public <T> T execute(Task<T> task);

    /**
     * Checks if the current thread this instance of ComThread;
     */
    boolean isCurrentThread();

    /**
     * Keeps track of wrappers that should be IUnknown::release-d.
     */
    public ReferenceQueue<Wrapper> getCollectableObjects();

    /**
     * Adds a {@link Com4jObject} to the live objects of this {@link ComThread}
     * <p>
     * This method increases the live object count of this thread and fires an
     * {@link ComObjectListener#onNewObject(Com4jObject)} event to all listeners.
     * </p>
     * @param r The new {@link Com4jObject}
     */
    public void addLiveObject( Com4jObject r );

    /**
     * Adds a {@link ComObjectListener} to this {@link ComThread}
     * @param listener the new listener
     * @throws IllegalArgumentException if the <code>listener</code> is <code>null</code> or if the listener is already registered.
     */
    public void addListener(ComObjectListener listener);

    /**
     * Removes the {@link ComObjectListener} from this {@link ComThread}
     * @param listener The listener to remove
     * @throws IllegalArgumentException if the listener was not registered to this {@link ComThread}
     */
    public void removeListener(ComObjectListener listener);
}
