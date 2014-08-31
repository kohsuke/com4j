import com4j.Holder;
import com4j.Variant;
import com4j_idl.ClassFactory;
import com4j_idl.ITestObject;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

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
        ITestObject t = ClassFactory.createTestObject();

        BigDecimal const199 = new BigDecimal("1.99");

        BigDecimal bd = t.testCurrency(null,const199);
        assertTrue(bd.compareTo(new BigDecimal("5.3"))==0); // $5.30

        bd = new BigDecimal("1.99");
        assertTrue(bd.compareTo(t.testCurrency(new Holder<BigDecimal>(bd),const199))==0);
    }

    public void testEmptyArray() throws Exception {
        ITestObject t = ClassFactory.createTestObject();
        Object[] a = {};
        Object[] b = (Object[]) t.testVariant(Variant.getMissing(), a);
        assertTrue(Arrays.deepEquals(a, b));
    }

    public void testEmpty2DArray() throws Exception {
        ITestObject t = ClassFactory.createTestObject();
        Object[][] a = {{},{},{}};
        Object[] b = (Object[]) t.testVariant(Variant.getMissing(), a);
        assertTrue(Arrays.deepEquals(a, b));
    }

    /**
     * Tests the currency type conversion.
     */
    public void testArray() throws Exception {
        ITestObject t = ClassFactory.createTestObject();
        Object[] a = {"a1", "a2", "a3"};
        Object[] b = (Object[]) t.testVariant(Variant.getMissing(), a);
        assertTrue(Arrays.deepEquals(a, b));
    }

    public void test2DArrays() throws Exception {
        ITestObject t = ClassFactory.createTestObject();

        Object[][] a = {
                {"a11","a12"},
                {"a21","a22"},
                {"a31","a32"}
        };

        Object[] b = (Object[]) t.testVariant(Variant.getMissing(), a);

        assertTrue(Arrays.deepEquals(a, b));

    }

    public void test3DArrays() throws Exception {
        ITestObject t = ClassFactory.createTestObject();
        Object[][][] a = {
                {{"a111", "a112"},
                        {"a121", "a122"}},
                {{"a211", "a212"},
                        {"a221", "a222"}},
                {{"a311", "a312"},
                        {"a321", "a322"}}
        };

        Object b = t.testVariant(Variant.getMissing(), a);
        assertTrue(b instanceof Object[]);
        assertTrue(Arrays.deepEquals(a, (Object[])b));
    }

    public void testDoubleArrays() throws Exception {
        ITestObject t = ClassFactory.createTestObject();

        Object[][] a = {
                {1.1,1.2},
                {2.1,2.2},
                {3.1,3.2}
        };

        Object[] b = (Object[]) t.testVariant(Variant.getMissing(), a);
        assertTrue(Arrays.deepEquals(a, b));
    }

    public void testPrimitiveArrays() throws Exception {
        ITestObject t = ClassFactory.createTestObject();

        double[][] a = {
                {1.1,1.2},
                {2.1,2.2},
                {3.1,3.2}
        };

        Object[] b = (Object[]) t.testVariant(Variant.getMissing(), a);
        assertTrue(Arrays.deepEquals(a, b));
    }


}
