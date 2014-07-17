
import wsh.IFileSystem3;
import wsh.ClassFactory;

/**
 * Uses Windows Scripting Host to figure out the file version
 * of Windows PE format files.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {
    public static void main(String[] args) {
        IFileSystem3 fs = ClassFactory.createFileSystemObject();
        for( String arg : args ) {
            System.out.println("File version of "+arg+" is");
            System.out.println("  "+fs.getFileVersion(arg));
        }
    }
}
