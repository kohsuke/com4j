import com4j.EventProxy;
import word.Window;
import word._Application;
import word._Document;

/**
 * Sample that automates Microsoft Word.
 *
 * @author Kohsuke Kawaguchi
 */
public class WordDemo {
    public static void main(String[] args) {
        _Application app = word.ClassFactory.createApplication();

        // subscribe to the event from Word
        EventProxy<?> eventSubscription = app.advise(WordAppEvents.class, new WordAppEvents() {
            public void documentChange() {
                System.out.println("document changed");
            }

            public void documentOpen(_Document doc) {
                System.out.println("document opened: "+doc.name());
            }

            public void newDocument(_Document doc) {
                System.out.println("new document : "+doc.name());
            }

            public void windowActivate(_Document doc, Window w) {
                System.out.println("window activated : "+doc.name()+" : "+w.caption());
            }
        });

        app.visible(true);

        app.documents().add( null, false, false, true);
        app.selection().typeText("Welcome to com4j");
        app.selection().typeParagraph();
        app.selection().typeText("Your Java/COM bridging solution");

        eventSubscription.close();
    }
}
