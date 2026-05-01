package com.example.unikart.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unikart.R;
import com.example.unikart.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private final List<ChatMessage> messages;
    private final String currentUserId;

    public ChatMessageAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        if (position < messages.size()) {
            holder.bind(messages.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout bubbleContainer;
        private final TextView tvMessage;
        private final TextView tvTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            bubbleContainer = itemView.findViewById(R.id.bubbleContainer);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(ChatMessage message) {
            if (message == null) return;

            tvMessage.setText(message.getText());

            // Format time
            if (message.getSentAt() > 0) {
                String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(new Date(message.getSentAt()));
                tvTime.setText(time);
            }

            boolean isMine = currentUserId != null && currentUserId.equals(message.getSenderId());

            if (isMine) {
                bubbleContainer.setGravity(Gravity.END);
                tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_sent);
                tvMessage.setTextColor(0xFFFFFFFF);
                tvTime.setGravity(Gravity.END);
            } else {
                bubbleContainer.setGravity(Gravity.START);
                tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_received);
                tvMessage.setTextColor(0xFF1A1A2E);
                tvTime.setGravity(Gravity.START);
            }
        }
    }
}
