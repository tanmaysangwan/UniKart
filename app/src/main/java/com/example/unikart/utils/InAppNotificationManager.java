package com.example.unikart.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.unikart.R;
import com.example.unikart.activities.ChatActivity;
import com.example.unikart.activities.OrdersActivity;

/**
 * Manages in-app notification banners that appear at the top of the screen
 * when the app is in the foreground.
 */
public class InAppNotificationManager {

    private static final String TAG = "InAppNotificationMgr";
    private static final long BANNER_DURATION_MS = 5000; // 5 seconds

    private InAppNotificationManager() {}

    /**
     * Shows an in-app notification banner for a chat message
     */
    public static void showChatNotification(Activity activity, String senderName, 
                                           String messagePreview, String chatId) {
        if (activity == null || activity.isFinishing()) return;

        activity.runOnUiThread(() -> {
            View banner = createBanner(activity, senderName, messagePreview, () -> {
                // On click - open chat
                Intent intent = NotificationHelper.buildChatIntent(activity, chatId, senderName);
                activity.startActivity(intent);
            });
            showBanner(activity, banner);
        });
    }

    /**
     * Shows an in-app notification banner for an order update
     */
    public static void showOrderNotification(Activity activity, String title, 
                                            String body, boolean openSellerTab) {
        if (activity == null || activity.isFinishing()) return;

        activity.runOnUiThread(() -> {
            View banner = createBanner(activity, title, body, () -> {
                // On click - open orders
                Intent intent = NotificationHelper.buildOrderIntent(activity, openSellerTab);
                activity.startActivity(intent);
            });
            showBanner(activity, banner);
        });
    }

    /**
     * Creates the notification banner view
     */
    private static View createBanner(Activity activity, String title, String body, 
                                    Runnable onClickAction) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View banner = inflater.inflate(R.layout.in_app_notification_banner, null);

        TextView tvTitle = banner.findViewById(R.id.tvNotificationTitle);
        TextView tvBody = banner.findViewById(R.id.tvNotificationBody);
        ImageView btnClose = banner.findViewById(R.id.btnCloseNotification);

        tvTitle.setText(title != null ? title : "Notification");
        tvBody.setText(body != null ? body : "");

        // Click to open
        banner.setOnClickListener(v -> {
            if (onClickAction != null) {
                onClickAction.run();
            }
            dismissBanner(activity, banner);
        });

        // Close button
        btnClose.setOnClickListener(v -> dismissBanner(activity, banner));

        return banner;
    }

    /**
     * Shows the banner with slide-down animation
     */
    private static void showBanner(Activity activity, View banner) {
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        // Remove any existing banners first
        View existingBanner = rootView.findViewWithTag("in_app_notification");
        if (existingBanner != null) {
            rootView.removeView(existingBanner);
        }

        // Add banner to top of screen with margins
        banner.setTag("in_app_notification");
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = getStatusBarHeight(activity) + dpToPx(activity, 8);
        params.leftMargin = dpToPx(activity, 0);
        params.rightMargin = dpToPx(activity, 0);
        
        rootView.addView(banner, params);

        // Slide down animation
        TranslateAnimation slideDown = new TranslateAnimation(
                0, 0, -300, 0
        );
        slideDown.setDuration(350);
        slideDown.setFillAfter(true);
        banner.startAnimation(slideDown);

        // Auto-dismiss after duration
        banner.postDelayed(() -> dismissBanner(activity, banner), BANNER_DURATION_MS);
    }

    /**
     * Dismisses the banner with slide-up animation
     */
    private static void dismissBanner(Activity activity, View banner) {
        if (banner == null || banner.getParent() == null) return;

        // Slide up animation
        TranslateAnimation slideUp = new TranslateAnimation(
                0, 0, 0, -300
        );
        slideUp.setDuration(350);
        slideUp.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                ViewGroup parent = (ViewGroup) banner.getParent();
                if (parent != null) {
                    parent.removeView(banner);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        banner.startAnimation(slideUp);
    }

    /**
     * Converts dp to pixels
     */
    private static int dpToPx(Activity activity, int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Gets the status bar height to position banner correctly
     */
    private static int getStatusBarHeight(Activity activity) {
        int resourceId = activity.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return activity.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
