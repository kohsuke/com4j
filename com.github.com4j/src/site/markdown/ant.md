# tlbimp Ant Task

`tlbimp.jar` comes with an implementation of Apache Ant task so that you can integrate tlbimp into your build process easily.

To declare the `tlbimp` task, include the following statement in your build script:

    <taskdef resource="/com4j/tlbimp/ant.properties">
      <classpath>
        <fileset dir="dir/to/com4j" includes="**/*.jar"/>
      </classpath>
    </taskdef>

## Synopsis

The `tlbimp` task supports the following attributes.

<table width=100% border=1>
<tr><td>
	Attribute
</td><td>
	Description
</td><td>
	Required
</td></tr>

<tr><td>
	destdir
</td><td>
	The output directory in which all the generated files will be placed.
	The package name will be automatically appended to this directory.
</td><td>
	Yes
</td></tr>

<tr><td>
	package
</td><td>
	The default Java package name in which the generated types will be placed
	(unless overriden by &lt;lib @package="..." />)
</td><td>
	No. Defaults to ""
</td></tr>

<tr><td>
	locale
</td><td>
	The locale to be used to convert method names and others.
	This is a string like "en_US", "ja_JP", etc.
</td><td>
	No. Defaults to the system default locale
</td></tr>
<!--
<tr><td>
</td><td>
</td><td>
</td></tr>
-->
</table>

## Nested Elements

### lib element

A `<lib>` element specifies one type library to compile. Sometimes a type library
depends on other type libraries.
Multiple lib elements can be used in this situation to control where each of the referenced type libraries are generated.

This element can take the following attributes:

<table border=1>

<tr><td>
	file
</td><td>
	A file that contains the type library to be processed.
</td><td rowspan=2>
	One of two.
</td></tr>

<tr><td>
	libid
</td><td>
	The LIBID of the type library to be processed.
	A string of the form "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
</td></tr>

<tr><td>
	libver
</td><td>
	The version of the type library given by @libid to be processed.
	When omitted, tlbimp searches the latest version and process it.
	A string of the form "<i>major</i>.<i>minor</i>"
</td><td>
	No. only valid with @libid
</td></tr>
<!--
<tr><td>
</td><td>
</td><td>
</td></tr>
-->
</table>

### LIBID and LIBVER

@libid and @libver needs more explanation. Often, the location of type libraries vary from a system to system.
For example, the type library for Microsoft Office is installed in the same directory where the user installed
Microsoft Office. Because people install things in different places, when multiple people are working on
the same project, this makes it difficult to consistently refer to the same type library.

@libid and @libver are useful in this case. Each type library has a unique GUID called "LIBID", and
the version of the type library. For example, Microsoft Excel 2000 type library has the LIBID of
"00020813-0000-0000-C000-000000000046" and the version of "1.3". Together they allow you to
reference a type library without knowing its actual location on the disk.

To find out the LIBID, the library version, and its dependencies, use [OleView](http://www.microsoft.com/downloads/details.aspx?FamilyID=5233b70d-d9b2-4cb5-aeb6-45664be858b6&displaylang=en).

## Examples

Compile Microsoft Excel type library without refering to the file path.

    <!-- compile Excel -->
    <tlbimp destdir="build/src" package="excel.types">
      <lib libid="00020813-0000-0000-C000-000000000046" libver="1.3" />
    </tlbimp>
    Compile Microsoft PowerPoint type library and its dependencies.

    <!-- compile PowerPoint -->
    <tlbimp destdir="build/src">
      <lib libid="91493440-5A91-11CF-8700-00AA0060263B" package="ppt" />
      <lib libid="0002E157-0000-0000-C000-000000000046" package="vba" />
      <lib libid="2DF8D04C-5BFA-101B-BDE5-00AA0044DE52" package="office" />
    </tlbimp>