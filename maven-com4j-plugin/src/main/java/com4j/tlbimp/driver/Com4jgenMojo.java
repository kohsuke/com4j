package com4j.tlbimp.driver;

import com4j.tlbimp.BindingException;
import com4j.tlbimp.ErrorListener;
import com4j.tlbimp.FileCodeWriter;
import com4j.tlbimp.def.IWTypeLib;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Maven2 mojo for running the com4j process to produce .java files for the
 * Java-COM bridge. No required parameters are mentioned below, but you must
 * specify EITHER "libId" or "file" as documented below.
 * 
 * Effectively, this is what runs to generate .java code for something like
 * iTunes:
 * 
 * <br/> <code>
 * java -jar tlbimp.jar -o generated -p com.mycompany.com4j.itunes &quot;C:\Program Files\iTunes\iTunes.exe&quot;
 * </code>
 * <br/>
 * 
 * But we're using it from a Maven2 pom.xml file instead! This allows us to
 * automate code generation without worrying about how com4j is setup.
 * 
 * @author Jason Thrasher
 * 
 * @goal gen
 * @phase generate-sources
 * @requiresProject
 * @requiresDependencyResolution
 */
public class Com4jgenMojo extends AbstractMojo implements ErrorListener {
	/**
	 * The Maven Project. We'll add a new source directory to this project if
	 * everthing is successful.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * Specify the desired Java package for generated code. This can be used as
	 * the alias, without the leading underscore:
	 * 
	 * <br/> <code>
	 * &lt;package&gt;com.mycompany.com4j.someprogram&lt;/package&gt;
	 * </code>
	 * <br/> Or as: <br/><code>
	 * &lt;_package&gt;com.mycompany.com4j.someprogram&lt;/_package&gt;
	 * </code><br/>
	 * 
	 * The underscore is there because of the way Maven Mojos are created (the
	 * pom.xml tag is the variable name), and "package" is a reserved java word
	 * and can't be used as a variable.
	 * 
	 * @parameter expression="${package}" alias="package"
	 *            default-value="org.jvnet.com4j.generated"
	 */
	private String _package; // reserved keyword...

	/**
	 * Directory in which to create the Java COM wrapper files. This directory
	 * will be added to the Maven project's compile source directory list and
	 * will therfore be auto-compiled when the Maven compile phase is run.
	 * 
	 * @parameter expression="${outputDirectory}"
	 *            default-value="${project.build.directory}/generated-sources/com4j/java"
	 */
	private File outputDirectory;

	/**
	 * You must specify either <code>&lt;file&gt;</code> or
	 * <code>&lt;libId&gt;</code>. If both are configuration parameters are
	 * specified, <code>&lt;libId&gt;</code> will win, and
	 * <code>&lt;file&gt;</code> will be ignored.
	 * 
	 * <br/> File is the Win32 program that com4j is generating the COM
	 * interface for. This file must exist at the given path. The path can be
	 * absolute or relative. Generally this will specify your .exe, .dll, or
	 * whatever file has a Windows COM interface. <br/>
	 * 
	 * <code>
	 * &lt;file&gt;C:\Program Files\iTunes\iTunes.exe&lt;/file&gt;
	 * </code>
	 * 
	 * 
	 * @parameter expression="${file}"
	 */
	private File file;

	/**
	 * You must specify either <code>&lt;file&gt;</code> or
	 * <code>&lt;libId&gt;</code>. If both are configuration parameters are
	 * specified, <code>&lt;libId&gt;</code> will win, and
	 * <code>&lt;file&gt;</code> will be ignored.
	 * 
	 * <br/> LIBID is the Windows identifier of the type library to be
	 * processed. It should be a string of the form
	 * <code>xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</code>. <br/>
	 * 
	 * Often, the location of type libraries vary from a system to system. For
	 * example, the type library for Microsoft Office is installed in the same
	 * directory where the user installed Microsoft Office. Because people
	 * install things in different places, when multiple people are working on
	 * the same project, this makes it difficult to consistently refer to the
	 * same type library. libid and libver are useful in this case. Each type
	 * library has a unique GUID called "LIBID", and the version of the type
	 * library. <br/> For example, Microsoft Excel 2000 type library has the
	 * LIBID of:<br />
	 * 
	 * <code>
	 * &lt;libId&gt;00020813-0000-0000-C000-000000000046&lt;/libId&gt;
	 * </code>
	 * 
	 * <br />
	 * and the version of:<br />
	 * 
	 * <code>
	 * &lt;libVer&gt;1.3&lt;/libVer&gt;
	 * </code>
	 * 
	 * <br />
	 * Together they allow you to reference a type library without knowing its
	 * actual location of the file on the disk.
	 * 
	 * @parameter expression="${libId}"
	 */
	private String libId;

	/**
	 * Optional library version. Leave empty to designate the latest package
	 * based on the libId. This parameter has no effect if
	 * <code>&lt;libId&gt;</code> is not used.
	 * 
	 * @parameter expression="${libVer}"
	 */
	private String libVer;

	public void execute() throws MojoExecutionException {
		getLog().debug("Starting Com4jMojo for: " + file);

		checkEnv();

		validate();

		// all is good, now proceed with launch
		Driver driver = new Driver();

		Lib lib = new Lib();
		// libId wins over the specified file
		if (libId != null) {
			lib.setLibid(libId);
			if (libVer != null)
				lib.setLibver(libVer);
		} else {
			lib.setFile(file);
		}
		lib.setPackage(_package);

		try {
			lib.validate(); // could throw IAE
			getLog().info(
					"Generating COM for LIBID: " + lib.getLibid()
							+ " found here: " + lib.getFile());
			driver.addLib(lib);
			driver.run(new FileCodeWriter(outputDirectory), this);
		} catch (NullPointerException npe) {
			getLog()
					.warn(
							"Com4j had an NPE error while running."
									+ " This usually happens when it can't create an interface."
									+ " You many need to manually touch the files before trying to compile them.");
		} catch (Exception e) {
			// com4j may throw warnings if it can't handle something (like MS
			// Excel), we should continue with the mojo though
			getLog().warn(
					"Com4j had an error while running: \n" + e.getMessage());
			throw new MojoExecutionException(e.getMessage(),e);
		}

		getLog().debug("adding generated files to Maven compile source");
		// add outputDirectory to compile path for the project
		project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

		getLog().debug("Finished Com4jMojo for: " + file);
	}

	/**
	 * Check the runtime environment.
	 * 
	 * @throws MojoExecutionException
	 */
	private void checkEnv() throws MojoExecutionException {
		// check OS
		String osName = System.getProperty("os.name");
		if (!osName.startsWith("Windows")) {
			getLog().warn("Wrong OS: " + osName);
			throw new MojoExecutionException(
					"Com4j can only be run on a Windows operating system, and you're running: "
							+ osName);
		}

		// check output dir exists
		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			getLog().warn("outputDirectory couldn't be created");
			throw new MojoExecutionException("The output directory "
					+ outputDirectory
					+ " doesn't exist and couldn't be created.");
		}
	}

	/**
	 * Check the configuration from the pom.xml
	 * 
	 * @throws MojoExecutionException
	 */
	private void validate() throws MojoExecutionException {
		if ((file == null && libId == null) || (file != null && libId != null)) {
			getLog()
					.warn(
							"You specified <file> and <libId>.  The <libId> always wins.");
		}

		// check that COM target exists
		if (file != null && !file.exists()) {
			getLog().warn("Can't find file: " + file);
			throw new MojoExecutionException(
					"The native COM target file couldn't be found: " + file);
		}
	}

	public void started(IWTypeLib lib) {
		getLog().info("Generating definitions from " + lib.getName());
	}

	public void error(BindingException e) {
		getLog().error(e.getMessage());
	}

	public void warning(String message) {
		getLog().warn(message);
	}

}