package com4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Provides faster number &lt;-> enum conversion.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class EnumDictionary<T extends Enum<T>> {
    private final Class<T> clazz;

    private final Map<Integer,T> fromValue = new HashMap<Integer,T>();
    private final Map<T,Integer> toValue = new HashMap<T,Integer>();

    private EnumDictionary(Class<T> clazz) {
        this.clazz = clazz;
        assert clazz.isEnum();

        T[] consts = clazz.getEnumConstants();
        for( T v : consts ) {
            fromValue.put(v.ordinal(),v);
            toValue.put(v,v.ordinal());
        }
    }

    /**
     * Looks up a dictionary from an enum class.
     */
    public static <T extends Enum<T>>
    EnumDictionary<T> get( Class<T> clazz ) {
        EnumDictionary<T> dic = (EnumDictionary<T>)registry.get(clazz);
        if(dic==null) {
            dic = new EnumDictionary<T>(clazz);
            registry.put(clazz,dic);
        }
        return dic;
    }

    public int value( Enum t ) {
        return toValue.get(t);
    }

    public T constant( int v ) {
        T t = fromValue.get(v);
        if(t==null)
            throw new IllegalArgumentException(clazz.getName()+" has no constant of the value "+v);
        return t;
    }

    private static final Map<Class<? extends Enum>,EnumDictionary> registry =
        Collections.synchronizedMap(new WeakHashMap<Class<? extends Enum>,EnumDictionary>());
}
