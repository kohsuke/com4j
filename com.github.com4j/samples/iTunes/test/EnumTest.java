import com4j.Com4jObject;
import iTunes.def.ClassFactory;
import iTunes.def.IITTrack;
import iTunes.def.IITTrackCollection;
import iTunes.def.IiTunes;
import junit.framework.TestCase;
import junit.textui.TestRunner;

import java.lang.reflect.InvocationTargetException;

/**
 * Hendrik Schreiber reported that enumeration doesn't work.
 *
 * @author Kohsuke Kawaguchi
 */
public class EnumTest extends TestCase {

    public void test1() throws InvocationTargetException, InterruptedException {
        IiTunes iTunes = ClassFactory.createiTunesApp();
        iTunes.play();
        IITTrackCollection tracks = iTunes.currentPlaylist().tracks();
        for (Com4jObject t : tracks) {
            IITTrack track = t.queryInterface(IITTrack.class);
            System.out.println(track.name());
        }
    }

    public static void main(String[] args) {
        TestRunner.run(EnumTest.class);
    }
}
