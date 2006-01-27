import office.MsoTriState;
import ppt.PpSlideLayout;
import ppt._Application;
import ppt._Presentation;
import ppt._Slide;

/**
 * @author Kohsuke Kawaguchi
 */
public class PowerPointDemo {
    public static void main(String[] args) {
        _Application app = ppt.ClassFactory.createApplication();
        app.activate();
        _Presentation prezo = app.presentations().add(MsoTriState.msoTrue);
        _Slide slide = prezo.slides().add(1, PpSlideLayout.ppLayoutText);

        slide.name("title slide");
        slide.shapes(1).textFrame().textRange().text("Welcome to com4j");

        slide.shapes(2).textFrame().textRange().text("Your Java/COM bridging solution");

        //prezo.slideShowSettings().run();
    }
}
