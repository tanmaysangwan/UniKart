package com.example.unikart.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.unikart.R;
import com.example.unikart.activities.ChatActivity;
import com.example.unikart.activities.OrdersActivity;
import com.example.unikart.activities.SplashActivity;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds and posts local notifications with proper deep-link PendingIntents.
 *
 * Deep-link strategy:
 *  - App in foreground/background → PendingIntent goes directly to ChatActivity / OrdersActivity.
 *  - App killed → FCM system tray tap launches SplashActivity (the launcher).
 *    SplashActivity reads the notification extras from its intent and forwards
 *    the user to the right screen after the auth check.
 *
 * Both cases use the same intent extras so SplashActivity and the target activities
 * share one code path.
 */
public class NotificationHelper {

    // Extra keys written into every notification intent — read by SplashActivity
    public static final String EXTRA_NOTIF_TYPE    = "notif_type";
    public static final String EXTRA_CHAT_ID       = "chat_id";
    public static final String EXTRA_SELLER_NAME   = "seller_name";
    public static final String EXTRA_OPEN_SELLER_TAB = "open_seller_tab";

    private static final AtomicInteger notifIdCounter = new AtomicInteger(1000);

    private NotificationHelper() {}

    // ── Channel setup ─────────────────────────────────────────────────────────

    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel chatChannel = new NotificationChannel(
                Constants.CHANNEL_ID_CHAT,
                Constants.CHANNEL_NAME_CHAT,
                NotificationManager.IMPORTANCE_HIGH
        );
        chatChannel.setDescription("Notifications for new chat messages");
        nm.createNotificationChannel(chatChannel);

        NotificationChannel ordersChannel = new NotificationChannel(
                Constants.CHANNEL_ID_ORDERS,
                Constants.CHANNEL_NAME_ORDERS,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        ordersChannel.setDescription("Notifications for order and transaction updates");
        nm.createNotificationChannel(ordersChannel);
    }

    // ── Show notifications ────────────────────────────────────────────────────

    /**
     * Shows a chat notification that opens ChatActivity directly on tap.
     *
     * @param senderName display name of the sender (used as title and chat header)
     * @param preview    message preview text
     * @param chatId     Firestore chat document ID
     */
    public static void showChatNotification(Context context, String senderName,
                                            String preview, String chatId) {
        // Direct intent → ChatActivity (app foreground/background)
        Intent chatIntent = buildChatIntent(context, chatId, senderName);

        // Splash intent → SplashActivity (app killed — system tray tap)
        // SplashActivity will forward to ChatActivity after auth check
        Intent splashIntent = buildSplashIntent(context, chatId, senderName);

        // Use the direct intent when the app process is alive, splash as fallback
        PendingIntent pi = PendingIntent.getActivity(
                context,
                notifIdCounter.get(),
                chatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.CHANNEL_ID_CHAT)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(senderName)
                .setContentText(preview)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(preview))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        postNotification(context, builder);
    }

    /**
     * Shows an order notification that opens OrdersActivity on the correct tab.
     *
     * @param openSellerTab true → "Selling/Lending" tab; false → "Buying/Renting" tab
     */
    public static void showOrderNotification(Context context, String title,
                                             String body, boolean openSellerTab) {
        Intent orderIntent = buildOrderIntent(context, openSellerTab);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                notifIdCounter.get(),
                orderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.CHANNEL_ID_ORDERS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi);

        postNotification(context, builder);
    }

    // ── Intent builders (also used by SplashActivity for forwarding) ──────────

    /** Intent that opens ChatActivity directly for the given chatId. */
    public static Intent buildChatIntent(Context context, String chatId, String senderName) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_NOTIF_TYPE, Constants.NOTIF_TYPE_CHAT);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        // seller_name doubles as the chat title when opened from a notification
        intent.putExtra("seller_name", senderName != null ? senderName : "Chat");
        intent.putExtra("chat_id", chatId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    /** Intent that opens OrdersActivity on the correct tab. */
    public static Intent buildOrderIntent(Context context, boolean openSellerTab) {
        Intent intent = new Intent(context, OrdersActivity.class);
        intent.putExtra(EXTRA_NOTIF_TYPE, Constants.NOTIF_TYPE_ORDER);
        intent.putExtra(EXTRA_OPEN_SELLER_TAB, openSellerTab);
        intent.putExtra("tab", openSellerTab ? 1 : 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    /**
     * Intent targeting SplashActivity with notification extras embedded.
     * Used when the app is killed — SplashActivity reads these and forwards.
     */
    public static Intent buildSplashIntent(Context context, String chatId, String senderName) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.putExtra(EXTRA_NOTIF_TYPE, Constants.NOTIF_TYPE_CHAT);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        intent.putExtra(EXTRA_SELLER_NAME, senderName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void postNotification(Context context, NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat nm = NotificationManagerCompat.from(context);
            nm.notify(notifIdCounter.getAndIncrement(), builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted on Android 13+
        }
    }
}
