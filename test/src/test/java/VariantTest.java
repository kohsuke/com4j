import com4j.Holder;
import com4j.Variant;
import com4j_idl.ClassFactory;
import com4j_idl.ITestObject;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    /**
     * Tests the conversion of ulonglong
     */
    public void testUI8() throws Exception {
        BigInteger bi = new BigInteger("2147483648"); // MAX_LONG +1

        ITestObject t = ClassFactory.createTestObject();
        BigInteger bi2 = (BigInteger) t.testUI8Conv(bi);

        assertEquals(bi,bi2);
    }

    public void testUI1() throws Exception {
        ITestObject t = ClassFactory.createTestObject();

        Short b = (Short)t.testUI1Conv(null);
        assertEquals((short)b,1);
    }

    /**
     * Tests the currency type conversion.
     */
    public void testCurrency() throws Exception {
        System.out.println("Waiting");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        ITestObject t = ClassFactory.createTestObject();

        BigDecimal const199 = new BigDecimal("1.99");

        BigDecimal bd = t.testCurrency(null,const199);
        assertTrue(bd.compareTo(new BigDecimal("5.3"))==0); // $5.30

        bd = new BigDecimal("1.99");
        assertTrue(bd.compareTo(t.testCurrency(new Holder<BigDecimal>(bd),const199))==0);
    }
}
