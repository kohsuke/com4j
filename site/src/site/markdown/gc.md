# com4j Garbage Collection and Reference Counting

A life cycle of a COM object is governed by reference counting, while that of a Java object is governed by the garbage collection. This document explains how com4j handles this difference.

### When does a COM object get released?

By default, a proxy object releases a reference to a COM object shortly after the JVM figures out that the proxy itself can be garbage collected. This hides the detail of the life-cycle management from user applications, but the downside is that you can't generally predict when a COM object will be deallocated.

### Releasing COM objects earlier

User applications can explicitly call `Com4jObject.dispose` method to release a reference to the COM object earlier. Once this method is called, the proxy object will become "diposed", and all successive calls to any of its COM methods will fail with `IllegalStateException`.

### Using ComObjectListener

Another way for applications to manage COM object life-cycle is to use `ComObjectListener`. A listener can be register to the current thread, and if registered, it will receive a callback each time a new com4j proxy is created.

This is useful when your application has a code block in which the COM access is confined. The idea is to keep track of all the COM objects and then dipose them all (except a few that outlive the scope) after the code block is done. See the javadoc of `ComObjectCollector` for more about this.

This is useful for larger applications where calling `dispose` method on individual objects is too tedious.