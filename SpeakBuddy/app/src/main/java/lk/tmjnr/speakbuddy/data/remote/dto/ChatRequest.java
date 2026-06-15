package lk.tmjnr.speakbuddy.data.remote.dto;

import java.util.List;

/**
 * Request body sent to the AI backend.
 */
public class ChatRequest {
    private String transcript;
    private String sessionId;
    private String topic;
    private List<HistoryItem> history;

    public ChatRequest(String transcript, String sessionId, String topic, List<HistoryItem> history) {
        this.transcript = transcript;
        this.sessionId = sessionId;
        this.topic = topic;
        this.history = history;
    }

    /**
     * A single message in conversation history for context.
     */
    public static class HistoryItem {
        private boolean isUser;
        private String text;

        public HistoryItem(boolean isUser, String text) {
            this.isUser = isUser;
            this.text = text;
        }

        public boolean isUser() { return isUser; }
        public String getText() { return text; }
    }

    public String getTranscript() { return transcript; }
    public String getSessionId() { return sessionId; }
    public String getTopic() { return topic; }
    public List<HistoryItem> getHistory() { return history; }
}
