package com.example.unikart.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unikart.R;
import com.example.unikart.adapters.ChatMessageAdapter;
import com.example.unikart.firebase.ChatRepository;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.models.ChatMessage;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private RecyclerView rvChatMessages;
    private EditText etMessage;
    private FrameLayout btnSend;
    private TextView tvChatTitle;
    private ProgressBar progressBar;

    private ChatRepository chatRepository;
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList;
    private ListenerRegistration messagesListener;

    private String chatId;
    private String sellerId;
    private String sellerName;
    private String productId;
    private String productTitle;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatRepository = new ChatRepository();
        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        messageList = new ArrayList<>();

        // Read intent extras
        sellerId     = getIntent().getStringExtra("seller_id");
        sellerName   = getIntent().getStringExtra("seller_name");
        productId    = getIntent().getStringExtra("product_id");
        productTitle = getIntent().getStringExtra("product_title");

        if (sellerName == null) sellerName = "Chat";
        if (sellerId   == null) sellerId   = "";

        initViews();
        setupRecyclerView();
        openOrCreateChat();
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rvChatMessages);
        etMessage      = findViewById(R.id.etMessage);
        btnSend        = findViewById(R.id.btnSend);
        tvChatTitle    = findViewById(R.id.tvChatTitle);
        progressBar    = findViewById(R.id.progressBar);

        // Header: name
        tvChatTitle.setText(sellerName);

        // Header: initial in avatar
        TextView tvHeaderInitial = findViewById(R.id.tvHeaderInitial);
        if (tvHeaderInitial != null && sellerName.length() > 0) {
            tvHeaderInitial.setText(String.valueOf(sellerName.charAt(0)).toUpperCase());
        }

        // Header: product subtitle
        TextView tvChatSubtitle = findViewById(R.id.tvChatSubtitle);
        if (tvChatSubtitle != null) {
            if (productTitle != null && !productTitle.isEmpty()) {
                tvChatSubtitle.setText(productTitle);
            } else {
                tvChatSubtitle.setVisibility(View.GONE);
            }
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        messageAdapter = new ChatMessageAdapter(messageList, currentUserId);
        rvChatMessages.setAdapter(messageAdapter);
    }

    private void openOrCreateChat() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Check if chat_id was passed directly
        String existingChatId = getIntent().getStringExtra("chat_id");
        if (existingChatId != null && !existingChatId.isEmpty()) {
            chatId = existingChatId;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Log.d(TAG, "Using existing chat: " + chatId);
            listenToMessages();
            return;
        }

        // Otherwise create or get chat
        chatRepository.getOrCreateChat(sellerId, sellerName, productId, productTitle,
                new ChatRepository.ChatCallback() {
                    @Override
                    public void onSuccess(String id) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        chatId = id;
                        Log.d(TAG, "Chat ready: " + chatId);
                        listenToMessages();
                    }

                    @Override
                    public void onFailure(String error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "openOrCreateChat failed: " + error);
                        tvChatTitle.setText(sellerName + " (offline)");
                    }
                });
    }

    private void listenToMessages() {
        if (chatId == null) return;

        messagesListener = chatRepository.listenToMessages(chatId,
                new ChatRepository.MessagesListener() {
                    @Override
                    public void onMessages(List<ChatMessage> messages) {
                        messageList.clear();
                        messageList.addAll(messages);
                        messageAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            rvChatMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Messages listener error: " + error);
                    }
                });
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;
        if (chatId == null) {
            Log.w(TAG, "sendMessage: chatId is null, chat not ready yet");
            return;
        }

        etMessage.setText("");
        btnSend.setEnabled(false);

        chatRepository.sendMessage(chatId, text, new ChatRepository.MessageCallback() {
            @Override
            public void onSuccess(String message) {
                btnSend.setEnabled(true);
                Log.d(TAG, "Message sent");
            }

            @Override
            public void onFailure(String error) {
                btnSend.setEnabled(true);
                Log.e(TAG, "sendMessage failed: " + error);
                etMessage.setText(text); // restore text on failure
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
