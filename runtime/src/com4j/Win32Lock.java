package com4j;

/**
 * Represents a Win32 Event lock.
 *
 * <p>
 * We can't use Java synchronization for {@link ComThread},
 * as it blocks incoming method dispatching requests.
 * We need to use this instead.
 *
 * @author Kohsuke Kawaguchi
 */
final class Win32Lock {
    private final int eventHandle;

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
     * Closes the allocated resource.
     */
    void dispose() {
        closeHandle(eventHandle);
    }

    private static native void closeHandle(int eventHandle);
    private static native int createEvent();
    private static native void activate0(int handle);
    private static native void suspend0(int handle);
}
