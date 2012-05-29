package com4j.util;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com4j.Com4jObject;
import com4j.ComObjectListener;

/**
 * {@link ComObjectListener} implementation that collects all
 * newly created {@link Com4jObject}s
 *
 * <p>
 * The intended use of this class is to record objects created
 * in a certain block and then dispose them all (except a few marked explicitly)
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

            // do this to avoid COM objects from disposed by the disposeAll method.
            col.remove(foo);
        } finally {
            // dispose all the COM objects created in this thread
            // since the listener is registered.
            // But "foo" won't be disposed because of the remove method.
            col.disposeAll();

            // make sure to remove the listener
            COM4J.removeListener(col);
        }
    }
 *</pre>
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Poindexter (staticsnow@gmail.com)
 */
public class ComObjectCollector implements ComObjectListener {
	
    /**
     * The collected {@link Com4jObject}s
     */
    protected final List<WeakReference<Com4jObject>> objects = new LinkedList<WeakReference<Com4jObject>>();
    
    /* (non-Javadoc)
     * @see com4j.ComObjectListener#onNewObject(com4j.Com4jObject)
     */
    public void onNewObject(Com4jObject obj) {
        objects.add(new WeakReference<Com4jObject>(obj));
    }

    /**
     * Removes the given object from the list of {@link Com4jObject}s that
     * this class keeps.
     *
     * <p>
     * If the application knows certain {@link Com4jObject} needs to live after
     * the {@link #disposeAll()} method, this method can be called to avoid the object
     * from being disposed.
     *
     * <p>
     * If the object passed in is not known to this {@link ComObjectCollector},
     * it is a no-op.
     * @param obj The object to remove
     */
    public void remove(Com4jObject obj) {
    	ListIterator<WeakReference<Com4jObject>> itr = objects.listIterator();
    	while(itr.hasNext()) {
    		Com4jObject o = itr.next().get();
    		if(o == obj) { //Intentional identity compare...each Wrapper instance owns a single ref.
    			itr.remove();
    			break;
    		}
    	}
    }

    /**
     * Calls the {@link Com4jObject#dispose()} method for all the {@link Com4jObject}s
     * known to this {@link ComObjectCollector}.
     *
     * <p>
     * Each time this method is called, it forgets all the disposed objects.
     */
    public void disposeAll() {
        for( WeakReference<Com4jObject> ref : objects) {
        	Com4jObject o = ref.get();
        	if(o != null) {
        		o.dispose();
        	}
        }
        objects.clear();
    }
}
