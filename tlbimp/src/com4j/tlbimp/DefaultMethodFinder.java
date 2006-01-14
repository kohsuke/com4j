package com4j.tlbimp;

import com4j.GUID;
import com4j.tlbimp.def.IInterface;
import com4j.tlbimp.def.IMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * Finds default methods from {@link IInterface}.
 * @author Kohsuke Kawaguchi
 */
final class DefaultMethodFinder {
    private final Map<GUID,IMethod> cache = new HashMap<GUID,IMethod>();

    public IMethod getDefaultMethod(IInterface intf) {
        GUID guid = intf.getGUID();
        if(cache.containsKey(guid))
            return cache.get(guid);

        IMethod r = doGetDefaultMethod(intf);
        cache.put(guid,r);
        return r;
    }

    private IMethod doGetDefaultMethod(IInterface intf) {
        int len = intf.countMethods();
        for( int i=0; i<len; i++ ) {
            IMethod m = intf.getMethod(i);
            if(m.getDispId()==0)
                return m;
        }
        return null;
    }
}
