package com4j.util;

import com4j.Com4jObject;
import com4j.ComObjectListener;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link ComObjectListener} implementation that collects all
 * newly created {@link Com4jObject}s
 *
 * <p>
 * The intended use of this class is to record objects created
 * in a certain block and then dipose them all (except a few marked explicitly)
 * at once at some later point.
 *
 * <p>
 * See the following code example for a typical usage:
 * <pre>
 * <pre class=code>
    void foo() {
        // we will start using COM objects.
        // so we'll register the listener and start keeping
        // track of COM objects we create.
        ComObjectCollector col = new ComObjectCollector();
        COM4J.addListener(col);

        try {
            // use COM objects as much as you want
            IFoo foo = doALotOfComStuff();

            // do this to avoid COM objects from disposed by the diposeAll method.
            col.remove(foo);
        } finally {
            // dipose all the COM objects created in this thread
            // since the listener is registered.
            // But "foo" won't be diposed because of the remove method.
            col.disposeAll();

            // make sure to remove the listener
            COM4J.removeListener(col);
        }
    }
 *</pre>
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ComObjectCollector implements ComObjectListener {
    protected final Map<Com4jObject,Object> objects = new WeakHashMap<Com4jObject,Object>();

    public void onNewObject(Com4jObject obj) {
        objects.put(obj,null);
    }

    /**
     * Removes the given object from the list of {@link Com4jObject}s that
     * this class keeps.
     *
     * <p>
     * If the application knows certain {@link Com4jObject} needs to live after
     * the {@link #diposeAll()} method, this method can be called to avoid the object
     * from being disposed.
     *
     * <p>
     * If the object passed in is not known to this {@link ComObjectCollector},
     * it is a no-op.
     */
    public void remove(Com4jObject obj) {
        objects.remove(obj);
    }

    /**
     * Calls the {@link Com4jObject#dispose()} method for all the {@link Com4jObject}s
     * known to this {@link ComObjectCollector}.
     *
     * <p>
     * Each time this method is called, it forgets all the diposed objects.
     */
    public void diposeAll() {
        for( Com4jObject o : objects.keySet() )
            o.dispose();
        objects.clear();
    }
}
