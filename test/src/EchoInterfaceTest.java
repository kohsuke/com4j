import com4j.Com4jObject;
import com4j_idl.ClassFactory;
import com4j_idl.ITestObject;
import junit.framework.TestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class EchoInterfaceTest extends TestCase {
    public void test1() throws Exception {
        ITestObject t = create();
        Com4jObject r = t.echoInterface(t);
        assertEquals(t,r);
    }

    public void test2() throws Exception {
        ITestObject t = create();
        assertNull(t.echoInterface(null));
    }

    private ITestObject create() {
        return ClassFactory.createTestObject();
    }
}
