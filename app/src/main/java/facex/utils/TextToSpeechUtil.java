package facex.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class TextToSpeechUtil {

    private static TextToSpeech tts;

    public static void say(Context context,String content){

        if(tts ==null) {
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        int result = tts.setLanguage(Locale.getDefault());
                        tts.setPitch(1.0f);
                        tts.setSpeechRate(1.0f);
                        say(tts, content);
                    }
                }
            });
        }else {
            say(tts,content);
        }
    }

    private static void say(TextToSpeech tts,String content){
        tts.speak(content,TextToSpeech.QUEUE_FLUSH, null, content);
    }
}
