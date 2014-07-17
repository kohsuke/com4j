Using com4j
==========

[Download com4j](https://github.com/kohsuke/com4j/downloads) or [access it from Maven repository](http://maven.jenkins-ci.org/content/repositories/releases/org/jvnet/com4j/com4j/)

Building com4j
==============
Building com4j is divided into two parts. Native and Java.

If you are only interested in hacking Java side of com4j, we made it so that you don't have to have the whole native build environment. For this purpose, we commit *.dll and *.pdb into Git, which are the output of the native builds.

To build the native side of com4j, you need:

- checkout git submodules that are linked
- Visual Studio 2008
    - From options menu, add JDK's JNI include/lib folders to your environment.
      (Do not add those to the project since these values aren't portable.)

Run your "Visual Studio command prompt" and execute ant from the `native` directory.


javah
-----
If you change the Java classes that define native methods, be sure to execute `native/run_javah.bat` to keep header files in sync
