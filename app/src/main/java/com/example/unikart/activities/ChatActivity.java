package com.example.unikart.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.adapters.ChatMessageAdapter;
import com.example.unikart.firebase.ChatRepository;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.models.ChatMessage;
import com.example.unikart.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
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
    private String buyerId;
    private String buyerName;
    private String productId;
    private String productTitle;
    private String currentUserId;

    // The other participant's resolved ID, name, and avatar URL
    private String otherUserId;
    private String otherUserName;
    private String otherUserAvatar;

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
        buyerId      = getIntent().getStringExtra("buyer_id");
        buyerName    = getIntent().getStringExtra("buyer_name");
        productId    = getIntent().getStringExtra("product_id");
        productTitle = getIntent().getStringExtra("product_title");

        if (sellerName == null) sellerName = "Seller";
        if (buyerName  == null) buyerName  = "Buyer";
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

        // Status bar spacer — expand to exactly the system status bar height
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        if (statusBarSpacer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                statusBarSpacer.setOnApplyWindowInsetsListener((v, insets) -> {
                    int sbHeight = insets.getInsets(WindowInsets.Type.statusBars()).top;
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    lp.height = sbHeight;
                    v.setLayoutParams(lp);
                    return insets;
                });
            } else {
                statusBarSpacer.setOnApplyWindowInsetsListener((v, insets) -> {
                    int sbHeight = insets.getSystemWindowInsetTop();
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    lp.height = sbHeight;
                    v.setLayoutParams(lp);
                    return insets;
                });
            }
        }

        // Header: show the OTHER person's name
        // If current user is the seller, show the buyer's name; otherwise show the seller's name
        boolean iAmSeller = sellerId != null && sellerId.equals(currentUserId);
        String displayName = iAmSeller ? buyerName : sellerName;
        if (displayName == null || displayName.isEmpty()) displayName = iAmSeller ? "Buyer" : "Seller";

        otherUserId   = iAmSeller ? buyerId   : sellerId;
        otherUserName = displayName;

        tvChatTitle.setText(displayName);

        // Header: initial in avatar (shown until real photo loads)
        TextView tvHeaderInitial = findViewById(R.id.tvHeaderInitial);
        if (tvHeaderInitial != null && displayName.length() > 0) {
            tvHeaderInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
        }

        // Load the other user's profile picture from Firestore
        ImageView ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
        if (ivHeaderAvatar != null && otherUserId != null && !otherUserId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection(Constants.COLLECTION_USERS)
                    .document(otherUserId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String avatarUrl = doc.getString("profilePicture");
                        otherUserAvatar = avatarUrl;
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            // Hide the initial letter, show the photo
                            if (tvHeaderInitial != null) tvHeaderInitial.setVisibility(View.GONE);
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.bg_avatar_placeholder)
                                    .circleCrop()
                                    .into(ivHeaderAvatar);
                            ivHeaderAvatar.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "Could not load other user avatar", e));
        }

        // Tapping the header (name or avatar) opens the other user's public profile
        LinearLayout headerClickTarget = findViewById(R.id.headerClickTarget);
        if (headerClickTarget != null) {
            headerClickTarget.setOnClickListener(v -> openOtherUserProfile());
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

    private void openOtherUserProfile() {
        if (otherUserId == null || otherUserId.isEmpty()) return;
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_USER_ID,     otherUserId);
        intent.putExtra(UserProfileActivity.EXTRA_USER_NAME,   otherUserName);
        if (otherUserAvatar != null) {
            intent.putExtra(UserProfileActivity.EXTRA_USER_AVATAR, otherUserAvatar);
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
