package com4j.stdole;

import com4j.*;

/**
 * Font Object
 * <p>
 * This interface was generated using tlbimp on stdole2.tlb
 * </p>
 */
@IID("{BEF6E002-A874-101A-8BBA-00AA00300CAB}")
public interface IFont extends Com4jObject {
  // Methods:
  /**
   * <p>
   * Getter method for the COM property "Name"
   * </p>
   * @return  Returns a value of type java.lang.String
   */

  @VTID(3)
  java.lang.String getName();


  /**
   * <p>
   * Setter method for the COM property "Name"
   * </p>
   * @param pname Mandatory java.lang.String parameter.
   */

  @VTID(4)
  void setName(
    java.lang.String pname);


  /**
   * <p>
   * Getter method for the COM property "Size"
   * </p>
   * @return  Returns a value of type java.math.BigDecimal
   */

  @VTID(5)
  @ReturnValue(type=NativeType.Currency)
  java.math.BigDecimal getSize();


  /**
   * <p>
   * Setter method for the COM property "Size"
   * </p>
   * @param psize Mandatory java.math.BigDecimal parameter.
   */

  @VTID(6)
  void setSize(
    @MarshalAs(NativeType.Currency) java.math.BigDecimal psize);


  /**
   * <p>
   * Getter method for the COM property "Bold"
   * </p>
   * @return  Returns a value of type boolean
   */

  @VTID(7)
  boolean getBold();


  /**
   * <p>
   * Setter method for the COM property "Bold"
   * </p>
   * @param pbold Mandatory boolean parameter.
   */

  @VTID(8)
  void setBold(
    boolean pbold);


  /**
   * <p>
   * Getter method for the COM property "Italic"
   * </p>
   * @return  Returns a value of type boolean
   */

  @VTID(9)
  boolean getItalic();


  /**
   * <p>
   * Setter method for the COM property "Italic"
   * </p>
   * @param pitalic Mandatory boolean parameter.
   */

  @VTID(10)
  void setItalic(
    boolean pitalic);


  /**
   * <p>
   * Getter method for the COM property "Underline"
   * </p>
   * @return  Returns a value of type boolean
   */

  @VTID(11)
  boolean getUnderline();


  /**
   * <p>
   * Setter method for the COM property "Underline"
   * </p>
   * @param punderline Mandatory boolean parameter.
   */

  @VTID(12)
  void setUnderline(
    boolean punderline);


  /**
   * <p>
   * Getter method for the COM property "Strikethrough"
   * </p>
   * @return  Returns a value of type boolean
   */

  @VTID(13)
  boolean getStrikethrough();


  /**
   * <p>
   * Setter method for the COM property "Strikethrough"
   * </p>
   * @param pstrikethrough Mandatory boolean parameter.
   */

  @VTID(14)
  void setStrikethrough(
    boolean pstrikethrough);


  /**
   * <p>
   * Getter method for the COM property "Weight"
   * </p>
   * @return  Returns a value of type short
   */

  @VTID(15)
  short getWeight();


  /**
   * <p>
   * Setter method for the COM property "Weight"
   * </p>
   * @param pweight Mandatory short parameter.
   */

  @VTID(16)
  void setWeight(
    short pweight);


  /**
   * <p>
   * Getter method for the COM property "Charset"
   * </p>
   * @return  Returns a value of type short
   */

  @VTID(17)
  short getCharset();


  /**
   * <p>
   * Setter method for the COM property "Charset"
   * </p>
   * @param pcharset Mandatory short parameter.
   */

  @VTID(18)
  void setCharset(
    short pcharset);


  /**
   * <p>
   * Getter method for the COM property "hFont"
   * </p>
   * @return  Returns a value of type int
   */

  @VTID(19)
  int getHFont();


  /**
   * @return  Returns a value of type stdole.stdole.IFont
   */

  @VTID(20)
  com4j.stdole.IFont clone();


  /**
   * @param pfontOther Mandatory stdole.stdole.IFont parameter.
   */

  @VTID(21)
  void isEqual(
    com4j.stdole.IFont pfontOther);


  /**
   * @param cyLogical Mandatory int parameter.
   * @param cyHimetric Mandatory int parameter.
   */

  @VTID(22)
  void setRatio(
    int cyLogical,
    int cyHimetric);


  /**
   * @param hFont Mandatory int parameter.
   */

  @VTID(23)
  void addRefHfont(
    int hFont);


  /**
   * @param hFont Mandatory int parameter.
   */

  @VTID(24)
  void releaseHfont(
    int hFont);


  // Properties:
}
