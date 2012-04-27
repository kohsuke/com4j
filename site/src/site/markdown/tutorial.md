# com4j tutorial

## Generate Type Definitions

Usually, the first step of using com4j is to generate Java type definitions from a COM type library. COM type libraries
are often found in .ocx, .dll, .exe, and/or .tlb files. I still don't know how to locate type libraries
for a given COM library other than guessing the file by using [OleView](http://www.microsoft.com/downloads/details.aspx?FamilyID=5233b70d-d9b2-4cb5-aeb6-45664be858b6&displaylang=en).

In this tutorial, we use `%WINDIR%\system32\wshom.ocx`, which contains
a type library for the [Windows Scripting Host](http://msdn.microsoft.com/library/default.asp?url=/library/en-us/script56/html/wsoriWindowsScriptHost.asp).
This type library should be available in all the modern flavors of Windows.

To generate Java definitions from a type library, do as follows:

    > java -jar tlbimp.jar -o wsh -p test.wsh %WINDIR%\system32\wshom.ocx

This should generate Java definitions in the `test.wsh` Java package and place all the files under the wsh directory.

## Learn What's Generated

First, take a look at the generated `ClassFactory` class. This class contains a series of `create***` methods
that are used to create new instances of COM objects.

    public abstract class ClassFactory {
        public static IFileSystem3 createFileSystemObject() {
            return COM4J.createInstance( IFileSystem3.class, "{0D43FE01-F093-11CF-8940-00A0C9054228}" );
        }
        ...
    }

Calling these methods causes an instanciation of a COM object, and returns a reference to its wrapper.

`tlbimp` also generates one Java interface for each interface definition found in a type library.
Typically they look like the following:

    @IID("{2A0B9D10-4B87-11D3-A97A-00104B365C9F}")
    public interface IFileSystem3 extends IFileSystem {
        @VTID(32)
        ITextStream getStandardStream(
            StandardStreamTypes standardStreamType,
            boolean unicode);

        @VTID(33)
        java.lang.String getFileVersion(
            java.lang.String fileName);
    }

When you are just trying to use definitions generated from `tlbimp`, you can ignore all those annotations.
Those are used to configure the com4j runtime to do the bridging correctly.
These interfaces are implemented by the com4j COM object wrapper, and calling a method on
this interface causes the runtime to call the corresponding COM method.

Additionally, tlbimp generates enumerations when a type library defines them.

    public enum StandardStreamTypes {
        StdIn, // 0
        StdOut, // 1
        StdErr, // 2
    }

## Using Generated Code

Using the generated code is simple. The following code illustrates how you can use the
`IFileSystem3.getFileVersion` method to obtain the version string of a file.

    public class Main {
      public static void main(String[] args) {
        IFileSystem3 fs = ClassFactory.createFileSystemObject();
        for( String file : args )
          System.out.println(fs.getFileVersion(file));
      }
    }
