package lk.tmjnr.speakbuddy.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import lk.tmjnr.speakbuddy.data.local.entity.ConversationEntity;
import lk.tmjnr.speakbuddy.data.repository.ChatRepository;
import lk.tmjnr.speakbuddy.domain.model.Conversation;
import lk.tmjnr.speakbuddy.domain.model.Topic;

public class HomeViewModel extends AndroidViewModel {

    private final ChatRepository repository;
    private final LiveData<List<ConversationEntity>> conversations;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = ChatRepository.getInstance(application);
        conversations = repository.getAllConversations();
    }

    public LiveData<List<ConversationEntity>> getConversations() {
        return conversations;
    }

    public Conversation createConversation(Topic topic) {
        Conversation conversation = Conversation.create(topic);
        repository.createConversation(conversation, null);
        return conversation;
    }

    public void deleteConversation(String conversationId) {
        repository.deleteConversation(conversationId);
    }
}
