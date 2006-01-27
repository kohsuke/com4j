import word._Application;

/**
 * @author Kohsuke Kawaguchi
 */
public class WordDemo {
    public static void main(String[] args) {
        _Application app = word.ClassFactory.createApplication();
        app.visible(true);

        app.documents().add( null, false, false, true);
        app.selection().typeText("Welcome to com4j");
        app.selection().typeParagraph();
        app.selection().typeText("Your Java/COM bridging solution");
    }
}
