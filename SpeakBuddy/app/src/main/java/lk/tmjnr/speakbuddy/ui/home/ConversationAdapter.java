package lk.tmjnr.speakbuddy.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.tmjnr.speakbuddy.R;
import lk.tmjnr.speakbuddy.data.local.entity.ConversationEntity;
import lk.tmjnr.speakbuddy.domain.model.Topic;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<ConversationEntity> conversations = new ArrayList<>();
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onClick(ConversationEntity conversation);
        void onLongClick(ConversationEntity conversation);
    }

    public void setListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setConversations(List<ConversationEntity> newList) {
        this.conversations = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationEntity conv = conversations.get(position);
        holder.bind(conv);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvLastMessage;
        private final TextView tvTimestamp;
        private final TextView tvMistakeCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvMistakeCount = itemView.findViewById(R.id.tvMistakeCount);
        }

        void bind(ConversationEntity conv) {
            tvTitle.setText(conv.title != null ? conv.title : "Conversation");

            if (conv.lastMessage != null && !conv.lastMessage.isEmpty()) {
                tvLastMessage.setText(conv.lastMessage);
                tvLastMessage.setVisibility(View.VISIBLE);
            } else {
                tvLastMessage.setVisibility(View.GONE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date(conv.updatedAt)));

            if (conv.totalMistakes > 0) {
                tvMistakeCount.setText(String.format(Locale.getDefault(), "%d mistakes", conv.totalMistakes));
                tvMistakeCount.setVisibility(View.VISIBLE);
            } else {
                tvMistakeCount.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(conv);
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(conv);
                return true;
            });
        }
    }
}
