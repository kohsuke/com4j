
import com4j.COM4J;
import com4j.Holder;

/**
 * Test program.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Main {
    public static void main(String[] args) {
        IWshShell wsh = COM4J.createInstance(IWshShell.class,"WScript.Shell");
        String s = wsh.ExpandEnvironmentStrings("%WinDir%");
//        Holder<String> h = new Holder<String>();
//        wsh.ExpandEnvironmentStrings("%WinDir%",h);
//        String s = h.value;
        System.out.println(s);
        COM4J.dispose(wsh);
    }
}
