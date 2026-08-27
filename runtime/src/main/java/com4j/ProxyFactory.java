package com4j;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates the concrete classes {@link Wrapper#create} hands out in place of
 * {@code java.lang.reflect.Proxy} instances.
 *
 * <p>
 * The JDK's built-in dynamic proxy generator emits a reflective {@code Method} lookup for
 * every method of the interface(s) being proxied, and packs all of those lookups into a single
 * {@code <clinit>}. COM interfaces with hundreds of members (e.g. Excel's {@code Application})
 * can make that one generated method exceed the JVM's 64KB per-method bytecode limit, which
 * fails with {@code MethodTooLargeException} - this became more likely to trip on JDK 15+, whose
 * ASM-based proxy generator produces a larger {@code <clinit>} per method than the one it
 * replaced.
 *
 * <p>
 * Here, each interface method gets its own small generated thunk instead of sharing one giant
 * initializer: the {@link java.lang.reflect.Method} table is resolved once, in plain Java code,
 * and simply handed to the generated class' constructor, so no method we generate scales with
 * the size of the interface being proxied. See {@link ProxyBase} for the fixed set of
 * {@link Com4jObject} methods, which are implemented directly there rather than generated.
 *
 * <p>
 * Non-public interfaces (e.g. a {@code private} nested interface a caller declares inline for
 * one-off use) fall back to {@link Proxy} instead: defining our generated class as a member of
 * such an interface's run-time package would require either borrowing its class loader's
 * identity via reflection - blocked by default on JDK 16+ without {@code --add-opens} - or
 * {@code java.lang.invoke.MethodHandles.Lookup}-based class injection, which needs JDK 9+ at
 * compile time. Non-public interfaces are necessarily small, hand-written ones, so they were
 * never at risk of {@code MethodTooLargeException} in the first place.
 */
final class ProxyFactory {

    private ProxyFactory() {
    }

    private static final Map<Class<?>, GeneratedProxy> CACHE = new ConcurrentHashMap<Class<?>, GeneratedProxy>();

    private static final Type PROXY_BASE_TYPE = Type.getType(ProxyBase.class);
    private static final Type WRAPPER_TYPE = Type.getType(Wrapper.class);
    private static final Type METHOD_ARRAY_TYPE = Type.getType(java.lang.reflect.Method[].class);

    private static final Method CTOR_METHOD =
            new Method("<init>", Type.VOID_TYPE, new Type[]{WRAPPER_TYPE, METHOD_ARRAY_TYPE});
    private static final Method INVOKE_METHOD =
            new Method("invoke", Type.getType(Object.class), new Type[]{Type.INT_TYPE, Type.getType(Object[].class)});

    private static final class GeneratedProxy {
        final Constructor<?> ctor;
        final java.lang.reflect.Method[] methods;

        GeneratedProxy(Constructor<?> ctor, java.lang.reflect.Method[] methods) {
            this.ctor = ctor;
            this.methods = methods;
        }
    }

    static <T extends Com4jObject> T create(Class<T> primaryInterface, Wrapper handler) {
        if (!Modifier.isPublic(primaryInterface.getModifiers())) {
            return primaryInterface.cast(Proxy.newProxyInstance(
                    primaryInterface.getClassLoader(), new Class<?>[]{primaryInterface}, handler));
        }

        GeneratedProxy p = CACHE.get(primaryInterface);
        if (p == null) {
            p = generate(primaryInterface);
            GeneratedProxy race = CACHE.putIfAbsent(primaryInterface, p);
            if (race != null)
                p = race;
        }
        try {
            return primaryInterface.cast(p.ctor.newInstance(handler, p.methods));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create proxy for " + primaryInterface, e);
        }
    }

    private static GeneratedProxy generate(Class<?> iface) {
        java.lang.reflect.Method[] methods = comMethods(iface);

        ClassLoader loader = iface.getClassLoader();
        String proxyName = "com4j.proxygen." + iface.getName().replace('.', '_') + "$$Proxy";
        String proxyInternalName = proxyName.replace('.', '/');

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, loader);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                proxyInternalName, null, PROXY_BASE_TYPE.getInternalName(), new String[]{Type.getInternalName(iface)});

        GeneratorAdapter ctor = new GeneratorAdapter(Opcodes.ACC_PUBLIC, CTOR_METHOD, null, null, cw);
        ctor.loadThis();
        ctor.loadArgs();
        ctor.invokeConstructor(PROXY_BASE_TYPE, CTOR_METHOD);
        ctor.returnValue();
        ctor.endMethod();

        for (int i = 0; i < methods.length; i++) {
            generateThunk(cw, methods[i], i);
        }

        cw.visitEnd();
        byte[] bytecode = cw.toByteArray();

        Class<?> generated = new ProxyClassLoader(loader).define(proxyName, bytecode);
        try {
            Constructor<?> ctorHandle = generated.getConstructor(Wrapper.class, java.lang.reflect.Method[].class);
            return new GeneratedProxy(ctorHandle, methods);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e); // the constructor we just generated must exist
        }
    }

    private static void generateThunk(ClassWriter cw, java.lang.reflect.Method m, int index) {
        Method asmMethod = Method.getMethod(m);
        GeneratorAdapter mg = new GeneratorAdapter(Opcodes.ACC_PUBLIC, asmMethod, null, exceptionTypes(m), cw);
        mg.loadThis();
        mg.push(index);
        mg.loadArgArray();
        mg.invokeVirtual(PROXY_BASE_TYPE, INVOKE_METHOD);
        Type returnType = asmMethod.getReturnType();
        if (returnType.getSort() == Type.VOID) {
            mg.pop();
        } else {
            mg.unbox(returnType);
        }
        mg.returnValue();
        mg.endMethod();
    }

    private static Type[] exceptionTypes(java.lang.reflect.Method m) {
        Class<?>[] exceptions = m.getExceptionTypes();
        if (exceptions.length == 0)
            return null;
        Type[] types = new Type[exceptions.length];
        for (int i = 0; i < exceptions.length; i++) {
            types[i] = Type.getType(exceptions[i]);
        }
        return types;
    }

    /**
     * The interface methods that need a generated thunk: everything except what
     * {@link ProxyBase} already implements directly ({@link Com4jObject}'s own methods),
     * de-duplicated and with synthetic/bridge methods dropped.
     */
    private static java.lang.reflect.Method[] comMethods(Class<?> iface) {
        Map<String, java.lang.reflect.Method> byKey = new LinkedHashMap<String, java.lang.reflect.Method>();
        for (java.lang.reflect.Method m : iface.getMethods()) {
            if (m.isSynthetic() || m.isBridge())
                continue;
            Class<?> declaringClass = m.getDeclaringClass();
            if (declaringClass == Com4jObject.class || declaringClass == Object.class)
                continue;
            byKey.put(m.getName() + Type.getMethodDescriptor(m), m);
        }
        return byKey.values().toArray(new java.lang.reflect.Method[0]);
    }

    /**
     * Loads exactly one generated class, as a child of the interface's own class loader so the
     * generated bytecode can see the interface (and whatever other types its methods reference).
     */
    private static final class ProxyClassLoader extends ClassLoader {
        ProxyClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytecode) {
            return defineClass(name, bytecode, 0, bytecode.length);
        }
    }

    /**
     * The generated methods never branch or merge control flow, so COMPUTE_FRAMES never
     * actually needs to resolve a common superclass here - this override only exists so that,
     * if it ever did, it resolves types through the proxied interface's loader instead of the
     * system class loader ClassWriter defaults to.
     */
    private static final class SafeClassWriter extends ClassWriter {
        private final ClassLoader loader;

        SafeClassWriter(int flags, ClassLoader loader) {
            super(flags);
            this.loader = loader;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                Class<?> c1 = Class.forName(type1.replace('/', '.'), false, loader);
                Class<?> c2 = Class.forName(type2.replace('/', '.'), false, loader);
                if (c1.isAssignableFrom(c2))
                    return type1;
                if (c2.isAssignableFrom(c1))
                    return type2;
                if (c1.isInterface() || c2.isInterface())
                    return "java/lang/Object";
                do {
                    c1 = c1.getSuperclass();
                } while (!c1.isAssignableFrom(c2));
                return c1.getName().replace('.', '/');
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }
}