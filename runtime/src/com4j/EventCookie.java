package com4j;

/**
 * Represents a subscription to COM events.
 *
 * <p>
 * When you are subscribing to COM events, com4j
 * makes sure that your object will not be garbage collected.
 * (Even if you release all the references to the event source
 * COM object, {@link EventCookie} still retains an interface pointer.)
 *
 * <p>
 * Therefore, you must call {@link #close()} to terminate
 * the subscription, or memory will leak.
 *
 * @author Kohsuke Kawaguchi
 */
public interface EventCookie {
    /**
     * Terminates the event subscription.
     *
     * <p>
     * This method can be safely invoked multiple times.
     */
    void close();
}
