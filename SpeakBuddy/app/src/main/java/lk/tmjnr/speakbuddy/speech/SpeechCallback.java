package lk.tmjnr.speakbuddy.speech;

/**
 * Callback interface for speech recognition events.
 */
public interface SpeechCallback {
    /** Called with partial (live) transcript as user speaks */
    void onPartialResult(String partial);

    /** Called with the final transcript when user stops speaking */
    void onFinalResult(String transcript);

    /** Called when a speech recognition error occurs */
    void onError(String message);

    /** Called when listening state changes */
    void onListeningStateChanged(boolean isListening);
}
