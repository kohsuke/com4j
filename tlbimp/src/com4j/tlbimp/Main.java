package com4j.tlbimp;

import com4j.COM4J;
import com4j.Holder;
import com4j.ComException;
import com4j.tlbimp.def.ITypeLib;
import com4j.tlbimp.def.ITypeInfo;
import com4j.tlbimp.def.IWTypeLib;
import com4j.tlbimp.def.IWType;

import java.io.File;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {
    public static void main(String[] args) {
        File typeLibFileName = new File(args[0]);

        IWTypeLib tlb = COM4J.loadTypeLibrary(typeLibFileName).queryInterface(IWTypeLib.class);
//        System.out.println(tlb.count());
        System.out.println(tlb.getName());
        System.out.println(tlb.getHelpString());

        int len = tlb.count();
        for( int i=0; i<len; i++ ) {
            IWType t = tlb.getType(i);
            System.out.println(t.getName());
            System.out.println(t.getHelpString());
        }
    }


//        ITypeLib tlb = COM4J.loadTypeLibrary(typeLibFileName).queryInterface(ITypeLib.class);
//
//        Holder<String> name = new Holder<String>();
//        Holder<String> docstr = new Holder<String>();
//
//        tlb.getDocumentation(0,name,docstr,null,null);
//        System.out.println(name.value+" : "+docstr.value);
//
//        for( int i=0; i<tlb.getTypeInfoCount(); i++ ) {
//            ITypeInfo ti = tlb.getTypeInfo(i);
//            try {
//                ti.getDocumentation(-1/*MEMBERID_NIL*/,name,docstr,null,null);
//                System.out.println(name.value+" : "+docstr.value);
//            } catch( ComException e ) {
//                System.out.println("N/A "+e.getMessage());
//            }
//
//            ti.release();
//        }
//        tlb.release();
//    }
}
