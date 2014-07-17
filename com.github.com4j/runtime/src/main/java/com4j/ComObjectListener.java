package com4j;

/**
 * Callback that receives a notification whenever
 * a new wrapper object is created.
 *
 * @see COM4J#addListener(ComObjectListener)
 * @see COM4J#removeListener(ComObjectListener)
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface ComObjectListener {
    /**
     * Called when a new COM object is created.
     * @param obj the newly created Com4jObject
     */
    void onNewObject( Com4jObject obj );
}
