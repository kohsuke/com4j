package com4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks a given parameter as an locale identifier. This is a hint for the runtime, that this parameter should always be passed
 * as the last parameter in the list. In COM there may only be ONE lcid parameter, which has to follow all the other parameters.
 *
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.PARAMETER })
public @interface LCID
{

}
