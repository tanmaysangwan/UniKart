package com.example.unikart.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unikart.R;
import com.example.unikart.adapters.ChatListAdapter;
import com.example.unikart.firebase.ChatRepository;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.models.ChatThread;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatsListActivity extends AppCompatActivity {

    private static final String TAG = "ChatsListActivity";

    private RecyclerView rvChats;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigation;

    private ChatRepository chatRepository;
    private ChatListAdapter chatListAdapter;
    private List<ChatThread> chatThreads;
    private ListenerRegistration chatsListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats_list);

        chatRepository = new ChatRepository();
        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        chatThreads = new ArrayList<>();

        initViews();
        setupRecyclerView();
        setupBottomNavigation();
        loadChats();
    }

    private void initViews() {
        rvChats = findViewById(R.id.rvChats);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Apply status bar height as top padding on the header so the gradient
        // fills behind the status bar and the text sits below it
        LinearLayout header = findViewById(R.id.chatListHeader);
        if (header != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                header.setOnApplyWindowInsetsListener((v, insets) -> {
                    int statusBarHeight = insets.getInsets(WindowInsets.Type.statusBars()).top;
                    v.setPadding(v.getPaddingLeft(), statusBarHeight + 16,
                            v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });
            } else {
                header.setOnApplyWindowInsetsListener((v, insets) -> {
                    int statusBarHeight = insets.getSystemWindowInsetTop();
                    v.setPadding(v.getPaddingLeft(), statusBarHeight + 16,
                            v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });
            }
        }
    }

    private void setupRecyclerView() {
        rvChats.setLayoutManager(new LinearLayoutManager(this));
        chatListAdapter = new ChatListAdapter(chatThreads, currentUserId);
        chatListAdapter.setOnChatClickListener(this::openChat);
        rvChats.setAdapter(chatListAdapter);
    }

    private void loadChats() {
        if (currentUserId == null) {
            showEmptyState("Not logged in");
            return;
        }

        showLoading(true);
        chatsListener = chatRepository.listenToUserChats(currentUserId,
                new ChatRepository.ChatListListener() {
                    @Override
                    public void onChats(List<ChatThread> chats) {
                        showLoading(false);
                        chatThreads.clear();
                        chatThreads.addAll(chats);
                        chatListAdapter.notifyDataSetChanged();

                        if (chatThreads.isEmpty()) {
                            showEmptyState("No conversations yet.\nStart chatting with sellers!");
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        Log.e(TAG, "loadChats error: " + error);
                        showEmptyState("Could not load chats.\n" + error);
                    }
                });
    }

    private void openChat(ChatThread thread) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chat_id", thread.getChatId());
        intent.putExtra("seller_id", thread.getSellerId());
        intent.putExtra("seller_name", thread.getSellerName());
        intent.putExtra("buyer_id", thread.getBuyerId());
        intent.putExtra("buyer_name", thread.getBuyerName());
        intent.putExtra("product_id", thread.getProductId());
        intent.putExtra("product_title", thread.getProductTitle());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_chat);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }
            if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddProductActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            if (id == R.id.nav_chat) return true;
            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, OrdersActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String message) {
        if (tvEmptyState != null) {
            tvEmptyState.setText(message);
            tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
