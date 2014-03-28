import com4j.IID;
import com4j.Com4jObject;
import com4j.VTID;
import com4j.ReturnValue;
import com4j.NativeType;
import com4j.MarshalAs;
import com4j.Holder;

/**
 * Variation of ITestObject.
 *
 * @author Kohsuke Kawaguchi
 */
@IID("{55167E25-E6D1-4672-86C8-242AE001B7AB}")
public interface ITestObject2 extends Com4jObject {
    @VTID(7)
    @ReturnValue(type= NativeType.VARIANT)
    int testVariant2(
        @MarshalAs(NativeType.VARIANT) int v1,
        @MarshalAs(NativeType.VARIANT_ByRef) Holder<Integer> v2);

    @VTID(8)
    int outByteBuf( String s, Holder<Integer> size );
}
