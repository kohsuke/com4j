package com4j;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Collection of living objects of a {@link ComThread}. This collection does not hold any strong references to the objects. This way the garbage collector can
 * clean up the objects. This class uses a special combination of a {@link HashMap} and {@link LinkedList}s to provide a (almost) constant runtime performance.
 * @author Michael Schnell (ScM)
 */
/*package*/class LiveObjectCollection
{

  /**
   * The objects of this collection are weakly referenced and stored in linked lists. One list for every pointer value. This guarantees to have constant times
   * for {@link #add(Com4jObject)} and {@link #remove(Com4jObject)}, assuming every COM object has a different pointer value. Since this is not necessarily the
   * case for all COM references ({@link Com4jObject#queryInterface(Class)} often returns the same pointer value) the runtime of {@link #remove(Com4jObject)} is
   * slightly slower, since we need to search the object in a (short) linked list.
   */
  private HashMap<Integer, LinkedList<WeakReference<Com4jObject>>> objects = new HashMap<Integer, LinkedList<WeakReference<Com4jObject>>>(20);

  /** The count of objects in this collection */
  private int count = 0;

  /**
   * Adds the given object to the collection
   * @param object the object to add
   */
  public synchronized void add(Com4jObject object) {
    LinkedList<WeakReference<Com4jObject>> list = objects.get(object.getPtr());
    if (list == null) {
      list = new LinkedList<WeakReference<Com4jObject>>();
      objects.put(object.getPtr(), list);
    }
    list.add(new WeakReference<Com4jObject>(object));
    count++;
  }

  /**
   * Removes the given object from the collection
   * @param object the object to remove
   */
  public synchronized void remove(Com4jObject object) {
    List<WeakReference<Com4jObject>> list = objects.get(object.getPtr());
    if (list == null) {
      throw new NoSuchElementException("The Com4jObject " + object + " is not in this collection!");
    }
    Iterator<WeakReference<Com4jObject>> it = list.iterator();
    while (it.hasNext()) {
      Com4jObject colObject = it.next().get();
      if (colObject == null || colObject == object) {
        // if colObject == null, then colObject was already finalized! This is the object we want to remove!
        // There should be only one finalized object for every call of remove, because every finalization of a Wrapper calls dispose() -> calls
        // dispose0() -> calls thread.removeLiveObject() -> calls this method.
        it.remove();
        count--;
        break; 
      }
    }
  }

  /**
   * Returns the count of objects in this collection.
   * @return the count of objects in this collection
   */
  public synchronized int getCount() {
    return count;
  }

  /**
   * Returns a snapshot of the collection as a list.
   * @return a snapshot of the collection as a list
   */
  public List<WeakReference<Com4jObject>> getSnapshot() {
    ArrayList<WeakReference<Com4jObject>> snapshot = new ArrayList<WeakReference<Com4jObject>>(count);
    int destIndex = 0;
    for (Integer i : objects.keySet()) {
      List<WeakReference<Com4jObject>> list = objects.get(i);
      for (WeakReference<Com4jObject> weakReference : list) {
        snapshot.add(weakReference);
        destIndex++;
      }
    }
    return snapshot;
  }

  /**
   * Returns whether this collection is empty.
   * @return whether this collection is empty
   */
  public boolean isEmpty() {
    return count == 0;
  }
}
