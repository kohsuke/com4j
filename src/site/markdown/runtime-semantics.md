# com4j Runtime Semantics

This document explains things you should know when you use com4j.

## Who Implements Those Interfaces?

At runtime, com4j automatically generates the implementation code for
interfaces with com4j annotations (see [this](http://java.sun.com/j2se/1.5/docs/api/java/lang/reflect/Proxy.html) for more info).
We call them "proxies" from now on. Each proxy holds a reference to a COM interface.
As illustrated in the following picture, two proxies may have references to the same
interface of the same object, or two may have different interfaces of the same object.

Because of this, you may not use an expression like `proxy1==proxy2` to check
if two proxies refer to the same COM object. For that you have to write `proxy1.equals(proxy2)`.

![proxies](proxies.png "COM4J Proxies")

## COM Error and Exception

Consider the following COM method:

    [helpstring("get the child object.")]
    HRESULT GetItem( [int] int index, [out,retval] IFoo** ppItem );

A COM method not just returns a "conceptual" return value (`IFoo*`) but also returns a `HRESULT`.
`tlbimp` always hide HRESULT from Java, thus the above method is bound to:

    IFoo GetItem( int index );

When the COM method invocation returns a failure `HRESULT`, the com4j runtime throws unchecked `ComException`.

This makes it impossible for the caller to know the actual HRESULT success code
returned from the method, whereas sometimes a COM method actually uses different success code
(for example, use `S_OK` and `S_FALSE` as a boolean function). See [this document](annotations.html) to
learn how you can map the HRESULT as the return value from the Java method.

## Casting and QueryInterface

When your Java code has a reference to `IFoo` and you need to get `IBar` of the same COM object,
you have to use the `queryInterface` method as follows:

    IBar bar = fooObject.queryInterface(IBar.class);

In other words, you cannot use a normal cast operator like `(IBar)fooObject`

## Conclusion

Other than those considerations listed above, you can pretty much use all those
COM objects just like ordinary Java objects. Interested readers are encouraged to
go to other available documents for a deeper understanding of how com4j works.