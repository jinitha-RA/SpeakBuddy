package lk.tmjnr.speakbuddy.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import lk.tmjnr.speakbuddy.data.local.entity.ConversationEntity;

@Dao
public interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    LiveData<List<ConversationEntity>> getAllConversations();

    @Query("SELECT * FROM conversations WHERE id = :id")
    ConversationEntity getConversationById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConversationEntity conversation);

    @Query("UPDATE conversations SET updatedAt = :time, totalMessages = totalMessages + 1, lastMessage = :lastMsg WHERE id = :id")
    void updateOnNewMessage(String id, long time, String lastMsg);

    @Query("UPDATE conversations SET totalMistakes = totalMistakes + :count WHERE id = :id")
    void incrementMistakeCount(String id, int count);

    @Query("UPDATE conversations SET isPinned = :pinned WHERE id = :id")
    void updatePinnedStatus(String id, boolean pinned);

    @Delete
    void delete(ConversationEntity conversation);

    @Query("DELETE FROM conversations WHERE id = :id")
    void deleteById(String id);
}
