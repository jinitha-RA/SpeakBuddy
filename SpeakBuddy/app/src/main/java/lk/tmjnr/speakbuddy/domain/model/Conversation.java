package lk.tmjnr.speakbuddy.domain.model;

/**
 * Domain model representing a conversation session.
 */
public class Conversation {
    private String id;
    private String topic;
    private String title;
    private String lastMessage;
    private long createdAt;
    private long updatedAt;
    private int totalMessages;
    private int totalMistakes;

    public Conversation() {
    }

    public static Conversation create(Topic topic) {
        Conversation c = new Conversation();
        c.id = java.util.UUID.randomUUID().toString();
        c.topic = topic.getKey();
        c.title = topic.getDisplayName();
        c.createdAt = System.currentTimeMillis();
        c.updatedAt = System.currentTimeMillis();
        c.totalMessages = 0;
        c.totalMistakes = 0;
        return c;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getTotalMessages() { return totalMessages; }
    public void setTotalMessages(int totalMessages) { this.totalMessages = totalMessages; }

    public int getTotalMistakes() { return totalMistakes; }
    public void setTotalMistakes(int totalMistakes) { this.totalMistakes = totalMistakes; }
}
