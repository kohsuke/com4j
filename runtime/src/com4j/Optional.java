package com4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates, that a given parameter of a COM interface function is optional. <br>
 * Right now this is only a hint for the programmer. In case of VARIANT parameters he can "omit" this parameter by passing {@link Variant#getMissing()}
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.PARAMETER })
public @interface Optional
{

}
