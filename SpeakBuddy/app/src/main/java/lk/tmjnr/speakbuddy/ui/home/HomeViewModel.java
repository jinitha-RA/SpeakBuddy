package lk.tmjnr.speakbuddy.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import lk.tmjnr.speakbuddy.data.local.entity.ConversationEntity;
import lk.tmjnr.speakbuddy.data.repository.ChatRepository;
import lk.tmjnr.speakbuddy.domain.model.Conversation;
import lk.tmjnr.speakbuddy.domain.model.Topic;

public class HomeViewModel extends AndroidViewModel {

    public enum SortOption {
        DATE,
        TOPIC,
        MISTAKES
    }

    private final ChatRepository repository;
    private final LiveData<List<ConversationEntity>> rawConversations;
    
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> categoryFilter = new MutableLiveData<>("all");
    private final MutableLiveData<SortOption> sortOption = new MutableLiveData<>(SortOption.DATE);
    
    private final MediatorLiveData<List<ConversationEntity>> filteredConversations = new MediatorLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = ChatRepository.getInstance(application);
        rawConversations = repository.getAllConversations();

        filteredConversations.addSource(rawConversations, convs -> combineAndFilter());
        filteredConversations.addSource(searchQuery, query -> combineAndFilter());
        filteredConversations.addSource(categoryFilter, category -> combineAndFilter());
        filteredConversations.addSource(sortOption, sort -> combineAndFilter());
    }

    public LiveData<List<ConversationEntity>> getConversations() {
        return rawConversations;
    }

    public LiveData<List<ConversationEntity>> getFilteredConversations() {
        return filteredConversations;
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void setCategoryFilter(String category) {
        categoryFilter.setValue(category);
    }

    public void setSortOption(SortOption option) {
        sortOption.setValue(option);
    }

    public SortOption getSortOption() {
        return sortOption.getValue();
    }

    public void togglePinConversation(ConversationEntity conversation) {
        repository.updatePinnedStatus(conversation.id, !conversation.isPinned);
    }

    private void combineAndFilter() {
        List<ConversationEntity> list = rawConversations.getValue();
        if (list == null) {
            filteredConversations.setValue(new ArrayList<>());
            return;
        }

        String query = searchQuery.getValue();
        String category = categoryFilter.getValue();
        SortOption sort = sortOption.getValue();

        List<ConversationEntity> filtered = new ArrayList<>();
        for (ConversationEntity c : list) {
            // 1. Filter by category
            if (category != null && !category.equals("all")) {
                if (c.topic == null || !c.topic.equals(category)) {
                    continue;
                }
            }

            // 2. Filter by search query
            if (query != null && !query.trim().isEmpty()) {
                String q = query.toLowerCase().trim();
                boolean matchesTitle = c.title != null && c.title.toLowerCase().contains(q);
                boolean matchesLastMsg = c.lastMessage != null && c.lastMessage.toLowerCase().contains(q);
                if (!matchesTitle && !matchesLastMsg) {
                    continue;
                }
            }

            filtered.add(c);
        }

        // 3. Sort
        filtered.sort((c1, c2) -> {
            // Pinned conversations always go first
            if (c1.isPinned != c2.isPinned) {
                return c1.isPinned ? -1 : 1;
            }

            // Otherwise, sort according to selected option
            if (sort == SortOption.TOPIC) {
                String t1 = c1.title != null ? c1.title : "";
                String t2 = c2.title != null ? c2.title : "";
                return t1.compareToIgnoreCase(t2);
            } else if (sort == SortOption.MISTAKES) {
                return Integer.compare(c2.totalMistakes, c1.totalMistakes); // descending
            } else {
                // Default to DATE (updatedAt descending)
                return Long.compare(c2.updatedAt, c1.updatedAt);
            }
        });

        filteredConversations.setValue(filtered);
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
