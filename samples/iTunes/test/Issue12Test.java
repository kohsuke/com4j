/*
 * MultiThreadingTest.java
 *
 * Created on 9. März 2005, 22:46
 */

import iTunes.def.ClassFactory;
import iTunes.def.IITPlaylist;
import iTunes.def.IITPlaylistCollection;
import iTunes.def.IITSource;
import junit.framework.TestCase;
import junit.textui.TestRunner;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;


/**
 * Demonstrating a problem using com4j for iTunes in multi-threade
 * applications.
 *
 * <p>
 * Run tldimp on iTunes.exe with package 'jtunes.itunes.com4j.types'. Add this
 * class and run it.
 * </p>
 *
 * <P>
 * The test assumes that there are at least two playlists in iTunes' library
 * source. It saves the library source in a static variable. Then it prints
 * the hashcodes of the first two playlists and whether they are equal by
 * <code>#equals(Object)</code>. This is done three times, first on the
 * main-thread, then on the event-dispatch-thread and finally on the
 * main-thread again.
 * </p>
 *
 * <p>
 * You will notice that on the EDT the hashcodes both are zero and the
 * playlists incorrectly seem to be equal. This unfortunately can break a lot
 * of code.
 * </p>
 *
 * <p>
 * The problem was introduced with version 2005/02/26 and is also in
 * 2005/03/07. In prior versions simply an exception was thrown.
 * </p>
 *
 * @author Frank-Michael Moser
 */
public class Issue12Test extends TestCase {

	/** iTunes' library source. */
	private IITSource iITSource;

    public void setUp() {
        iITSource = ClassFactory.createiTunesApp().librarySource();
    }

    public void tearDown() {
        iITSource.dispose();
        iITSource = null;
    }

    public void test1() throws InvocationTargetException, InterruptedException {
		dumpPlaylists();

        SwingUtilities.invokeAndWait(
            new Runnable() {
                public void run() {
                    dumpPlaylists();
                }
            });

		dumpPlaylists();
	}

	/**
	 * Dump the first two playlist items of the iTunes' library source. Show
	 * the hashcodes and check whether the items are equal.
	 */

	private void dumpPlaylists() {
		System.out.println("Thread: " + Thread.currentThread().getName());

		IITPlaylistCollection playlists = iITSource.playlists();

		for (int i = 1; i <= 2; i++) {

			IITPlaylist playlist = playlists.item(i);

			System.out.println(
				"  item(" + i + "): " + playlist.name() + " hashCode=" +
				playlist.hashCode());

            assertTrue( playlist.hashCode()!=0 );
		}

		System.out.println(
			"  item(1).equals(item(2)): " +
			playlists.item(1).equals(playlists.item(2)));

        assertFalse(playlists.item(1).equals(playlists.item(2)));
	}

	/**
	 * Run the MultiThreadingTest.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
        TestRunner.run(Issue12Test.class);
	}
}
