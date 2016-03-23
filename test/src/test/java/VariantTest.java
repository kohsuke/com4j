import com4j.Holder;
import com4j.Variant;
import com4j_idl.ClassFactory;
import com4j_idl.ITestObject;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.math.BigDecimal;
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
        testArray2(new Object[]{});
    }

    public void testEmpty2DArray() throws Exception {
        testArray2(new Object[][]{{}, {}, {}});
    }

    /**
     * Tests the currency type conversion.
     */
    public void testArray() throws Exception {
        testArray2(new Object[]{"a1", "a2", "a3"});
    }

    public void test2DArrays() throws Exception {
        testArray2(new Object[][] {
                {"a11","a12"},
                {"a21","a22"},
                {"a31","a32"}
        });
    }

    public void test3DArrays() throws Exception {
        testArray2(new Object[][][] {
                {{"a111", "a112"},
                        {"a121", "a122"}},
                {{"a211", "a212"},
                        {"a221", "a222"}},
                {{"a311", "a312"},
                        {"a321", "a322"}}
        });
    }

    public void testDoubleArrays() throws Exception {
        testArray2(new Object[][]{
                {1.1, 1.2},
                {2.1, 2.2},
                {3.1, 3.2}
        });
    }

    public void testPrimitiveArrays() throws Exception {
        testArray2(new double[][] {
                {1.1,1.2},
                {2.1,2.2},
                {3.1,3.2}
        });
    }

    public void test2DArrayWithsubArray() throws Exception {
        Object[][] a = {
                {"a11", "a12", "a13", "a14", "a15"},
                {"a21", "a22", "a23", "a24", "a25"}
        };

        a[1][4] = new Object[][] {
                {"s11", "s12"},
                {"s21", "s22"},
                {"s31", "s32"},
                {"s41", "s42"},
                {"s51", "s52"}
        };

        testArray2(a);
    }

    public void testNullArrays() throws Exception {
        testArray2(new Object[]{"a1", "a2", null, "a4", "a15"});
    }

    public void testVeryBigDimArray() throws Exception {
        Object[][] a1 = new Object[][]{
                {"a11", "a12"},
                {"a21", "a22"}
        };

        testArray2(a1);

        Object[][][][][][][][][][][][] a2 = new Object[][][][][][][][][][][][]{{{{{{{{{{{{"a1"}}}}}}}}}}}};
        testArray2(a2);
    }

    private void testArray2(Object[] orig) {
        //Object[] a = (Object[])deepCopy(orig);

        Object[] r = getAsReturnValue(orig);
        assertTrue(Arrays.deepEquals(orig, r));

        Object[] r2 = getAsReturnValue(r);
        assertTrue(Arrays.deepEquals(orig, r2));


        //a = (Object[])deepCopy(orig);
        r = getAsRefValue(orig);
        assertTrue(Arrays.deepEquals(orig, r));

        r2 = getAsRefValue(r);
        assertTrue(Arrays.deepEquals(orig, r2));
    }


    private Object[] getAsReturnValue(Object[] a) {
        ITestObject t = ClassFactory.createTestObject();

        Object[] c = (Object[])t.testVariant(Variant.getMissing(), a); // return copy of a, test return value conversion
        return c;
    }

    private Object[] getAsRefValue(Object[] a) {
        ITestObject t = ClassFactory.createTestObject();

        Variant bv = Variant.getMissing();

        t.testVariant(a, bv); // copy a -> b, test internal conversion
        return bv.convertTo(Object[].class);
    }


}
