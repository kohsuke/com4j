import com4j.IID;
import com4j.DISPID;
import word._Document;
import word.Window;

/**
 * Manually written event sink interface.
 *
 * TODO: have tlbimp generate this for you
 *
 * @author Kohsuke Kawaguchi
 */
@IID("{000209FE-0000-0000-C000-000000000046}")
public interface WordAppEvents {
    @DISPID(3)
    void documentChange();

    @DISPID(4)
    void documentOpen(_Document doc);

    @DISPID(9)
    void newDocument(_Document doc);

    @DISPID(10)
    void windowActivate(_Document doc, Window w);
}
