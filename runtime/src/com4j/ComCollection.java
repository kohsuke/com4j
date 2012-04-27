package com4j;

import com4j.stdole.IEnumVARIANT;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Wraps IEnumVARIANT and implements {@link Iterator}.
 *
 * @author Kohsuke Kawaguchi
 * @author Michael Schnell (ScM, (C) 2008, 2009, Michael-Schnell@gmx.de)
 */
final class ComCollection<T> implements Iterator<T> {

    /**
     * The wrapped IEnumVARIANT
     */
    private final IEnumVARIANT e;

    /**
     * The prefetched next VARIANT element
     */
    private Variant next;


    /**
     * The expected item type.
     */
    private final Class<T> type;

    /**
     * Constructs a new ComCollection
     * @param type The class object of the type
     * @param e The newly wrapped IEnumVARIANT
     */
    ComCollection(Class<T> type, IEnumVARIANT e) {
        this.e = e;
        this.type = type;
        this.next = new Variant();
        fetch();
    }


    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return next!=null;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    @SuppressWarnings("unchecked")
    public T next() {
        if(next==null)
            throw new NoSuchElementException();

        Object r;
        try {
            // ideally we'd like to use ChangeVariantType to do the conversion
            // but for now let's just support interface types
            if(Com4jObject.class.isAssignableFrom(type)) {
                r = next.object((Class<? extends Com4jObject>)type);
            } else
                throw new UnsupportedOperationException("I don't know how to handle "+type);
        } finally {
            fetch();
        }
        return (T)r;
    }

    /**
     * Throws {@link UnsupportedOperationException}
     * @throws UnsupportedOperationException Removing an element from the iterator is not supported
     */
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Removing an element from a ComCollection iterator is not supported.");
    }

    /**
     * Fetches the next element.
     */
    private void fetch() {
    	next.clear();
        // We need to remember for what thread the IEnumVARIANT was marshaled. Because if we want to interpret this
        // VARIANT as an interface pointer later on, we need to do this in the same thread!
        next.thread = e.getComThread();
        int r = e.next(1,next);
        if(r==0) {
            next = null;
            e.dispose();
        }
    }
}
