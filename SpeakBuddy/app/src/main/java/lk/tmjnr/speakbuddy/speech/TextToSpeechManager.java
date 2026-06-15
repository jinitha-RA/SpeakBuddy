package lk.tmjnr.speakbuddy.speech;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * Wraps Android TextToSpeech with completion callbacks.
 */
public class TextToSpeechManager implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean isReady = false;
    private final TtsCallback callback;
    private final Handler mainHandler;

    public interface TtsCallback {
        void onSpeechStarted();
        void onSpeechCompleted();
        void onError(String message);
    }

    public TextToSpeechManager(Context context, TtsCallback callback) {
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                mainHandler.post(() -> callback.onError("English TTS not available on this device"));
                return;
            }

            tts.setPitch(1.0f);
            tts.setSpeechRate(0.95f);  // Slightly slower for learners
            isReady = true;

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    mainHandler.post(callback::onSpeechStarted);
                }

                @Override
                public void onDone(String utteranceId) {
                    mainHandler.post(callback::onSpeechCompleted);
                }

                @Override
                public void onError(String utteranceId) {
                    mainHandler.post(() -> callback.onError("TTS playback failed"));
                }
            });
        } else {
            mainHandler.post(() -> callback.onError("Text-to-speech initialization failed"));
        }
    }

    /**
     * Speak the given text. Stops any ongoing speech first.
     */
    public void speak(String text) {
        if (!isReady || tts == null || text == null || text.isEmpty()) return;

        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params,
                "utterance_" + System.currentTimeMillis());
    }

    /**
     * Stop any ongoing speech.
     */
    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    /**
     * Check if TTS is currently speaking.
     */
    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    /**
     * Release resources. Call in Activity onDestroy().
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
