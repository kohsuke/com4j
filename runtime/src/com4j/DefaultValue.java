package com4j;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * The default value of this parameter as noted in COM type library.
 *
 * <p>
 * Java doesn't support parameter default values, so this value is strictly
 * for documentation purpose, so that you as a programmer knows what value
 * to pass in when you'd like to omit the parameter.
 *
 * <p>
 * This parameter is not used by the com4j runtime.
 *
 * @author Kohsuke Kawaguchi
 */
@Retention(RUNTIME)
@Target(PARAMETER)
@Documented
public @interface DefaultValue {
  /**
   * Returns the default value of the annotated argument
   * @return the default value of the annotated argument
   */
    String value();
}
