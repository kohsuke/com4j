package com4j;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Base class for the per-interface classes {@link ProxyFactory} generates to stand in for
 * {@code java.lang.reflect.Proxy} instances of COM interfaces.
 *
 * <p>
 * All of the {@link Com4jObject} methods are implemented here, directly against the
 * {@link Wrapper} handler, so that generated subclasses only need bytecode for the COM
 * interface's own methods.
 *
 * <p>
 * These are deliberately not {@code final}: a COM interface can perfectly legally redeclare one
 * of these names for its own purposes (e.g. Excel's {@code Application} interface has a "Name"
 * property, which shows up here as its own {@code setName(String)}). When that happens, the
 * redeclared method's declaring class is the COM interface, not {@link Com4jObject}, so
 * {@link ProxyFactory} generates a real COM-invoking thunk for it and that override wins - which
 * is what should happen, since a user after this class' {@code setName} would call it on the
 * {@link Wrapper} directly rather than through the generated proxy.
 *
 * @see ProxyFactory
 */
public abstract class ProxyBase implements Com4jObject {

    private final Wrapper handler;

    /**
     * The interface methods a generated subclass' methods dispatch through, indexed by the
     * constant each method thunk was generated with. Resolved once via reflection by
     * {@link ProxyFactory} and handed in here, rather than re-resolved per instance.
     */
    private final Method[] methods;

    protected ProxyBase(Wrapper handler, Method[] methods) {
        this.handler = handler;
        this.methods = methods;
    }

    /**
     * Called by generated method thunks with the constant index (into {@link #methods}) that
     * {@link ProxyFactory} generated them for.
     */
    protected final Object invoke(int methodIndex, Object[] args) {
        try {
            return handler.invoke(this, methods[methodIndex], args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Throwable t) {
            throw new UndeclaredThrowableException(t);
        }
    }

    public boolean equals(Object o) { return handler.equals(o); }
    public int hashCode() { return handler.hashCode(); }
    public String toString() { return handler.toString(); }
    public int getPtr() { return handler.getPtr(); }
    public long getPointer() { return handler.getPointer(); }
    public long getIUnknownPointer() { return handler.getIUnknownPointer(); }
    public ComThread getComThread() { return handler.getComThread(); }
    public void dispose() { handler.dispose(); }
    public <T extends Com4jObject> boolean is(Class<T> comInterface) { return handler.is(comInterface); }
    public <T extends Com4jObject> T queryInterface(Class<T> comInterface) { return handler.queryInterface(comInterface); }
    public <T> EventCookie advise(Class<T> eventInterface, T receiver) { return handler.advise(eventInterface, receiver); }
    public void setName(String name) { handler.setName(name); }
}