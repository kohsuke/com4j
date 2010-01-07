package com4j.stdole;

import com4j.*;

/**
 * <p>
 * This enum was initially generated using tlbimp on stdole2.tlb
 * </p>
 */
public enum LoadPictureConstants implements ComEnum {
  /**
   * <p>
   * The value of this constant is 0
   * </p>
   */
  Default(0),
  /**
   * <p>
   * The value of this constant is 1
   * </p>
   */
  Monochrome(1),
  /**
   * <p>
   * The value of this constant is 2
   * </p>
   */
  VgaColor(2),
  /**
   * <p>
   * The value of this constant is 4
   * </p>
   */
  Color(4);

  private final int value;

  LoadPictureConstants(final int value) { this.value=value; }
  /**
   * Returns the value of the constant
   * @return The value of the constant
   */
  public int comEnumValue() { return value; }
}
