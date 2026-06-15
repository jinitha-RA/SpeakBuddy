package lk.tmjnr.speakbuddy.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lk.tmjnr.speakbuddy.data.local.AppDatabase;
import lk.tmjnr.speakbuddy.data.local.dao.ConversationDao;
import lk.tmjnr.speakbuddy.data.local.dao.MessageDao;
import lk.tmjnr.speakbuddy.data.local.entity.ConversationEntity;
import lk.tmjnr.speakbuddy.data.local.entity.MessageEntity;
import lk.tmjnr.speakbuddy.data.remote.ApiService;
import lk.tmjnr.speakbuddy.data.remote.RetrofitClient;
import lk.tmjnr.speakbuddy.data.remote.dto.ChatRequest;
import lk.tmjnr.speakbuddy.data.remote.dto.ChatResponse;
import lk.tmjnr.speakbuddy.domain.model.Conversation;
import lk.tmjnr.speakbuddy.domain.model.GrammarMistake;
import lk.tmjnr.speakbuddy.domain.model.Message;
import lk.tmjnr.speakbuddy.domain.model.Topic;
import lk.tmjnr.speakbuddy.util.Resource;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository that coordinates local Room DB and remote API calls.
 */
public class ChatRepository {

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final ApiService apiService;
    private final Gson gson;
    private final ExecutorService executor;

    private static volatile ChatRepository INSTANCE;

    private ChatRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.conversationDao = db.conversationDao();
        this.messageDao = db.messageDao();
        this.apiService = RetrofitClient.getInstance();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static ChatRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ChatRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ChatRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // ── Conversations ──────────────────────────────────────────

    public LiveData<List<ConversationEntity>> getAllConversations() {
        return conversationDao.getAllConversations();
    }

    public void createConversation(Conversation conversation, Runnable onComplete) {
        executor.execute(() -> {
            ConversationEntity entity = new ConversationEntity();
            entity.id = conversation.getId();
            entity.topic = conversation.getTopic();
            entity.title = conversation.getTitle();
            entity.createdAt = conversation.getCreatedAt();
            entity.updatedAt = conversation.getUpdatedAt();
            entity.totalMessages = 0;
            entity.totalMistakes = 0;
            entity.isPinned = false;
            conversationDao.insert(entity);
            if (onComplete != null)
                onComplete.run();
        });
    }

    public void updatePinnedStatus(String id, boolean pinned) {
        executor.execute(() -> conversationDao.updatePinnedStatus(id, pinned));
    }

    public void deleteConversation(String conversationId) {
        executor.execute(() -> conversationDao.deleteById(conversationId));
    }

    // ── Messages ───────────────────────────────────────────────

    public LiveData<List<MessageEntity>> getMessages(String conversationId) {
        return messageDao.getMessagesForConversation(conversationId);
    }

    /**
     * Save a user message locally and send it to the AI backend.
     */
    public LiveData<Resource<ChatResponse>> sendMessage(
            String conversationId, String transcript, String topic) {

        MutableLiveData<Resource<ChatResponse>> result = new MutableLiveData<>();
        result.postValue(Resource.loading());

        executor.execute(() -> {
            // 1. Save user message locally
            MessageEntity userMsg = new MessageEntity();
            userMsg.id = UUID.randomUUID().toString();
            userMsg.conversationId = conversationId;
            userMsg.isUser = true;
            userMsg.text = transcript;
            userMsg.timestamp = System.currentTimeMillis();
            messageDao.insert(userMsg);

            // Update conversation
            conversationDao.updateOnNewMessage(conversationId,
                    System.currentTimeMillis(), transcript);

            // 2. Build history context (last 10 messages)
            List<MessageEntity> recentMessages = messageDao.getRecentMessages(conversationId, 50);
            List<ChatRequest.HistoryItem> history = new ArrayList<>();
            // Reverse to chronological order
            for (int i = recentMessages.size() - 1; i >= 0; i--) {
                MessageEntity m = recentMessages.get(i);
                history.add(new ChatRequest.HistoryItem(m.isUser, m.text));
            }

            // 3. Call API
            ChatRequest request = new ChatRequest(transcript, conversationId, topic, history);
            apiService.sendMessage(request).enqueue(new Callback<ChatResponse>() {
                @Override
                public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ChatResponse aiResp = response.body();
                        // Save AI response locally
                        executor.execute(() -> {
                            MessageEntity aiMsg = new MessageEntity();
                            aiMsg.id = UUID.randomUUID().toString();
                            aiMsg.conversationId = conversationId;
                            aiMsg.isUser = false;
                            aiMsg.text = aiResp.getReply();
                            aiMsg.correctedSentence = aiResp.getCorrectedSentence();
                            aiMsg.mistakesJson = gson.toJson(aiResp.getMistakes());
                            aiMsg.followUpSuggestion = aiResp.getFollowUpSuggestion();
                            aiMsg.timestamp = System.currentTimeMillis();
                            messageDao.insert(aiMsg);

                            conversationDao.updateOnNewMessage(conversationId,
                                    System.currentTimeMillis(),
                                    aiResp.getReply());

                            if (aiResp.getMistakes() != null && !aiResp.getMistakes().isEmpty()) {
                                conversationDao.incrementMistakeCount(
                                        conversationId, aiResp.getMistakes().size());
                            }
                        });
                        result.postValue(Resource.success(aiResp));
                    } else {
                        result.postValue(Resource.error("Server error: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<ChatResponse> call, Throwable t) {
                    result.postValue(Resource.error("Network error: " + t.getMessage()));
                }
            });
        });

        return result;
    }

    /**
     * Save an AI opening message locally (no API call needed).
     */
    public void saveAiMessage(String conversationId, String text) {
        executor.execute(() -> {
            MessageEntity msg = new MessageEntity();
            msg.id = UUID.randomUUID().toString();
            msg.conversationId = conversationId;
            msg.isUser = false;
            msg.text = text;
            msg.timestamp = System.currentTimeMillis();
            messageDao.insert(msg);

            conversationDao.updateOnNewMessage(conversationId,
                    System.currentTimeMillis(), text);
        });
    }

    // ── Helpers ────────────────────────────────────────────────

    /**
     * Convert a MessageEntity to a domain Message.
     */
    public Message entityToMessage(MessageEntity entity) {
        Message m = new Message();
        m.setId(entity.id);
        m.setConversationId(entity.conversationId);
        m.setUser(entity.isUser);
        m.setText(entity.text);
        m.setCorrectedSentence(entity.correctedSentence);
        m.setFollowUpSuggestion(entity.followUpSuggestion);
        m.setTimestamp(entity.timestamp);

        if (entity.mistakesJson != null && !entity.mistakesJson.isEmpty()
                && !entity.mistakesJson.equals("null")) {
            Type listType = new TypeToken<List<GrammarMistake>>() {
            }.getType();
            try {
                List<GrammarMistake> mistakes = gson.fromJson(entity.mistakesJson, listType);
                m.setMistakes(mistakes);
            } catch (Exception e) {
                m.setMistakes(new ArrayList<>());
            }
        }
        return m;
    }
}
