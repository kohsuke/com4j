package com4j.tlbimp.def;

import com4j.GUID;

/**
 * Commonality between {@link IInterfaceDecl} and {@link IDispInterfaceDecl}.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface IInterface extends ITypeDecl {
    GUID getGUID();
    int countMethods();
    IMethod getMethod(int idx);
}
