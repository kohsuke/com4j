
import com4j.Com4jObject;
import com4j.IID;
import com4j.VTID;
import com4j.MarshalAs;
import com4j.ReturnValue;
import com4j.Holder;
import static com4j.NativeType.*;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
@IID("{F935DC21-1CF0-11D0-ADB9-00C04FD58A0B}")
public interface IWshShell extends Com4jObject {
    // IUnknown 3
    // IDispath 4

    @VTID(12)
    @ReturnValue(index=1,type=BSTR)
    String ExpandEnvironmentStrings(
        @MarshalAs(BSTR) String param
//        @MarshalAs(BSTR_ByRef) Holder<String> out
    );
}
