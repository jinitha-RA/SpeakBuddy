package lk.tmjnr.speakbuddy.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import lk.tmjnr.speakbuddy.data.local.entity.MessageEntity;

@Dao
public interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    LiveData<List<MessageEntity>> getMessagesForConversation(String convId);

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp DESC LIMIT :limit")
    List<MessageEntity> getRecentMessages(String convId, int limit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MessageEntity message);

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    void deleteAllForConversation(String convId);
}
