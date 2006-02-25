package com4j.tlbimp;

import com4j.GUID;
import com4j.IID;
import com4j.tlbimp.def.IMethod;
import com4j.tlbimp.def.IDispInterfaceDecl;
import com4j.tlbimp.def.IType;
import com4j.tlbimp.Generator.LibBinder;

import java.util.List;
import java.util.ArrayList;

/**
 * Generates a disp-only interface.
 *
 * @author Kohsuke Kawaguchi
 */
final class DispInterfaceGenerator extends InvocableInterfaceGenerator<IDispInterfaceDecl> {

    public DispInterfaceGenerator(LibBinder lib, IDispInterfaceDecl t) {
        super(lib, t);
    }

    protected List<String> getBaseTypes() {
        return new ArrayList<String>(); // needs to return a fresh list
    }

    protected GUID getIID() {
        return GUID_IDISPATCH;
    }

    protected MethodBinderImpl createMethodBinder(IMethod m) throws BindingException {
        if(EventInterfaceGenerator.isBogusDispatchMethod(m))
            return null;
        else
            return new MethodBinderImpl(g,m);
    }

    private final class MethodBinderImpl extends InvocableInterfaceGenerator<IDispInterfaceDecl>.MethodBinderImpl {
        public MethodBinderImpl(Generator g, IMethod method) throws BindingException {
            super(g, method);
        }

        @Override
        protected IType getReturnTypeBinding() {
            return getDispInterfaceReturnType();
        }

        @Override
        protected void annotate(IndentingWriter o) {
            o.printf("@DISPID(%1d)",method.getDispId());
            o.println();
            switch(method.getKind()) {
            case PROPERTYGET:
                o.println("@PropGet");
                break;
            case PROPERTYPUT:
            case PROPERTYPUTREF:
                o.println("@PropPut");
                break;
            }
        }
    }

    private static final GUID GUID_IDISPATCH = new GUID(IID.IDispatch);

    private static final int DISPATCH_PROPERTYGET    = 0x2;
    private static final int DISPATCH_PROPERTYPUT    = 0x4;
    private static final int DISPATCH_PROPERTYPUTREF = 0x8;
}
