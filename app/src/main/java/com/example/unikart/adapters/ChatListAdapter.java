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
        holder.bind(chatThreads.get(position));
    }

    @Override
    public int getItemCount() {
        return chatThreads.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvAvatarInitial;
        private final TextView tvProductTitle;
        private final TextView tvLastMessage;
        private final TextView tvTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName          = itemView.findViewById(R.id.tvName);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvProductTitle  = itemView.findViewById(R.id.tvProductTitle);
            tvLastMessage   = itemView.findViewById(R.id.tvLastMessage);
            tvTime          = itemView.findViewById(R.id.tvTime);
        }

        public void bind(ChatThread thread) {
            // Show the OTHER person's name
            boolean iAmSeller = thread.getSellerId() != null
                    && thread.getSellerId().equals(currentUserId);
            String displayName = iAmSeller ? thread.getBuyerName() : thread.getSellerName();
            if (displayName == null || displayName.isEmpty()) displayName = iAmSeller ? "Buyer" : "Seller";
            tvName.setText(displayName);

            // Avatar initial — first letter of display name, uppercase
            tvAvatarInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());

            // Product tag
            String product = thread.getProductTitle();
            if (product != null && !product.isEmpty()) {
                tvProductTitle.setVisibility(View.VISIBLE);
                tvProductTitle.setText(product);
            } else {
                tvProductTitle.setVisibility(View.GONE);
            }

            // Last message preview
            String last = thread.getLastMessage();
            tvLastMessage.setText((last != null && !last.isEmpty()) ? last : "No messages yet");

            // Relative time (e.g. "2 min ago", "Yesterday")
            if (thread.getLastMessageAt() > 0) {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                        thread.getLastMessageAt(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                );
                tvTime.setText(timeAgo);
            } else {
                tvTime.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onChatClick(thread);
            });
        }
    }
}
