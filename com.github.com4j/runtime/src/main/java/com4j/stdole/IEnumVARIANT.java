package com4j.stdole;

import com4j.*;

/**
 *
 * <p>
 * This interface was generated using tlbimp on stdole2.tlb
 * </p>
 *
 */

@IID("{00020404-0000-0000-C000-000000000046}")
public interface IEnumVARIANT extends Com4jObject {
  // Methods:
  /**
   * @param celt Mandatory int parameter.
   * @param rgvar Mandatory java.lang.Object parameter.
   * @return  Returns a value of type int
   */

  @VTID(3)
  int next(
    int celt,
    java.lang.Object rgvar);


  /**
   * @param celt Mandatory int parameter.
   */

  @VTID(4)
  void skip(
    int celt);


  /**
   */

  @VTID(5)
  void reset();


  /**
   * @return  Returns a value of type stdole.stdole.IEnumVARIANT
   */

  @VTID(6)
  com4j.stdole.IEnumVARIANT clone();


  // Properties:
}
