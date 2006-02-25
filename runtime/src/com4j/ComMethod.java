package com4j;

/**
 * Internal abstraction that represents a COM method invocation.
 *
 * <p>
 * Instances hide the details of how to invoke a COM method.
 * (or a series of them.)
 *
 * @author Kohsuke Kawaguchi
 */
abstract class ComMethod {
    /**
     * Invokes a method and returns a value.
     *
     * @param ptr
     *      The interface pointer. {@link ComMethod} has apriori knowledge
     *      of what interface it points to.
     *
     * @param args
     *      The invocation arguments.
     */
    abstract Object invoke( int ptr, Object[] args );
}
