import junit.framework.TestCase;
import com4j_idl.ClassFactory;
import com4j_idl.ITestObject;
import com4j.Variant;
import com4j.Holder;

/**
 * @author Kohsuke Kawaguchi
 */
public class VariantTest extends TestCase {
    public void test1() {
        ITestObject t = ClassFactory.createTestObject();
        Variant v = new Variant();
        v.set(5);
        Object r = t.testVariant(1, v);

        assertEquals(1,v.intValue());
        assertEquals(5,((Integer)r).intValue());
    }

    public void test2() {
        ITestObject t = ClassFactory.createTestObject();
        Holder<Integer> v = new Holder<Integer>(5);
        Object r = t.testVariant(1, v);

        assertEquals(1,(int)v.value);
        assertEquals(5,((Integer)r).intValue());
    }

    public void test3() {
        ITestObject2 t = ClassFactory.createTestObject().queryInterface(ITestObject2.class);

        Holder<Integer> v = new Holder<Integer>(5);
        int r = t.testVariant2(1,v);

        assertEquals(1,(int)v.value);
        assertEquals(5,r);
    }
}
