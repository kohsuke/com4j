package com4j;

import java.util.Iterator;

/**
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */

public class ROT implements Iterable<Com4jObject>
{
  private static final ROT instance = new ROT();

  /**
   * Disallow public instantiation. (Making ROT a singleton)
   */
  private ROT() {
  }

  /**
   * Retrieves the singleton instance of this class
   * @return The singleton instance of the running object table (ROT)
   */
  static ROT getInstance() {
    return instance;
  }

  /**
   * Returns an iterator over the elements of the running object table
   * @return The Iterator.
   */
  public Iterator<Com4jObject> iterator() {
    return new ROTIterator();
  }

  /**
   * The implementation of the running object table Iterator class
   * @author scm
   */
  private static class ROTIterator implements Iterator<Com4jObject>
  {
    /** The native win32 pointer to the running object table (IRunningObjectTable*) */
    long rotPointer = 0;

    /** The native win32 pointer to the EnumMoniker object (IEnumMoniker*) */
    long enumMonikerPointer = 0;

    /**
     * The next object of the running object table. To be able to provide the functionality of the hasNext method, we always need to prefetch one object ahead.
     */
    Com4jObject nextObject = null;

    /** Constructs and initializes the Iterator object */
    public ROTIterator() {
      new InitTask().execute(); // We have to do this in a ComThread
      fetchObject(); // We have to prefetch always one Object to provide the hasNext method
    }

    public void finalize() {
      cleanUp();
    }

    private void cleanUp() {
      // We have to release the native interface pointers!
      if (rotPointer != 0) {
        Native.release(rotPointer);
        rotPointer = 0;
      }
      if (enumMonikerPointer != 0) {
        Native.release(enumMonikerPointer);
        enumMonikerPointer = 0;
      }
    }

    private void fetchObject() {
      // We need to execute this in a ComThread, hence the redirection.
      nextObject = new GetNextRunningObjectTask().execute();
    }

    public boolean hasNext() {
      return nextObject != null;
    }

    public Com4jObject next() {
      if (rotPointer != 0 && enumMonikerPointer != 0 && nextObject != null) {
        Com4jObject current = nextObject;
        fetchObject();
        return current;
      } else {
        return null;
      }
    }

    /**
     * Removing elements form the ROT through an Iterator does not make sense (we cannot unregister third party objects).
     */
    public void remove() {
      throw new UnsupportedOperationException("You cannot remove an arbitary object form the table");
    }

    private class InitTask extends Task<Boolean> {
        public Boolean call() {
            rotPointer = Native.getRunningObjectTable();
            enumMonikerPointer = Native.getEnumMoniker(rotPointer);
            return Boolean.TRUE;
        }
    }

    private class GetNextRunningObjectTask extends Task<Com4jObject>
    {
      public Com4jObject call() {
        long pointer = Native.getNextRunningObject(rotPointer, enumMonikerPointer);
        if (pointer == 0) {
          // we reached the end of the ROT. So we can clean up the native COM pointers.
          cleanUp();
          return null;
        }
        return Wrapper.create(pointer);
      }
    }
  }

}
