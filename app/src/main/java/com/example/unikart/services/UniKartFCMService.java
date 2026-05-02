package com.example.unikart.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.unikart.utils.Constants;
import com.example.unikart.utils.FCMTokenManager;
import com.example.unikart.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Handles incoming FCM messages and token refreshes.
 *
 * When the app is in the FOREGROUND or receives a data-only message:
 *   onMessageReceived() fires → we build and show the notification ourselves
 *   with a PendingIntent that goes directly to ChatActivity / OrdersActivity.
 *
 * When the app is KILLED and the FCM payload has a "notification" block:
 *   The FCM SDK shows the notification automatically via the system tray.
 *   Tapping it launches the launcher activity (SplashActivity).
 *   SplashActivity reads the notification extras from its intent and forwards
 *   the user to the correct screen after the auth check.
 *
 * We send data-only messages (no "notification" block) from NotificationSender
 * so that onMessageReceived() always fires regardless of app state, giving us
 * full control over the deep-link PendingIntent.
 */
public class UniKartFCMService extends FirebaseMessagingService {

    private static final String TAG = "UniKartFCMService";

    // ── Token refresh ─────────────────────────────────────────────────────────

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed");

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId != null) {
            FCMTokenManager.saveTokenToFirestore(userId, token);
        } else {
            Log.d(TAG, "onNewToken: no logged-in user, token will be saved on next login");
        }
    }

    // ── Message received ──────────────────────────────────────────────────────

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM message received");

        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");

        if (Constants.NOTIF_TYPE_CHAT.equals(type)) {
            handleChatMessage(data, remoteMessage.getNotification());
        } else if (Constants.NOTIF_TYPE_ORDER.equals(type)) {
            handleOrderMessage(data, remoteMessage.getNotification());
        } else if (remoteMessage.getNotification() != null) {
            // Generic fallback
            String title = remoteMessage.getNotification().getTitle();
            String body  = remoteMessage.getNotification().getBody();
            if (title != null && body != null) {
                NotificationHelper.showOrderNotification(this, title, body, false);
            }
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleChatMessage(Map<String, String> data,
                                   RemoteMessage.Notification notification) {
        // sender_name and chat_id come from the FCM data payload
        String senderName = safeGet(data, "sender_name", "New Message");
        String chatId     = safeGet(data, "chat_id", "");

        // title/body are in the data map (data-only message)
        String title   = safeGet(data, "title", senderName);
        String preview = safeGet(data, "body", "");

        // Fall back to notification block if present (shouldn't happen with data-only)
        if (notification != null) {
            if (notification.getTitle() != null) title   = notification.getTitle();
            if (notification.getBody()  != null) preview = notification.getBody();
        }

        NotificationHelper.showChatNotification(this, title, preview, chatId);
    }

    private void handleOrderMessage(Map<String, String> data,
                                    RemoteMessage.Notification notification) {
        boolean openSellerTab = "true".equals(data.get("open_seller_tab"));

        String title = safeGet(data, "title", "Order Update");
        String body  = safeGet(data, "body", "");

        if (notification != null) {
            if (notification.getTitle() != null) title = notification.getTitle();
            if (notification.getBody()  != null) body  = notification.getBody();
        }

        NotificationHelper.showOrderNotification(this, title, body, openSellerTab);
    }

    private static String safeGet(Map<String, String> map, String key, String fallback) {
        String v = map.get(key);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }
}
