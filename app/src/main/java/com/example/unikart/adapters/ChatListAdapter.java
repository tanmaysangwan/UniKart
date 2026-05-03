package com.example.unikart.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.models.ChatThread;
import com.example.unikart.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

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
        private final ImageView ivAvatar;
        private final TextView tvProductTitle;
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private final Context context;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            context         = itemView.getContext();
            tvName          = itemView.findViewById(R.id.tvName);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            ivAvatar        = itemView.findViewById(R.id.ivAvatar);
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

            // Avatar initial — shown until photo loads
            tvAvatarInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
            tvAvatarInitial.setVisibility(View.VISIBLE);
            ivAvatar.setVisibility(View.GONE);

            // Load the other person's profile picture from Firestore
            String otherUserId = iAmSeller ? thread.getBuyerId() : thread.getSellerId();
            if (otherUserId != null && !otherUserId.isEmpty()) {
                FirebaseFirestore.getInstance()
                        .collection(Constants.COLLECTION_USERS)
                        .document(otherUserId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            String avatarUrl = doc.getString("profilePicture");
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                tvAvatarInitial.setVisibility(View.GONE);
                                ivAvatar.setVisibility(View.VISIBLE);
                                Glide.with(context)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.ic_user_placeholder)
                                        .error(R.drawable.ic_user_placeholder)
                                        .circleCrop()
                                        .into(ivAvatar);
                            } else {
                                // No profile picture - show user icon
                                tvAvatarInitial.setVisibility(View.GONE);
                                ivAvatar.setVisibility(View.VISIBLE);
                                ivAvatar.setImageResource(R.drawable.ic_user_placeholder);
                            }
                        });
            }

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

            // Relative time
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
