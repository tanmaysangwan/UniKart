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
            holder.bind(messages.get(position), position);
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout bubbleContainer;
        private final LinearLayout bubble;
        private final TextView tvMessage;
        private final TextView tvTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            bubbleContainer = itemView.findViewById(R.id.bubbleContainer);
            bubble          = itemView.findViewById(R.id.bubble);
            tvMessage       = itemView.findViewById(R.id.tvMessage);
            tvTime          = itemView.findViewById(R.id.tvTime);
        }

        void bind(ChatMessage message, int position) {
            if (message == null) return;

            tvMessage.setText(message.getText());

            // Time inside bubble, bottom-right
            if (message.getSentAt() > 0) {
                tvTime.setText(new SimpleDateFormat("h:mm a", Locale.getDefault())
                        .format(new Date(message.getSentAt())));
                tvTime.setVisibility(View.VISIBLE);
            } else {
                tvTime.setVisibility(View.GONE);
            }

            boolean isMine = currentUserId != null
                    && currentUserId.equals(message.getSenderId());

            // Determine if this is the last message in a consecutive run
            // (used for tail vs no-tail bubble shape)
            boolean isLastInGroup = isLastInGroup(position);

            if (isMine) {
                // Sent — right side, indigo gradient bubble
                bubbleContainer.setGravity(Gravity.END);
                bubble.setBackgroundResource(isLastInGroup
                        ? R.drawable.bg_chat_bubble_sent
                        : R.drawable.bg_chat_bubble_sent_notail);
                tvMessage.setTextColor(0xFFFFFFFF);
                tvTime.setTextColor(0xAAFFFFFF);

                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) bubble.getLayoutParams();
                params.setMarginStart(60);
                params.setMarginEnd(0);
                bubble.setLayoutParams(params);
            } else {
                // Received — left side, white bubble
                bubbleContainer.setGravity(Gravity.START);
                bubble.setBackgroundResource(isLastInGroup
                        ? R.drawable.bg_chat_bubble_received
                        : R.drawable.bg_chat_bubble_received_notail);
                tvMessage.setTextColor(0xFF1A1A2E);
                tvTime.setTextColor(0xFF999999);

                LinearLayout.LayoutParams params =
                        (LinearLayout.LayoutParams) bubble.getLayoutParams();
                params.setMarginStart(0);
                params.setMarginEnd(60);
                bubble.setLayoutParams(params);
            }
        }

        /** True if the next message is from a different sender (or this is the last message). */
        private boolean isLastInGroup(int position) {
            if (position >= messages.size() - 1) return true;
            ChatMessage current = messages.get(position);
            ChatMessage next    = messages.get(position + 1);
            return !current.getSenderId().equals(next.getSenderId());
        }
    }
}
