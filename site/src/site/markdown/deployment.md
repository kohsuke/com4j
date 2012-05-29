# com4j deployment

`com4j.jar` includes `com4j-x86.dll` and `com4j-x64.jar` inside,
and at the runtime it does the right thing to load it.
Because of this, you normally just have to bundle `com4j.jar` with your application.
The only downside of this convenient approach is a sligh runtime performance hit.

Alternatively, you can either:

1. Place `com4j*.dll` in the same directory as `com4j.jar`.
1. Set the system property `java.library.path` to include the directory
  where `com4j*.dll` resides. This needs to be done when you launch a JVM,
  because the value of the property is cached by class loaders.

Java Web Start##

Applications that use com4j can be deployed by using Java Web Start.
See the jnlp sample in the distribution for details.
If you have [Java Web Start](http://java.sun.com/products/javawebstart/) software installed,
you can try [this demo](demo/com4j-jnlp-demo.jnlp).