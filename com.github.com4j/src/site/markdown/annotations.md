# com4j Annotation Guide

This document explains the details of how the runtime bridges a Java method invocation into a COM method invocation, and how one can use annotations to control this process.

In the most general form, a Java method can be annotated as follows:

    @IID(iid)
    public interface INTERFACE {
      @VTID(vtid)
      @ReturnValue(index=rindex,inout=rio,type=rt)
      T foo(
        @MarshalAs(t1) T1 param1,
        @MarshalAs(t2) T2 param2,
        ... );
    }

### IID

The `iid` parameter of the surrounding interface designates the IID of the COM interface. The method invocation is done against this interface of a COM object.

### VTID

The mandatory `vtid` parameter describes the index of the method in the given interface.
The com4j runtime never uses the method name information to decide which COM method to invoke.
You can deteminer the virtual-table index by counting methods defined on that interface.
For example, `IUnknown` has 3 methods, so `@VTID(3)` would designate the first method
on an interface derived from `IUnknown`. `IDispatch` defines 4 additional methods,
so the first method on an interface derived from `IDispatch` would have `@VITD(7)`.

Using the wrong VTID often causes the JVM to crash, because you end up calling a wrong method (or non-existent method) with a wrong set of parameters. So be careful when you manually tweak this.

### rindex

In COM, a return value is usually passed as a parameter by reference.
Therefore, when a Java method has a return value, com4j bridges it as a parameter.
The optional `rindex` specifies where this parameter is passed among the real parameters.
For example, the following Java method:

    @ReturnValue(index=0) Tr foo( T1 t1, T2 t2 )

would be bridged to the following COM method invocation:

    HRESULT Foo( [out,retval] Tr* r, T1 t1, T2 t2 );

Similarly, the following Java method:

    @ReturnValue(index=1) Tr foo( T1 t1, T2 t2 )

would be bridged to the following COM method invocation:

    HRESULT Foo( T1 t1, [out,retval] Tr* r, T2 t2 );

When `rindex` is omitted, it means that the return value is passed after the last parameter,
which is what most COM methods do.

### rio

Although rare, a COM method parameter can have `[in,out,retval]` semantics,
which means it takes a value from the caller, modifies it, and returns it as the return value of the method.

Specifying `true` for `rio` would achieve this semantics.
With this switch turned on, instead of inserting a return value among the parameters,
the com4j runtime overloads the designated parameter both as a parameter and a return value.
Thus the following Java method:

    @ReturnValue(index=1,inout=true) T2 foo( T1 t1, T2 t2 )

would be bridged to the following COM method invocation:

    HRESULT Foo( T1 t1, [int,out,retval] T2* t2 );


### rt

The optional `rt` parameter specifies the native return type for this method and the semantics of
how the return value is mapped to Java. When omitted, [a pre-defined table](/javadoc/com4j/NativeType.html#Default) is used
to decide which native type to use from the Java return type.

For possible values, their semantics, and allowed Java types, see [the javadoc of NativeType](/javadoc/com4j/NativeType.html).

### t1,t2,...

Parameters can be optionally annotated by the `MarshalAs` attribute to control how
a Java parameter is bound to a parameter of a native type.
When omitted, the same [pre-defined table](/javadoc/com4j/NativeType.html#Default) is used to decide which native type to use.

`NativeType` specified for the return type and `NativeType` specified for parameters sometimes have slightly different semantics.
See [the javadoc](/javadoc/com4j/NativeType.html) for details.

## COM Error and Exception

Consider the following COM method:

    [helpstring("get the child object.")]
    HRESULT GetItem( [int] int index, [out,retval] IFoo** ppItem );

A COM method not just returns a "conceptual" return value (`IFoo*`) but also returns a HRESULT. `tlbimp` always hides
`HRESULT` from Java, thus the above method is bound to:

    IFoo GetItem( int index );

When the COM method invocation returns a failure `HRESULT`, the com4j runtime throws unchecked `ComException`.

Sometimes a COM method actually uses this HRESULT to return a meaningful value. For example,

    [helpstring("count the items and returns it, or return a failure code.")]
    HRESULT CountItems();

If you want to access the HRESULT return value, use `NativeType.HRESULT` as follows, which returns the `HRESULT` value as a Java `int`:

    @ReturnValue(type=NativeType.HRESULT)
    int countItems();