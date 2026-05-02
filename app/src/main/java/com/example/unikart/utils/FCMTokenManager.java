package com.example.unikart.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages FCM token lifecycle:
 * - Fetches the current device token
 * - Saves it to the user's Firestore document so other users can send them notifications
 *
 * Call {@link #refreshAndSaveToken(String)} after a successful login and whenever
 * {@link com.example.unikart.services.UniKartFCMService#onNewToken(String)} fires.
 */
public class FCMTokenManager {

    private static final String TAG = "FCMTokenManager";
    private static final String FIELD_FCM_TOKEN = "fcmToken";

    private FCMTokenManager() {}

    /**
     * Fetches the current FCM registration token and saves it to Firestore
     * under {@code users/{userId}.fcmToken}.
     *
     * @param userId the authenticated user's UID
     */
    public static void refreshAndSaveToken(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "refreshAndSaveToken: userId is null, skipping");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token == null || token.isEmpty()) {
                        Log.w(TAG, "FCM token is null or empty");
                        return;
                    }
                    Log.d(TAG, "FCM token obtained, saving to Firestore");
                    saveTokenToFirestore(userId, token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get FCM token", e));
    }

    /**
     * Saves a known token directly (called from {@code onNewToken} in the FCM service).
     * Uses set+merge so it works even if the fcmToken field doesn't exist yet.
     */
    public static void saveTokenToFirestore(String userId, String token) {
        if (userId == null || userId.isEmpty() || token == null || token.isEmpty()) return;

        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_FCM_TOKEN, token);

        // Use set with merge=true — unlike update(), this works even if the field
        // or document doesn't exist yet
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(v -> Log.d(TAG, "FCM token saved for user: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
    }

    /**
     * Clears the FCM token from Firestore on logout so the user stops receiving
     * notifications on this device.
     *
     * @param userId the user's UID
     */
    public static void clearToken(String userId) {
        if (userId == null || userId.isEmpty()) return;

        Map<String, Object> update = new HashMap<>();
        update.put(FIELD_FCM_TOKEN, "");

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(update)
                .addOnSuccessListener(v -> Log.d(TAG, "FCM token cleared for user: " + userId))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to clear FCM token (non-critical)", e));
    }
}
