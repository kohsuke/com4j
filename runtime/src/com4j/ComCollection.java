package com4j;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Wraps IEnumVARIANT and implements {@link Iterator}.
 *
 * @author Kohsuke Kawaguchi
 */
final class ComCollection<T> implements Iterator<T> {

    private final IEnumVARIANT e;

    private Variant next;


    /**
     * The expected item type.
     */
    private final Class<T> type;

    ComCollection(Class<T> type, IEnumVARIANT e) {
        this.e = e;
        this.type = type;
        fetch();
    }

    public boolean hasNext() {
        return next!=null;
    }

    public T next() {
        if(next==null)
            throw new NoSuchElementException();
        Variant v = next;
        next = null;
        fetch();

        Object r;
        try {
// ideally we'd like to use ChangeVariantType to do the conversion
            // but for now let's just support interface types
            if(Com4jObject.class.isAssignableFrom(type)) {
                r = v.object((Class<? extends Com4jObject>)type);
            } else
                throw new UnsupportedOperationException("I don't know how to handle "+type);
        } finally {
            v.clear();
        }
        return (T)r;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void fetch() {
        next = new Variant();
        int r = e.next(1,next);
        if(r==0) {
            next = null;
            e.dispose();
        }
    }
}
