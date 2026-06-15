package lk.tmjnr.speakbuddy.domain.model;

import java.util.List;

/**
 * Domain model representing a chat message (either user or AI).
 */
public class Message {
    private String id;
    private String conversationId;
    private boolean isUser;
    private String text;
    private String correctedSentence;
    private List<GrammarMistake> mistakes;
    private String followUpSuggestion;
    private long timestamp;
    private boolean isTypingIndicator;

    public Message() {
    }

    /** Create a user message */
    public static Message userMessage(String conversationId, String text) {
        Message m = new Message();
        m.id = java.util.UUID.randomUUID().toString();
        m.conversationId = conversationId;
        m.isUser = true;
        m.text = text;
        m.timestamp = System.currentTimeMillis();
        return m;
    }

    /** Create an AI message */
    public static Message aiMessage(String conversationId, String reply,
                                     String correctedSentence,
                                     List<GrammarMistake> mistakes,
                                     String followUpSuggestion) {
        Message m = new Message();
        m.id = java.util.UUID.randomUUID().toString();
        m.conversationId = conversationId;
        m.isUser = false;
        m.text = reply;
        m.correctedSentence = correctedSentence;
        m.mistakes = mistakes;
        m.followUpSuggestion = followUpSuggestion;
        m.timestamp = System.currentTimeMillis();
        return m;
    }

    /** Create a typing indicator (not a real message) */
    public static Message typingIndicator() {
        Message m = new Message();
        m.isTypingIndicator = true;
        return m;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public boolean isUser() { return isUser; }
    public void setUser(boolean user) { isUser = user; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCorrectedSentence() { return correctedSentence; }
    public void setCorrectedSentence(String correctedSentence) { this.correctedSentence = correctedSentence; }

    public List<GrammarMistake> getMistakes() { return mistakes; }
    public void setMistakes(List<GrammarMistake> mistakes) { this.mistakes = mistakes; }

    public String getFollowUpSuggestion() { return followUpSuggestion; }
    public void setFollowUpSuggestion(String followUpSuggestion) { this.followUpSuggestion = followUpSuggestion; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isTypingIndicator() { return isTypingIndicator; }
    public void setTypingIndicator(boolean typingIndicator) { isTypingIndicator = typingIndicator; }
}
