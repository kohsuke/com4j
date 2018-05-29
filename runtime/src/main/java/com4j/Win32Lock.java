package com4j;

/**
 * Represents a Win32 Event lock.
 *
 * <p>
 * We can't use Java synchronization for {@link ComThreadMulti},
 * as it blocks incoming method dispatching requests.
 * We need to use this instead.
 *
 * @author Kohsuke Kawaguchi
 * @author Michael Schnell (ScM, (C) 2009, Michael-Schnell@gmx.de)
 */
final class Win32Lock {
    private final long eventHandle;

    /**
     * Constructs a new native win32 lock object.
     */
    Win32Lock() {
        eventHandle = createEvent();
    }

    /**
     * Signals the event.
     */
    void activate() {
        activate0(eventHandle);
    }

    /**
     * Blocks until the event is signaled.
     *
     * This runs Windows message loop.
     */
    void suspend() {
        suspend0(eventHandle);
    }

    /**
     * Blocks until the event is signaled or the timeout is reached.
     *
     * This runs Windows message loop.
     */
    void suspend(int timeoutMillis){
        suspend1(eventHandle, timeoutMillis);
    }

    /**
     * Closes the allocated resource.
     */
    void dispose() {
        closeHandle(eventHandle);
    }

    private static native void closeHandle(long eventHandle);
    private static native int createEvent();
    private static native void activate0(long handle);
    private static native void suspend0(long handle);
    private static native void suspend1(long handle, int timeoutMillis);
}
