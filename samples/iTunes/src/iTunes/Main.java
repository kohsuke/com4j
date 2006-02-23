package iTunes;

import com4j.EventCookie;
import iTunes.def.ClassFactory;
import iTunes.def.IITTrack;
import iTunes.def.IiTunes;
import iTunes.def.events._IiTunesEvents;

/**
 * Uses iTunes COM API to drive iTunes.
 *
 * <p>
 * Thanks to Frank-Michael Moser for pointing me to iTunes.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws Exception {
        IiTunes iTunes = ClassFactory.createiTunesApp();
        // go to music store now!
//        iTunes.gotoMusicStoreHomePage();

        EventCookie cookie = iTunes.advise(_IiTunesEvents.class, new _IiTunesEvents() {
            public void onDatabaseChangedEvent(Object deletedObjectIDs, Object changedObjectIDs) {
                System.out.println("Database changed:" + deletedObjectIDs + "," + changedObjectIDs);
            }

            public void onPlayerPlayEvent(Object iTrack) {
                System.out.println("Playing " + iTrack);
            }

            public void onPlayerStopEvent(Object iTrack) {
                System.out.println("Stopped " + iTrack);
            }
        });

        IITTrack track = iTunes.currentTrack();
        if(track==null) {
            System.out.println("Nothing is playing");
        } else {
            System.out.println("Now playing: "+ track.name());
        }

        System.out.println("Listening to events (will quit in 15 seconds)");
        System.out.println("Play/stop songs in iTunes and see what happens");
        Thread.sleep(15000);

        cookie.close();
    }
}
