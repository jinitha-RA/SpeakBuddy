package lk.tmjnr.speakbuddy.domain.model;

/**
 * Conversation topics for practice sessions.
 */
public enum Topic {
    DAILY("daily", "Daily Conversation"),
    INTERVIEW("interview", "Job Interview"),
    PRESENTATION("presentation", "Presentation Practice"),
    IELTS("ielts", "IELTS Speaking");

    private final String key;
    private final String displayName;

    Topic(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Topic fromKey(String key) {
        for (Topic t : values()) {
            if (t.key.equals(key)) return t;
        }
        return DAILY;
    }
}
