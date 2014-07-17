import junit.framework.TestCase;
import com4j.COM4J;
import com4j.Com4jObject;
import com4j.IID;
import com4j.DISPID;

/**
 * Tests IDispatch.invoke
 * @author Kohsuke Kawaguchi
 */
public class DispatchInvokeTest extends TestCase {
    @IID(IID.IDispatch)
    private static interface IFileSystem3 extends Com4jObject {
        @DISPID(0x00004e2a)
        String getFileVersion(String fileName);
    }

    public void test() throws Exception {
        // use Windows scripting host
        IFileSystem3 fs = COM4J.createInstance(IFileSystem3.class, "{0D43FE01-F093-11CF-8940-00A0C9054228}");
        String ver = fs.getFileVersion("c:\\windows\\system32\\user32.dll");
        System.out.println(ver);
        assertTrue(ver.matches("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$"));
    }
}
