package lk.tmjnr.speakbuddy.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages",
        foreignKeys = @ForeignKey(
                entity = ConversationEntity.class,
                parentColumns = "id",
                childColumns = "conversationId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("conversationId"))
public class MessageEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String conversationId;
    public boolean isUser;
    public String text;
    public String correctedSentence;
    public String mistakesJson;       // JSON array of GrammarMistake
    public String followUpSuggestion;
    public long timestamp;
}
