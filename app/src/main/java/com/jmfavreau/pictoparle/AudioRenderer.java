package com.jmfavreau.pictoparle;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class AudioRenderer {
    private TextToSpeech tts;
    private String lang;
    private int audio_verbosity;
    private boolean silence;

    public AudioRenderer(Context context, String l) {
        this.lang = l;
        this.audio_verbosity = 1;
        // set text-to-speech method with the good language
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    if (lang.equals("fr"))
                        tts.setLanguage(Locale.FRANCE);
                    else
                        tts.setLanguage(Locale.UK);
                }
            }
        });
        silence = false;

    }

    public void setAudioVerbosity(int av) {
        audio_verbosity = av;
    }

    public void setSilence(boolean silence) {
        this.silence = silence;
    }

    public void speak(String msg) {
        speak(msg, msg, msg);
    }
    public void speak(String verboseMsg, String msg) {
        speak(verboseMsg, msg, msg);
    }

    public void speak(String verboseMsg, String msg, String briefMsg) {
        if (!silence) {
            switch (audio_verbosity) {
                case 1:
                    speakInternal(verboseMsg);
                    break;
                case 2:
                    speakInternal(msg);
                    break;
                case 3:
                default:
                    speakInternal(briefMsg);
                    break;
            }
        }
    }

    private void speakInternal(String msg) {
        if (!msg.equals(""))
            tts.speak(msg,  TextToSpeech.QUEUE_FLUSH, null);
    }


}
