import com4j.Holder;
import iTunes.def.ClassFactory;
import iTunes.def.ITPlayButtonState;
import iTunes.def.IiTunes;
import junit.framework.TestCase;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Issue17Test extends TestCase {

    public void test1() throws InvocationTargetException, InterruptedException {
        IiTunes iiTunes = ClassFactory.createiTunesApp();

        Holder<Boolean> h1 = new Holder<Boolean>();
        Holder<ITPlayButtonState> h2 = new Holder<ITPlayButtonState>();
        Holder<Boolean> h3 = new Holder<Boolean>();
        iiTunes.getPlayerButtonsState(h1,h2,h3);

        System.out.println(h1.value);
        System.out.println(h2.value);
        System.out.println(h3.value);
    }
}
