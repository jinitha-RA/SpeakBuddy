package lk.tmjnr.speakbuddy.ui.chat;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import lk.tmjnr.speakbuddy.R;
import lk.tmjnr.speakbuddy.databinding.ActivityChatBinding;
import lk.tmjnr.speakbuddy.domain.model.Topic;
import lk.tmjnr.speakbuddy.speech.SpeechCallback;
import lk.tmjnr.speakbuddy.speech.SpeechRecognizerManager;
import lk.tmjnr.speakbuddy.speech.TextToSpeechManager;
import lk.tmjnr.speakbuddy.util.Resource;

/**
 * Main chat screen where users practice speaking with the AI.
 * Integrates SpeechRecognizer (STT), Gemini AI (via API), and TextToSpeech (TTS).
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_TOPIC = "topic";
    public static final String EXTRA_TITLE = "title";

    private ActivityChatBinding binding;
    private ChatViewModel viewModel;
    private ChatAdapter chatAdapter;

    private SpeechRecognizerManager speechManager;
    private TextToSpeechManager ttsManager;
    private AnimatorSet pulseAnimator;

    private boolean isNewConversation = false;

    // Permission launcher
    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startListening();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.error_mic_permission,
                            Snackbar.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get extras
        String conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        String topic = getIntent().getStringExtra(EXTRA_TOPIC);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        if (conversationId == null) {
            finish();
            return;
        }

        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.init(conversationId, topic);

        // Setup toolbar
        binding.toolbar.setTitle(title != null ? title : "SpeakBuddy");
        binding.toolbar.setSubtitle(Topic.fromKey(topic).getDisplayName());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Setup chat
        setupChatRecyclerView();
        setupSpeechManagers();
        setupMicButton();
        observeViewModel();

        // Check if this is a new conversation (no messages yet)
        isNewConversation = true;
    }

    private void setupChatRecyclerView() {
        chatAdapter = new ChatAdapter();
        chatAdapter.setReplayListener(text -> {
            if (ttsManager != null) {
                ttsManager.speak(text);
                viewModel.setMicState(ChatViewModel.MicState.SPEAKING);
            }
        });
        binding.recyclerChat.setAdapter(chatAdapter);
    }

    private void setupSpeechManagers() {
        // Initialize STT
        speechManager = new SpeechRecognizerManager(this, new SpeechCallback() {
            @Override
            public void onPartialResult(String partial) {
                viewModel.setLiveTranscript(partial);
            }

            @Override
            public void onFinalResult(String transcript) {
                viewModel.setLiveTranscript("");

                if (transcript != null && !transcript.trim().isEmpty()) {
                    // Stop listening while processing
                    if (speechManager != null) speechManager.stopListening();

                    // Send to AI
                    chatAdapter.showTypingIndicator();
                    scrollToBottom();

                    viewModel.sendMessage(transcript).observe(ChatActivity.this, resource -> {
                        if (resource == null) return;

                        switch (resource.getStatus()) {
                            case SUCCESS:
                                chatAdapter.removeTypingIndicator();
                                if (resource.getData() != null) {
                                    // TTS speaks the reply
                                    String reply = resource.getData().getReply();
                                    if (reply != null && !reply.isEmpty()) {
                                        ttsManager.speak(reply);
                                        viewModel.setMicState(ChatViewModel.MicState.SPEAKING);
                                    } else {
                                        viewModel.setMicState(ChatViewModel.MicState.IDLE);
                                    }
                                }
                                break;
                            case ERROR:
                                chatAdapter.removeTypingIndicator();
                                viewModel.setMicState(ChatViewModel.MicState.IDLE);
                                Snackbar.make(binding.getRoot(),
                                        resource.getMessage() != null ? resource.getMessage() : getString(R.string.error_api),
                                        Snackbar.LENGTH_LONG)
                                        .setAction(R.string.retry, v -> {
                                            // Retry last message
                                        })
                                        .show();
                                break;
                            case LOADING:
                                viewModel.setMicState(ChatViewModel.MicState.PROCESSING);
                                break;
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                viewModel.setMicState(ChatViewModel.MicState.IDLE);
                Toast.makeText(ChatActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onListeningStateChanged(boolean isListening) {
                if (isListening) {
                    viewModel.setMicState(ChatViewModel.MicState.LISTENING);
                }
            }
        });

        // Initialize TTS
        ttsManager = new TextToSpeechManager(this, new TextToSpeechManager.TtsCallback() {
            @Override
            public void onSpeechStarted() {
                viewModel.setMicState(ChatViewModel.MicState.SPEAKING);
            }

            @Override
            public void onSpeechCompleted() {
                viewModel.setMicState(ChatViewModel.MicState.IDLE);
            }

            @Override
            public void onError(String message) {
                viewModel.setMicState(ChatViewModel.MicState.IDLE);
            }
        });
    }

    private void setupMicButton() {
        binding.fabMic.setOnClickListener(v -> {
            ChatViewModel.MicState state = viewModel.getMicState().getValue();
            if (state == null) state = ChatViewModel.MicState.IDLE;

            switch (state) {
                case IDLE:
                    checkPermissionAndListen();
                    break;
                case LISTENING:
                    speechManager.stopListening();
                    viewModel.setMicState(ChatViewModel.MicState.IDLE);
                    viewModel.setLiveTranscript("");
                    break;
                case SPEAKING:
                    ttsManager.stop();
                    viewModel.setMicState(ChatViewModel.MicState.IDLE);
                    break;
                case PROCESSING:
                    // Ignore taps during processing
                    break;
            }
        });
    }

    private void checkPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startListening() {
        ttsManager.stop(); // Prevent echo
        speechManager.startListening();
    }

    private void observeViewModel() {
        // Observe messages from Room DB
        viewModel.getMessages().observe(this, messages -> {
            if (messages != null) {
                chatAdapter.setMessages(messages);
                scrollToBottom();

                // Send opening message for new conversations
                if (isNewConversation && messages.isEmpty()) {
                    isNewConversation = false;
                    sendOpeningMessage();
                } else {
                    isNewConversation = false;
                }
            }
        });

        // Observe mic state for UI updates
        viewModel.getMicState().observe(this, this::updateMicButtonState);

        // Observe live transcript
        viewModel.getLiveTranscript().observe(this, transcript -> {
            if (transcript != null && !transcript.isEmpty()) {
                binding.tvLiveTranscript.setText(transcript);
            } else {
                ChatViewModel.MicState state = viewModel.getMicState().getValue();
                if (state == ChatViewModel.MicState.LISTENING) {
                    binding.tvLiveTranscript.setText(R.string.listening);
                } else {
                    binding.tvLiveTranscript.setText("");
                    binding.tvLiveTranscript.setHint(R.string.tap_mic_hint);
                }
            }
        });

        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void updateMicButtonState(ChatViewModel.MicState state) {
        stopPulseAnimation();

        switch (state) {
            case IDLE:
                binding.fabMic.setImageResource(R.drawable.ic_mic);
                binding.fabMic.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.mic_idle)));
                binding.fabMic.setEnabled(true);
                binding.tvLiveTranscript.setHint(R.string.tap_mic_hint);
                break;

            case LISTENING:
                binding.fabMic.setImageResource(R.drawable.ic_mic_active);
                binding.fabMic.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.mic_listening)));
                binding.fabMic.setEnabled(true);
                startPulseAnimation();
                binding.tvLiveTranscript.setText(R.string.listening);
                break;

            case PROCESSING:
                binding.fabMic.setImageResource(R.drawable.ic_mic);
                binding.fabMic.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.mic_processing)));
                binding.fabMic.setEnabled(false);
                binding.tvLiveTranscript.setText(R.string.processing);
                break;

            case SPEAKING:
                binding.fabMic.setImageResource(R.drawable.ic_volume_up);
                binding.fabMic.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.mic_speaking)));
                binding.fabMic.setEnabled(true);
                binding.tvLiveTranscript.setText(R.string.speaking);
                break;
        }
    }

    private void startPulseAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.fabMic, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.fabMic, "scaleY", 1f, 1.15f, 1f);
        pulseAnimator = new AnimatorSet();
        pulseAnimator.playTogether(scaleX, scaleY);
        pulseAnimator.setDuration(800);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                ChatViewModel.MicState state = viewModel.getMicState().getValue();
                if (state == ChatViewModel.MicState.LISTENING && pulseAnimator != null) {
                    pulseAnimator.start();
                }
            }
        });
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
            binding.fabMic.setScaleX(1f);
            binding.fabMic.setScaleY(1f);
        }
    }

    private void sendOpeningMessage() {
        String topic = getIntent().getStringExtra(EXTRA_TOPIC);
        String openingMessage;

        switch (Topic.fromKey(topic)) {
            case INTERVIEW:
                openingMessage = getString(R.string.opening_interview);
                break;
            case PRESENTATION:
                openingMessage = getString(R.string.opening_presentation);
                break;
            case IELTS:
                openingMessage = getString(R.string.opening_ielts);
                break;
            default:
                openingMessage = getString(R.string.opening_daily);
                break;
        }

        viewModel.saveOpeningMessage(openingMessage);

        // Speak the opening message after a short delay
        binding.getRoot().postDelayed(() -> {
            if (!isFinishing()) {
                ttsManager.speak(openingMessage);
                viewModel.setMicState(ChatViewModel.MicState.SPEAKING);
            }
        }, 500);
    }

    private void scrollToBottom() {
        binding.recyclerChat.postDelayed(() -> {
            int itemCount = chatAdapter.getItemCount();
            if (itemCount > 0) {
                binding.recyclerChat.smoothScrollToPosition(itemCount - 1);
            }
        }, 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechManager != null) {
            speechManager.destroy();
        }
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        stopPulseAnimation();
    }
}
