package lk.tmjnr.speakbuddy.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String topic;
    public String title;
    public String lastMessage;
    public long createdAt;
    public long updatedAt;
    public int totalMessages;
    public int totalMistakes;
}
