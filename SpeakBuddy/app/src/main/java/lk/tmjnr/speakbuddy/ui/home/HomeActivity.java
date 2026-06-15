package lk.tmjnr.speakbuddy.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import lk.tmjnr.speakbuddy.R;
import lk.tmjnr.speakbuddy.data.local.entity.ConversationEntity;
import lk.tmjnr.speakbuddy.databinding.ActivityHomeBinding;
import lk.tmjnr.speakbuddy.domain.model.Conversation;
import lk.tmjnr.speakbuddy.domain.model.Topic;
import lk.tmjnr.speakbuddy.ui.chat.ChatActivity;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private HomeViewModel viewModel;
    private ConversationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupSort();
        setupFab();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter();
        adapter.setListener(new ConversationAdapter.OnConversationClickListener() {
            @Override
            public void onClick(ConversationEntity conversation) {
                openChat(conversation.id, conversation.topic, conversation.title);
            }

            @Override
            public void onLongClick(ConversationEntity conversation) {
                showOptionsDialog(conversation);
            }
        });
        binding.recyclerConversations.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                viewModel.setSearchQuery(query);
                binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearch.setText("");
        });
    }

    private void setupFilters() {
        binding.chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipDaily) {
                viewModel.setCategoryFilter(Topic.DAILY.getKey());
            } else if (checkedId == R.id.chipInterview) {
                viewModel.setCategoryFilter(Topic.INTERVIEW.getKey());
            } else if (checkedId == R.id.chipPresentation) {
                viewModel.setCategoryFilter(Topic.PRESENTATION.getKey());
            } else if (checkedId == R.id.chipIelts) {
                viewModel.setCategoryFilter(Topic.IELTS.getKey());
            } else {
                viewModel.setCategoryFilter("all");
            }
        });
    }

    private void setupSort() {
        binding.btnSort.setOnClickListener(v -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, binding.btnSort);
            popup.getMenu().add(0, 1, 0, "Date");
            popup.getMenu().add(0, 2, 1, "Topic");
            popup.getMenu().add(0, 3, 2, "Most Mistakes");

            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == 1) {
                    viewModel.setSortOption(HomeViewModel.SortOption.DATE);
                    binding.btnSort.setText("Sort: Date");
                    return true;
                } else if (itemId == 2) {
                    viewModel.setSortOption(HomeViewModel.SortOption.TOPIC);
                    binding.btnSort.setText("Sort: Topic");
                    return true;
                } else if (itemId == 3) {
                    viewModel.setSortOption(HomeViewModel.SortOption.MISTAKES);
                    binding.btnSort.setText("Sort: Mistakes");
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void setupFab() {
        binding.fabNewChat.setOnClickListener(v -> showTopicSelector());
    }

    private void observeData() {
        viewModel.getFilteredConversations().observe(this, conversations -> {
            if (conversations == null || conversations.isEmpty()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.recyclerConversations.setVisibility(View.GONE);
            } else {
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.recyclerConversations.setVisibility(View.VISIBLE);
                adapter.setConversations(conversations);
            }
        });
    }

    private void showTopicSelector() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_topic, null);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.cardDaily).setOnClickListener(v -> {
            dialog.dismiss();
            createAndOpenChat(Topic.DAILY);
        });
        sheetView.findViewById(R.id.cardInterview).setOnClickListener(v -> {
            dialog.dismiss();
            createAndOpenChat(Topic.INTERVIEW);
        });
        sheetView.findViewById(R.id.cardPresentation).setOnClickListener(v -> {
            dialog.dismiss();
            createAndOpenChat(Topic.PRESENTATION);
        });
        sheetView.findViewById(R.id.cardIelts).setOnClickListener(v -> {
            dialog.dismiss();
            createAndOpenChat(Topic.IELTS);
        });

        dialog.show();
    }

    private void createAndOpenChat(Topic topic) {
        Conversation conversation = viewModel.createConversation(topic);
        openChat(conversation.getId(), topic.getKey(), topic.getDisplayName());
    }

    private void openChat(String conversationId, String topic, String title) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(ChatActivity.EXTRA_TOPIC, topic);
        intent.putExtra(ChatActivity.EXTRA_TITLE, title);
        startActivity(intent);
    }

    private void showOptionsDialog(ConversationEntity conversation) {
        String[] options = {
                conversation.isPinned ? "Unpin from Top" : "Pin to Top",
                "Delete Conversation"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        viewModel.togglePinConversation(conversation);
                    } else if (which == 1) {
                        showDeleteDialog(conversation);
                    }
                })
                .show();
    }

    private void showDeleteDialog(ConversationEntity conversation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to delete this conversation?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteConversation(conversation.id);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
