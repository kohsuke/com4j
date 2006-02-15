package iTunes;

import iTunes.def.ClassFactory;
import iTunes.def.IITTrack;
import iTunes.def.IiTunes;

/**
 * Uses iTunes COM API to drive iTunes.
 *
 * <p>
 * Thanks to Frank-Michael Moser for pointing me to iTunes.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) {
		IiTunes iTunes = ClassFactory.createiTunesApp();
        // go to music store now!
//        iTunes.gotoMusicStoreHomePage();

        IITTrack track = iTunes.currentTrack();
        if(track==null) {
            System.out.println("Nothing is playing");
        } else {
            System.out.println("Now playing: "+ track.name());
        }
    }
}
