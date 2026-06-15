package lk.tmjnr.speakbuddy.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.tmjnr.speakbuddy.R;
import lk.tmjnr.speakbuddy.domain.model.Message;
import lk.tmjnr.speakbuddy.util.SpannableUtils;

/**
 * RecyclerView adapter for chat messages.
 * Supports user messages, AI messages (with grammar corrections), and typing indicator.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI = 1;
    private static final int TYPE_TYPING = 2;

    private final List<Message> messages = new ArrayList<>();
    private OnReplayClickListener replayListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public interface OnReplayClickListener {
        void onReplay(String text);
    }

    public void setReplayListener(OnReplayClickListener listener) {
        this.replayListener = listener;
    }

    public void setMessages(List<Message> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void showTypingIndicator() {
        // Remove existing typing indicator first
        removeTypingIndicator();
        messages.add(Message.typingIndicator());
        notifyItemInserted(messages.size() - 1);
    }

    public void removeTypingIndicator() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).isTypingIndicator()) {
                messages.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (msg.isTypingIndicator()) return TYPE_TYPING;
        return msg.isUser() ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_USER:
                return new UserViewHolder(
                        inflater.inflate(R.layout.item_message_user, parent, false));
            case TYPE_AI:
                return new AiViewHolder(
                        inflater.inflate(R.layout.item_message_ai, parent, false));
            case TYPE_TYPING:
                return new TypingViewHolder(
                        inflater.inflate(R.layout.item_typing_indicator, parent, false));
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);

        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else if (holder instanceof AiViewHolder) {
            ((AiViewHolder) holder).bind(msg);
        }
        // TypingViewHolder doesn't need binding
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ── ViewHolders ───────────────────────────────────────────

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTime;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
            tvTime = itemView.findViewById(R.id.tvUserTime);
        }

        void bind(Message msg) {
            tvMessage.setText(msg.getText());
            tvTime.setText(timeFormat.format(new Date(msg.getTimestamp())));
        }
    }

    class AiViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout layoutCorrection;
        private final TextView tvCorrections;
        private final TextView tvReply;
        private final TextView tvTime;
        private final ImageButton btnReplay;

        AiViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCorrection = itemView.findViewById(R.id.layoutCorrection);
            tvCorrections = itemView.findViewById(R.id.tvCorrections);
            tvReply = itemView.findViewById(R.id.tvAiReply);
            tvTime = itemView.findViewById(R.id.tvAiTime);
            btnReplay = itemView.findViewById(R.id.btnReplay);
        }

        void bind(Message msg) {
            tvReply.setText(msg.getText());
            tvTime.setText(timeFormat.format(new Date(msg.getTimestamp())));

            // Show corrections if present
            if (msg.getMistakes() != null && !msg.getMistakes().isEmpty()) {
                layoutCorrection.setVisibility(View.VISIBLE);
                tvCorrections.setText(SpannableUtils.buildCorrectionSpannable(msg.getMistakes()));
            } else {
                layoutCorrection.setVisibility(View.GONE);
            }

            // Replay button
            btnReplay.setOnClickListener(v -> {
                if (replayListener != null && msg.getText() != null) {
                    replayListener.onReplay(msg.getText());
                }
            });
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        TypingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
