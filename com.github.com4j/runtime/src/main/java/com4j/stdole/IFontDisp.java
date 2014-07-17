package com4j.stdole;

import com4j.*;

/**
 *
 * <p>
 * This interface was generated using tlbimp on stdole2.tlb
 * </p>
 */
@IID("{00020400-0000-0000-C000-000000000046}")
public interface IFontDisp extends Com4jObject {
  // Methods:
  // Properties:
  /**
   * <p>
   * Getter method for the COM property "Name"
   * </p>
   * @return The COM property Name as a java.lang.String
   */
  @DISPID(0)
  @PropGet
  java.lang.String getName();

  /**
   * <p>
   * Setter method for the COM property "Name"
   * </p>
   * @param newValue The new value for the COM property Name as a java.lang.String
   */
  @DISPID(0)
  @PropPut
  void setName(java.lang.String newValue);

  /**
   * <p>
   * Getter method for the COM property "Size"
   * </p>
   * @return The COM property Size as a java.math.BigDecimal
   */
  @DISPID(2)
  @PropGet
  java.math.BigDecimal getSize();

  /**
   * <p>
   * Setter method for the COM property "Size"
   * </p>
   * @param newValue The new value for the COM property Size as a java.math.BigDecimal
   */
  @DISPID(2)
  @PropPut
  void setSize(java.math.BigDecimal newValue);

  /**
   * <p>
   * Getter method for the COM property "Bold"
   * </p>
   * @return The COM property Bold as a boolean
   */
  @DISPID(3)
  @PropGet
  boolean getBold();

  /**
   * <p>
   * Setter method for the COM property "Bold"
   * </p>
   * @param newValue The new value for the COM property Bold as a boolean
   */
  @DISPID(3)
  @PropPut
  void setBold(boolean newValue);

  /**
   * <p>
   * Getter method for the COM property "Italic"
   * </p>
   * @return The COM property Italic as a boolean
   */
  @DISPID(4)
  @PropGet
  boolean getItalic();

  /**
   * <p>
   * Setter method for the COM property "Italic"
   * </p>
   * @param newValue The new value for the COM property Italic as a boolean
   */
  @DISPID(4)
  @PropPut
  void setItalic(boolean newValue);

  /**
   * <p>
   * Getter method for the COM property "Underline"
   * </p>
   * @return The COM property Underline as a boolean
   */
  @DISPID(5)
  @PropGet
  boolean getUnderline();

  /**
   * <p>
   * Setter method for the COM property "Underline"
   * </p>
   * @param newValue The new value for the COM property Underline as a boolean
   */
  @DISPID(5)
  @PropPut
  void setUnderline(boolean newValue);

  /**
   * <p>
   * Getter method for the COM property "Strikethrough"
   * </p>
   * @return The COM property Strikethrough as a boolean
   */
  @DISPID(6)
  @PropGet
  boolean getStrikethrough();

  /**
   * <p>
   * Setter method for the COM property "Strikethrough"
   * </p>
   * @param newValue The new value for the COM property Strikethrough as a boolean
   */
  @DISPID(6)
  @PropPut
  void setStrikethrough(boolean newValue);

  /**
   * <p>
   * Getter method for the COM property "Weight"
   * </p>
   * @return The COM property Weight as a short
   */
  @DISPID(7)
  @PropGet
  short getWeight();

  /**
   * <p>
   * Setter method for the COM property "Weight"
   * </p>
   * @param newValue The new value for the COM property Weight as a short
   */
  @DISPID(7)
  @PropPut
  void setWeight(short newValue);

  /**
   * <p>
   * Getter method for the COM property "Charset"
   * </p>
   * @return The COM property Charset as a short
   */
  @DISPID(8)
  @PropGet
  short getCharset();

  /**
   * <p>
   * Setter method for the COM property "Charset"
   * </p>
   * @param newValue The new value for the COM property Charset as a short
   */
  @DISPID(8)
  @PropPut
  void setCharset(short newValue);

}
