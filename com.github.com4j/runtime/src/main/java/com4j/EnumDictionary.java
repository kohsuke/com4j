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
abstract class EnumDictionary<T extends Enum<T>> {
    protected final Class<T> clazz;


    private EnumDictionary(Class<T> clazz) {
        this.clazz = clazz;
        assert clazz.isEnum();
    }

    /**
     * Looks up a dictionary from an enum class.
     */
    public static <T extends Enum<T>>
    EnumDictionary<T> get( Class<T> clazz ) {
        EnumDictionary<T> dic = registry.get(clazz);
        if(dic==null) {
            boolean sparse = ComEnum.class.isAssignableFrom(clazz);
            if(sparse)
                dic = new Sparse<T>(clazz);
            else
                dic = new Continuous<T>(clazz);
            registry.put(clazz,dic);
        }
        return dic;
    }

    /**
     * Convenience method to be invoked by JNI.
     */
    static <T extends Enum<T>>
    T get( Class<T> clazz, int v ) {
        return get(clazz).constant(v);
    }

    /**
     * Gets the integer value for the given enum constant.
     */
    abstract int value( Enum<T> t );
    /**
     * Gets the enum constant object from its integer value.
     */
    abstract T constant( int v );


    /**
     * For enum constants that doesn't use any {@link ComEnum}.
     */
    static class Continuous<T extends Enum<T>> extends EnumDictionary<T> {
        private T[] consts;

        private Continuous(Class<T> clazz) {
            super(clazz);
            consts = clazz.getEnumConstants();
        }

        public int value(Enum<T> t ) {
            return t.ordinal();
        }

        public T constant( int v ) {
            return consts[v];
        }
    }

    /**
     * For enum constants with {@link ComEnum}.
     */
    static class Sparse<T extends Enum<T>> extends EnumDictionary<T> {
        private final Map<Integer,T> fromValue = new HashMap<Integer,T>();

        private Sparse(Class<T> clazz) {
            super(clazz);

            T[] consts = clazz.getEnumConstants();
            for( T v : consts ) {
                fromValue.put(((ComEnum)v).comEnumValue(),v);
            }
        }

        public int value(Enum<T> t ) {
            return ((ComEnum)t).comEnumValue();
        }

        public T constant( int v ) {
            T t = fromValue.get(v);
            if(t==null)
                throw new IllegalArgumentException(clazz.getName()+" has no constant of the value "+v);
            return t;
        }
    }


    private static final  Map<Class<? extends Enum>,EnumDictionary> registry =
        Collections.synchronizedMap(new WeakHashMap<Class<? extends Enum>,EnumDictionary>());
}
