import com4j_idl.ClassFactory;
import com4j_idl.ITestObject;
import junit.framework.TestCase;
import com4j.ComException;

/**
 * @author Kohsuke Kawaguchi
 */
public class LongTest extends TestCase {
    public void test1() {
        ITestObject t = ClassFactory.createTestObject();
        long magic = 0x100000002L;
        assertEquals(magic,t.testInt64(magic));
        try {
            t.testInt64(1);
            fail();
        } catch(ComException e) {
            // expected
            e.printStackTrace();
        }
    }
}
