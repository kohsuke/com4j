package com4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies which method parameter in the COM method
 * is used as the return value
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReturnValue {
    /**
     * The index of the parameter with [retval] marker.
     *
     * <p>
     * If 0, the retval parameter is the 1st parameter in the parameter list.
     * The default value '-1' means the return value is the last parameter.
     *
     * <p>
     * If the method has the return type <tt>void</tt>, the COM method
     * is assumed to have no return value (regardless of this annotation.)
     */
    int index() default -1;

    /**
     * True if the [retval] parameter is "in/out" (therefore it also shows up in the
     * Java parameter list.) Otherwise, the return value is not a part of the
     * Java parameter list.
     */
    boolean inout() default false;

    /**
     * The native type to be unmarshalled.
     */
    NativeType type() default NativeType.Default;

    /**
     * Indicates that the return value is actually a result from invoking
     * default properties.
     *
     * <p>
     * For example, when the underlying type definitions are as follows:
     * <pre>
     * interface IFoo {
     *   &#64;VTID(10)
     *   IBar abc();
     * }
     * interface IBar {
     *   &#64;DefaultProperty
     *   IZot def();
     * }
     * interface IZot {
     *   &#64;
     *   int value(int index);
     * }
     * </pre>
     * <p>
     * The following method on the IFoo interface effectively works
     * as a short-cut of the following chain:
     * </p>
     * <pre>
     * IFoo {
     *   &#64;ReturnValue(defaultPropertyThrough={IFoo.class,IBar.class})
     *   &#64;VTID(10)
     *   int abc(int index);
     * }
     *
     * // equivalent
     * pFoo.value(5);
     * pFoo.abc().def().value(5);
     * </pre>
     */
    Class<? extends Com4jObject>[] defaultPropertyThrough() default {};
}
