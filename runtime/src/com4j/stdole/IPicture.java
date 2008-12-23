package com4j.stdole;

import com4j.*;

/**
 * Picture Object
 *
 * <p>
 * This interface was generated using tlbimp on stdole2.tlb
 * </p>
 */
@IID("{7BF80980-BF32-101A-8BBB-00AA00300CAB}")
public interface IPicture extends Com4jObject {
  // Methods:
  /**
   * <p>
   * Getter method for the COM property "Handle"
   * </p>
   * @return  Returns a value of type int
   */

  @VTID(3)
  int getHandle();


  /**
   * <p>
   * Getter method for the COM property "hPal"
   * </p>
   * @return  Returns a value of type int
   */

  @VTID(4)
  int getHPal();


  /**
   * <p>
   * Getter method for the COM property "Type"
   * </p>
   * @return  Returns a value of type short
   */

  @VTID(5)
  short getType();


  /**
   * <p>
   * Getter method for the COM property "Width"
   * </p>
   * @return  Returns a value of type int
   */

  @VTID(6)
  int getWidth();


  /**
   * <p>
   * Getter method for the COM property "Height"
   * </p>
   * @return  Returns a value of type int
   */

  @VTID(7)
  int getHeight();


  /**
   * @param hdc Mandatory int parameter.
   * @param x Mandatory int parameter.
   * @param y Mandatory int parameter.
   * @param cx Mandatory int parameter.
   * @param cy Mandatory int parameter.
   * @param xSrc Mandatory int parameter.
   * @param ySrc Mandatory int parameter.
   * @param cxSrc Mandatory int parameter.
   * @param cySrc Mandatory int parameter.
   * @param prcWBounds Mandatory java.nio.Buffer parameter.
   */

  @VTID(8)
  void render(
    int hdc,
    int x,
    int y,
    int cx,
    int cy,
    int xSrc,
    int ySrc,
    int cxSrc,
    int cySrc,
    java.nio.Buffer prcWBounds);


  /**
   * <p>
   * Setter method for the COM property "hPal"
   * </p>
   * @param phpal Mandatory int parameter.
   */

  @VTID(9)
  void setHPal(
    int phpal);


  /**
   * <p>
   * Getter method for the COM property "CurDC"
   * </p>
   * @return  Returns a value of type int
   */

  @VTID(10)
  int getCurDC();


  /**
   * @param hdcIn Mandatory int parameter.
   * @param phdcOut Mandatory Holder<Integer> parameter.
   * @param phbmpOut Mandatory Holder<Integer> parameter.
   */

  @VTID(11)
  void selectPicture(
    int hdcIn,
    Holder<Integer> phdcOut,
    Holder<Integer> phbmpOut);


  /**
   * <p>
   * Getter method for the COM property "KeepOriginalFormat"
   * </p>
   * @return  Returns a value of type boolean
   */

  @VTID(12)
  boolean getKeepOriginalFormat();


  /**
   * <p>
   * Setter method for the COM property "KeepOriginalFormat"
   * </p>
   * @param pfkeep Mandatory boolean parameter.
   */

  @VTID(13)
  void setKeepOriginalFormat(
    boolean pfkeep);


  /**
   */

  @VTID(14)
  void pictureChanged();


  /**
   * @param pstm Mandatory java.nio.Buffer parameter.
   * @param fSaveMemCopy Mandatory boolean parameter.
   * @return  Returns a value of type int
   */

  @VTID(15)
  int saveAsFile(
    java.nio.Buffer pstm,
    boolean fSaveMemCopy);


  /**
   * <p>
   * Getter method for the COM property "Attributes"
   * </p>
   * @return  Returns a value of type int
   */

  @VTID(16)
  int getAttributes();


  /**
   * @param hdc Mandatory int parameter.
   */

  @VTID(17)
  void setHdc(
    int hdc);


  // Properties:
}
