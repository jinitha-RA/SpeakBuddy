package lk.tmjnr.speakbuddy.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Wraps Android SpeechRecognizer with continuous listening support.
 * Must be created and used on the main thread.
 */
public class SpeechRecognizerManager {

    private SpeechRecognizer recognizer;
    private final Context context;
    private final SpeechCallback callback;
    private final Handler mainHandler;
    private boolean isContinuous = false;
    private boolean isDestroyed = false;

    public SpeechRecognizerManager(Context context, SpeechCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
        initRecognizer();
    }

    private void initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(recognitionListener);
        }
    }

    /**
     * Start listening for speech. Call from main thread.
     */
    public void startListening() {
        if (recognizer == null || isDestroyed) return;

        isContinuous = true;
        callback.onListeningStateChanged(true);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L);

        try {
            recognizer.startListening(intent);
        } catch (Exception e) {
            callback.onError("Failed to start speech recognition");
        }
    }

    /**
     * Stop listening for speech.
     */
    public void stopListening() {
        isContinuous = false;
        if (recognizer != null) {
            try {
                recognizer.stopListening();
                recognizer.cancel();
            } catch (Exception ignored) {
            }
        }
        callback.onListeningStateChanged(false);
    }

    /**
     * Check if currently in continuous listening mode.
     */
    public boolean isContinuous() {
        return isContinuous;
    }

    /**
     * Release all resources. Must be called in Activity onDestroy().
     */
    public void destroy() {
        isDestroyed = true;
        isContinuous = false;
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception ignored) {
            }
            recognizer = null;
        }
    }

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            if (isDestroyed) return;

            switch (error) {
                case SpeechRecognizer.ERROR_NO_MATCH:
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    // No speech detected — silently restart if in continuous mode
                    if (isContinuous) {
                        mainHandler.postDelayed(() -> {
                            if (isContinuous && !isDestroyed) startListening();
                        }, 100);
                    }
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    callback.onError("Network error. Speech recognition requires internet.");
                    isContinuous = false;
                    callback.onListeningStateChanged(false);
                    break;
                case SpeechRecognizer.ERROR_AUDIO:
                    callback.onError("Audio recording error. Check microphone permissions.");
                    isContinuous = false;
                    callback.onListeningStateChanged(false);
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    callback.onError("Microphone permission is required.");
                    isContinuous = false;
                    callback.onListeningStateChanged(false);
                    break;
                default:
                    // For other errors, try restarting if continuous
                    if (isContinuous) {
                        mainHandler.postDelayed(() -> {
                            if (isContinuous && !isDestroyed) startListening();
                        }, 500);
                    }
                    break;
            }
        }

        @Override
        public void onResults(Bundle results) {
            if (isDestroyed) return;

            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                callback.onFinalResult(matches.get(0));
            }

            // In continuous mode, don't auto-restart after final result
            // The ChatViewModel will handle when to restart listening
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            if (isDestroyed) return;

            ArrayList<String> partials = partialResults.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (partials != null && !partials.isEmpty()) {
                callback.onPartialResult(partials.get(0));
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };
}
