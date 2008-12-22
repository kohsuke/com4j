package com4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
 
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD })
public @interface UseDefaultValues
{
  /**
   * Returns an array of indices, that maps the parameter of the java interface to the parameter index of the COM interface <br>
   * Assume we have an COM interface with an function like
   *
   * <pre>
   * [helpstring(&quot;Test function&quot;)]
   * HRESULT TestFunction([in] VARIANT notOptional, [in, optional, defaultvalue(&quot;forty-two&quot;)] VARIANT opt1,
   *                      [in, defaultvalue(42)] VARIANT opt2, [in, lcid] long localeID, [out, retval] VARIANT *result);
   * </pre>
   *
   * Then we can generate three Java interface methods:
   *
   * <pre>
   * Variant testFunction(Variant notOptional, Variant opt1, Variant opt2, int localeID);
   *
   * Variant testFunction(Variant notOptional, Variant opt1, int localeID);
   *
   * Variant testFunction(Variant notOptional, int localeID);
   * </pre>
   *
   * The first function would not need a {@link UseDefaultValues} annotation at all. The second function needs an annotation with a parameter mapping [0, 1, 3]
   * and the third function needs the mapping [0, 3] <br>
   * The mapping wouldn't be necessary, if we consider that there is at most one <code>[lcid]</code> parameter and that this parameter always needs to be at the
   * end of the optional parameters. But consider the case, where we have an list of optional parameters with default values and Com4j is not able to handle an
   * optional parameter in the middle of list. Using parameter index mapping makes it possible to generate Java interface methods anyway.
   * <br>
   * @return A list of indices, mapping the java parameter to the index of the COM parameter
   */
  int[] paramIndexMapping();

  /**
   *
   * @return A list of the indices, naming the position of the optional parameters in the COM function
   */
  int[] optParamIndex();

  /**
   *
   * @return A list of Java Classes used to represent the optional parameters on the Java side.
   */
  Class<?>[] javaType();

  /**
   *
   * @return A list of NativeTypes used to generate the default parameters
   */
  NativeType[] nativeType();

  /**
   * A list of {@link Variant.Type}s used to generate the default parameters. The entry in this list is only needed in the case that the corresponding
   * NativeType is of type {@link NativeType#VARIANT}
   * @return A list of Variant.Types used to generate the default parameters.
   */
  Variant.Type[] variantType();

  /**
   *
   * @return A List of Strings representing the default parameter.
   */
  String[] literal();
}
