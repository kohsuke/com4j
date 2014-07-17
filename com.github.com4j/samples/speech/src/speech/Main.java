package speech;

import speech.types.ClassFactory;
import speech.types.ISpeechVoice;
import speech.types.SpeechVoiceSpeakFlags;

/**
 * An example that uses the Microsoft Speech API.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) throws Exception {
        ISpeechVoice v = ClassFactory.createSpVoice();
        System.out.println("Make sure your speaker is not off...");
        v.speak("We the People of the United States, in Order to " +
                "form a more perfect Union, establish Justice, " +
                "insure domestic Tranquility, provide for the " +
                "common defence, promote the general Welfare, " +
                "and secure the Blessings of Liberty to ourselves " +
                "and our Posterity, do ordain and establish this " +
                "Constitution for the United States of America.",SpeechVoiceSpeakFlags.SVSFDefault);
    }
}
