
This project aims to develop:

- A Java library that allows Java applications to seemlessly interoperate with Microsoft Component Object Model.
- A Java tool that imports a COM type library and generates the Java definitions of that library.

The goal of the project is to provide a better integration of Java and COM.

## Feature Highlights

- Takes advantages of J2SE 1.5 features to improve usability.
- Binds directly to the vtable interface (not IDispatch) for improved performance and broeader support for more COM interfaces.
- Supports event callback
- Works on 32bit and 64bit JVM

## Downloadables

Download the distribution from [here](https://github.com/kohsuke/com4j/downloads). Source code is on [GitHub](https://github.com/kohsuke/com4j)

## Documentations

### Introductory

- [Quick Introduction](tutorial.html)
- [User's Guide --- the runtime semantics of com4j](runtime-semantics.html)
- [Distributing your applications that use com4j](deployment.html)
- [Using COM events with com4j](event.html)
- [Using tlbimp from Ant](ant.html)
- [Using tlbimp from Maven](maven-com4j-plugin/index.html)

### Advanced

- [com4j annotation guide](annotations.html)
- [Garbage collection and reference counting](gc.html)

### Status
The project is in active development. Help wanted! Contact the project owner if you are interested.