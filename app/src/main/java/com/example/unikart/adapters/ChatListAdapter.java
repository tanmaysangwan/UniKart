package com.example.unikart.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unikart.R;
import com.example.unikart.models.ChatThread;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final List<ChatThread> chatThreads;
    private final String currentUserId;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ChatThread thread);
    }

    public ChatListAdapter(List<ChatThread> chatThreads, String currentUserId) {
        this.chatThreads = chatThreads;
        this.currentUserId = currentUserId;
    }

    public void setOnChatClickListener(OnChatClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_thread, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatThread thread = chatThreads.get(position);
        holder.bind(thread);
    }

    @Override
    public int getItemCount() {
        return chatThreads.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvProductTitle;
        private final TextView tvLastMessage;
        private final TextView tvTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvProductTitle = itemView.findViewById(R.id.tvProductTitle);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        public void bind(ChatThread thread) {
            // Show other person's name
            String displayName = thread.getSellerId().equals(currentUserId)
                    ? "Buyer"
                    : thread.getSellerName();
            tvName.setText(displayName);

            tvProductTitle.setText(thread.getProductTitle() != null && !thread.getProductTitle().isEmpty()
                    ? thread.getProductTitle()
                    : "General Chat");

            tvLastMessage.setText(thread.getLastMessage() != null && !thread.getLastMessage().isEmpty()
                    ? thread.getLastMessage()
                    : "No messages yet");

            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    thread.getLastMessageAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            tvTime.setText(timeAgo);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(thread);
                }
            });
        }
    }
}
