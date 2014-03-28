package com4j.tlbimp.def;

import com4j.GUID;
import com4j.IID;
import com4j.VTID;

/**
 * Commonality between {@link IInterfaceDecl} and {@link IDispInterfaceDecl}.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
@IID("{FEADAF50-8751-4a12-87D6-B0EAA190C299}")
public interface IInterface extends ITypeDecl {
  @VTID(7)
  GUID getGUID();

  @VTID(8)
  int countMethods();

  @VTID(9)
  IMethod getMethod(int idx);

  @VTID(10)
  int countProperties();

  @VTID(11)
  IProperty getProperty(int idx);

  /**
   * return true if this interface is a dual interface
   */
  @VTID(12)
  boolean isDual();
}
