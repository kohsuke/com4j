package com4j;

/**
 * Used to execute a chunk of code from {@link ComThread}.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
abstract class Task {
    abstract void run();
    
    /**
     * Managed by {@link ComThread} to form a linked list from
     * {@link ComThread#taskList}.
     */
    Task next;
}
