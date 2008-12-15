package com4j.tlbimp.def;

import com4j.IID;
import com4j.VTID;

/**
 * @author Michael Schnell (scm, (C) 2008, Michael-Schnell@gmx.de)
 */
 
@IID("{5B18BA32-4A44-4a45-A80B-59631299A7EA}")
public interface IProperty extends ITypeDecl
{
  @VTID(3)
  String getName();

  @VTID(4)
  String getHelpString();

  @VTID(5)
  IType getType();

  @VTID(6)
  int getDispId();
}
