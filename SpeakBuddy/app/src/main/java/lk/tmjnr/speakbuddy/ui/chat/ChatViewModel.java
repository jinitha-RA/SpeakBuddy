package lk.tmjnr.speakbuddy.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.List;

import lk.tmjnr.speakbuddy.data.local.entity.MessageEntity;
import lk.tmjnr.speakbuddy.data.remote.dto.ChatResponse;
import lk.tmjnr.speakbuddy.data.repository.ChatRepository;
import lk.tmjnr.speakbuddy.domain.model.Message;
import lk.tmjnr.speakbuddy.util.Resource;

/**
 * ViewModel for the chat screen. Manages mic state, messages, and API calls.
 */
public class ChatViewModel extends AndroidViewModel {

    public enum MicState {
        IDLE,        // Not listening
        LISTENING,   // Actively recording
        PROCESSING,  // Waiting for API
        SPEAKING     // TTS is playing
    }

    private final ChatRepository repository;
    private final MutableLiveData<MicState> micState = new MutableLiveData<>(MicState.IDLE);
    private final MutableLiveData<String> liveTranscript = new MutableLiveData<>("");
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private String conversationId;
    private String topic;
    private LiveData<List<Message>> messages;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = ChatRepository.getInstance(application);
    }

    /**
     * Initialize with conversation details.
     */
    public void init(String conversationId, String topic) {
        this.conversationId = conversationId;
        this.topic = topic;

        // Transform MessageEntity list to domain Message list
        LiveData<List<MessageEntity>> entityMessages = repository.getMessages(conversationId);
        messages = Transformations.map(entityMessages, entities -> {
            List<Message> result = new ArrayList<>();
            if (entities != null) {
                for (MessageEntity entity : entities) {
                    result.add(repository.entityToMessage(entity));
                }
            }
            return result;
        });
    }

    public LiveData<List<Message>> getMessages() {
        return messages;
    }

    public LiveData<MicState> getMicState() {
        return micState;
    }

    public LiveData<String> getLiveTranscript() {
        return liveTranscript;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public String getConversationId() {
        return conversationId;
    }

    // ── Mic state management ──────────────────────────────────

    public void setMicState(MicState state) {
        micState.postValue(state);
    }

    public void setLiveTranscript(String text) {
        liveTranscript.postValue(text);
    }

    // ── API call ──────────────────────────────────────────────

    /**
     * Send user's transcript to the AI backend and get a response.
     */
    public LiveData<Resource<ChatResponse>> sendMessage(String transcript) {
        micState.postValue(MicState.PROCESSING);
        liveTranscript.postValue("");
        return repository.sendMessage(conversationId, transcript, topic);
    }

    /**
     * Save the AI's opening message for a new conversation.
     */
    public void saveOpeningMessage(String text) {
        repository.saveAiMessage(conversationId, text);
    }

    public void postError(String message) {
        errorMessage.postValue(message);
    }
}
