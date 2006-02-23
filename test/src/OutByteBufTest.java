import com4j.COM4J;
import com4j.Holder;
import com4j_idl.ClassFactory;
import junit.framework.TestCase;

import java.nio.ByteBuffer;

/**
 * @author Kohsuke Kawaguchi
 */
public class OutByteBufTest extends TestCase {
    public void test1() throws Exception {
        ITestObject2 t = ClassFactory.createTestObject().queryInterface(ITestObject2.class);
        Holder<Integer> sz = new Holder<Integer>();
        int ptr = t.outByteBuf("Test",sz);
        ByteBuffer buf = COM4J.createBuffer(ptr, sz.value);

        byte[] tmp = new byte[13];
        buf.get(tmp);

        String msg = new String(tmp);
        assertEquals("Hello, World!",msg);
        System.out.println(msg);
    }
}
