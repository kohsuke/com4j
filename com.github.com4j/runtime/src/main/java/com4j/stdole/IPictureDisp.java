package com4j.stdole;

import com4j.*;
/**
 *
 * <p>
 * This interface was generated using tlbimp on stdole2.tlb
 * </p>
 *
 */
@IID("{00020400-0000-0000-C000-000000000046}")
public interface IPictureDisp extends Com4jObject {
  // Methods:
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

  @DISPID(6)
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


  // Properties:
  /**
   * <p>
   * Getter method for the COM property "Handle"
   * </p>
   * @return The COM property Handle as a int
   */
  @DISPID(0)
  @PropGet
  int getHandle();

  /**
   * <p>
   * Setter method for the COM property "Handle"
   * </p>
   * @param newValue The new value for the COM property Handle as a int
   */
  @DISPID(0)
  @PropPut
  void setHandle(int newValue);

  /**
   * <p>
   * Getter method for the COM property "hPal"
   * </p>
   * @return The COM property hPal as a int
   */
  @DISPID(2)
  @PropGet
  int getHPal();

  /**
   * <p>
   * Setter method for the COM property "hPal"
   * </p>
   * @param newValue The new value for the COM property hPal as a int
   */
  @DISPID(2)
  @PropPut
  void setHPal(int newValue);

  /**
   * <p>
   * Getter method for the COM property "Type"
   * </p>
   * @return The COM property Type as a short
   */
  @DISPID(3)
  @PropGet
  short getType();

  /**
   * <p>
   * Setter method for the COM property "Type"
   * </p>
   * @param newValue The new value for the COM property Type as a short
   */
  @DISPID(3)
  @PropPut
  void setType(short newValue);

  /**
   * <p>
   * Getter method for the COM property "Width"
   * </p>
   * @return The COM property Width as a int
   */
  @DISPID(4)
  @PropGet
  int getWidth();

  /**
   * <p>
   * Setter method for the COM property "Width"
   * </p>
   * @param newValue The new value for the COM property Width as a int
   */
  @DISPID(4)
  @PropPut
  void setWidth(int newValue);

  /**
   * <p>
   * Getter method for the COM property "Height"
   * </p>
   * @return The COM property Height as a int
   */
  @DISPID(5)
  @PropGet
  int getHeight();

  /**
   * <p>
   * Setter method for the COM property "Height"
   * </p>
   * @param newValue The new value for the COM property Height as a int
   */
  @DISPID(5)
  @PropPut
  void setHeight(int newValue);

}
