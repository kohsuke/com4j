Building com4j
==============
Building com4j is divided into two parts. Native and Java.

If you are only interested in hacking Java side of com4j, we made it so that you don't have to have the whole native build environment. For this purpose, we commit *.dll and *.pdb into Subversion, which are the output of the native builds.

To build the native side of com4j, you need:

 - checkout git submodules that are linked
 - Visual Studio 2008
   - From options menu, add JDK's JNI include/lib folders to your environment.
     (Do not add those to the project since these values aren't portable.)
