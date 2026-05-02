package com.example.unikart.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unikart.R;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.NotificationHelper;
import com.example.unikart.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager sessionManager = new SessionManager(this);
        FirebaseManager firebaseManager = FirebaseManager.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (firebaseManager.isUserLoggedIn() && sessionManager.isLoggedIn()) {
                // User is authenticated — check if we were launched from a notification tap
                Intent deepLink = resolveNotificationDeepLink();
                if (deepLink != null) {
                    startActivity(deepLink);
                } else {
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                }
            } else {
                sessionManager.logout();
                startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            }
            finish();
        }, SPLASH_DELAY_MS);
    }

    /**
     * Checks whether this activity was launched by tapping a notification.
     * If so, builds and returns the correct deep-link intent.
     * Returns null if this is a normal app launch.
     */
    private Intent resolveNotificationDeepLink() {
        Intent incoming = getIntent();
        if (incoming == null) return null;

        String notifType = incoming.getStringExtra(NotificationHelper.EXTRA_NOTIF_TYPE);
        if (notifType == null) return null;

        if (Constants.NOTIF_TYPE_CHAT.equals(notifType)) {
            String chatId     = incoming.getStringExtra(NotificationHelper.EXTRA_CHAT_ID);
            String senderName = incoming.getStringExtra(NotificationHelper.EXTRA_SELLER_NAME);
            if (chatId == null) chatId = incoming.getStringExtra("chat_id");
            return NotificationHelper.buildChatIntent(this, chatId, senderName);
        }

        if (Constants.NOTIF_TYPE_ORDER.equals(notifType)) {
            boolean openSellerTab = incoming.getBooleanExtra(
                    NotificationHelper.EXTRA_OPEN_SELLER_TAB, false);
            return NotificationHelper.buildOrderIntent(this, openSellerTab);
        }

        return null;
    }
}
