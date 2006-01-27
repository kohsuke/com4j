import com4j.CLSCTX;
import com4j.COM4J;
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
        // to work around the custom marshaller issue with Office, specify CLSCTX.LOCAL_SERVER explicitly
        _Application app = ppt.ClassFactory.createApplication();
            //COM4J.createInstance(_Application.class, "{91493441-5A91-11CF-8700-00AA0060263B}", CLSCTX.LOCAL_SERVER );
        app.activate();
        _Presentation prezo = app.presentations().add(MsoTriState.msoTrue);
        _Slide slide = prezo.slides().add(1, PpSlideLayout.ppLayoutText);

        slide.name("title slide");
        slide.shapes(1).textFrame().textRange().text("Welcome to com4j");

        slide.shapes(2).textFrame().textRange().text("Your Java/COM bridging solution");

        //prezo.slideShowSettings().run();
    }
}
