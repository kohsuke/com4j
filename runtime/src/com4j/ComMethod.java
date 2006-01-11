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
    abstract Object invoke( int ptr, Object[] args );
}
