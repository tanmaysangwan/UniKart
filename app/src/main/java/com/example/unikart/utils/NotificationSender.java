package com.example.unikart.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends push notifications via the FCM HTTP v1 API using a service account.
 *
 * ONE-TIME SETUP:
 *   1. Firebase Console → Project Settings → Service Accounts → Generate new private key
 *   2. Rename the downloaded file to  service-account.json
 *   3. Place it at  app/src/main/assets/service-account.json
 *
 * Call {@link #init(Context)} once from Application.onCreate() before any sends.
 * All other methods are fire-and-forget; failures are logged but never crash the app.
 */
public class NotificationSender {

    private static final String TAG = "NotificationSender";

    private static final String FCM_SCOPE  = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String TOKEN_URL  = "https://oauth2.googleapis.com/token";
    // %s = Firebase project_id from the service account JSON
    private static final String FCM_V1_URL = "https://fcm.googleapis.com/v1/projects/%s/messages:send";
    private static final MediaType JSON_MT = MediaType.get("application/json; charset=utf-8");

    // Application context — set once via init()
    private static Context appCtx = null;

    // Cached OAuth2 access token
    private static String cachedAccessToken = null;
    private static long   tokenExpiryMs     = 0;

    // Service-account fields parsed from assets/service-account.json
    private static String  saProjectId   = null;
    private static String  saClientEmail = null;
    private static String  saPrivateKey  = null;  // base64 only, no PEM headers
    private static boolean saLoaded      = false;
    private static boolean saLoadFailed  = false;

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private NotificationSender() {}

    // ── Initialisation ────────────────────────────────────────────────────────

    /**
     * Call once from {@code UniKartApp.onCreate()}.
     * Stores the application context and eagerly loads the service account.
     */
    public static void init(Context context) {
        appCtx = context.getApplicationContext();
        loadServiceAccount();
    }

    // ── Public send methods ───────────────────────────────────────────────────

    /**
     * Sends a "new chat message" push to {@code recipientUserId}.
     * Looks up their FCM token from Firestore, then calls FCM v1.
     */
    public static void sendChatNotification(String recipientUserId,
                                            String senderName,
                                            String messagePreview,
                                            String chatId) {
        if (appCtx == null) { Log.e(TAG, "Not initialised — call NotificationSender.init() first"); return; }
        fetchFcmTokenThenSend(recipientUserId, fcmToken -> {
            try {
                String preview = messagePreview != null && messagePreview.length() > 100
                        ? messagePreview.substring(0, 100) + "…" : messagePreview;
                JSONObject data = new JSONObject();
                data.put("type", Constants.NOTIF_TYPE_CHAT);
                data.put("chat_id", chatId != null ? chatId : "");
                data.put("sender_name", senderName != null ? senderName : "");

                postFcmMessage(fcmToken,
                        senderName != null ? senderName : "New message",
                        preview != null ? preview : "",
                        Constants.CHANNEL_ID_CHAT,
                        data);
            } catch (Exception e) {
                Log.e(TAG, "sendChatNotification build failed", e);
            }
        });
    }

    /**
     * Sends an order-related push to {@code recipientUserId}.
     */
    public static void sendOrderNotification(String recipientUserId,
                                             String title,
                                             String body,
                                             boolean openSellerTab) {
        if (appCtx == null) { Log.e(TAG, "Not initialised — call NotificationSender.init() first"); return; }
        fetchFcmTokenThenSend(recipientUserId, fcmToken -> {
            try {
                JSONObject data = new JSONObject();
                data.put("type", Constants.NOTIF_TYPE_ORDER);
                data.put("open_seller_tab", String.valueOf(openSellerTab));

                postFcmMessage(fcmToken,
                        title != null ? title : "Order Update",
                        body  != null ? body  : "",
                        Constants.CHANNEL_ID_ORDERS,
                        data);
            } catch (Exception e) {
                Log.e(TAG, "sendOrderNotification build failed", e);
            }
        });
    }

    // ── Firestore token lookup ────────────────────────────────────────────────

    private interface FcmTokenCallback { void onToken(String fcmToken); }

    private static void fetchFcmTokenThenSend(String recipientUserId, FcmTokenCallback cb) {
        if (recipientUserId == null || recipientUserId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(recipientUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.w(TAG, "Recipient doc not found: " + recipientUserId);
                        return;
                    }
                    String fcmToken = doc.getString("fcmToken");
                    if (fcmToken == null || fcmToken.isEmpty()) {
                        Log.d(TAG, "Recipient " + recipientUserId + " has no FCM token — skipping push");
                        return;
                    }
                    cb.onToken(fcmToken);
                })
                .addOnFailureListener(e -> Log.e(TAG, "fetchFcmToken failed for " + recipientUserId, e));
    }

    // ── FCM v1 send ───────────────────────────────────────────────────────────

    private static void postFcmMessage(String fcmToken, String title, String body,
                                       String channelId, JSONObject data) {
        getAccessToken(accessToken -> {
            if (accessToken == null) {
                Log.e(TAG, "No OAuth2 token — FCM send skipped. Check service-account.json in assets/");
                return;
            }
            try {
                // ── Data-only message (no "notification" block) ──────────────
                // This ensures onMessageReceived() fires in ALL app states
                // (foreground, background, AND killed), giving us full control
                // over the notification appearance and deep-link PendingIntent.
                // The title/body are carried in the data map instead.
                final JSONObject payload_data = (data != null) ? data : new JSONObject();
                payload_data.put("title", title);
                payload_data.put("body", body);
                payload_data.put("channel_id", channelId);

                JSONObject android = new JSONObject();
                android.put("priority", "high");

                JSONObject message = new JSONObject();
                message.put("token", fcmToken);
                message.put("data", payload_data);
                message.put("android", android);

                JSONObject payload = new JSONObject();
                payload.put("message", message);

                String url = String.format(FCM_V1_URL, saProjectId);
                RequestBody reqBody = RequestBody.create(payload.toString(), JSON_MT);
                Request req = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .post(reqBody)
                        .build();

                HTTP.newCall(req).enqueue(new Callback() {
                    @Override public void onFailure(Call call, java.io.IOException e) {
                        Log.e(TAG, "FCM v1 HTTP failed", e);
                    }
                    @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                        String rb = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            Log.d(TAG, "FCM v1 sent OK — token prefix: "
                                    + fcmToken.substring(0, Math.min(20, fcmToken.length())) + "…");
                        } else {
                            Log.e(TAG, "FCM v1 error " + response.code() + ": " + rb);
                            if (response.code() == 401) {
                                cachedAccessToken = null;
                                tokenExpiryMs = 0;
                            }
                        }
                        response.close();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "postFcmMessage failed", e);
            }
        });
    }

    // ── OAuth2 access token via JWT ───────────────────────────────────────────

    private interface AccessTokenCallback { void onToken(String token); }

    private static void getAccessToken(AccessTokenCallback cb) {
        // Return cached token if still valid (60s buffer before expiry)
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryMs - 60_000L) {
            Log.d(TAG, "Using cached OAuth2 token");
            cb.onToken(cachedAccessToken);
            return;
        }

        if (saLoadFailed) {
            Log.e(TAG, "Service account failed to load — check assets/service-account.json exists");
            cb.onToken(null);
            return;
        }
        if (!saLoaded) {
            Log.e(TAG, "Service account not loaded yet — was NotificationSender.init() called?");
            cb.onToken(null);
            return;
        }

        Log.d(TAG, "Requesting new OAuth2 access token for: " + saClientEmail);
        try {
            String jwt = buildJwt();
            String formBody = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer"
                    + "&assertion=" + jwt;
            RequestBody body = RequestBody.create(formBody,
                    MediaType.get("application/x-www-form-urlencoded"));
            Request req = new Request.Builder().url(TOKEN_URL).post(body).build();

            HTTP.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, java.io.IOException e) {
                    Log.e(TAG, "OAuth2 token request network failed", e);
                    cb.onToken(null);
                }
                @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                    try {
                        String rb = response.body() != null ? response.body().string() : "";
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "OAuth2 token HTTP error " + response.code() + ": " + rb);
                            cb.onToken(null);
                            return;
                        }
                        JSONObject json = new JSONObject(rb);
                        cachedAccessToken = json.getString("access_token");
                        int expiresIn = json.optInt("expires_in", 3600);
                        tokenExpiryMs = System.currentTimeMillis() + expiresIn * 1000L;
                        Log.d(TAG, "OAuth2 token obtained successfully, valid for " + expiresIn + "s");
                        cb.onToken(cachedAccessToken);
                    } catch (Exception e) {
                        Log.e(TAG, "OAuth2 token parse failed", e);
                        cb.onToken(null);
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "buildJwt failed", e);
            cb.onToken(null);
        }
    }

    // ── JWT builder (RS256) ───────────────────────────────────────────────────

    private static String buildJwt() throws Exception {
        long nowSec = System.currentTimeMillis() / 1000L;

        JSONObject header = new JSONObject();
        header.put("alg", "RS256");
        header.put("typ", "JWT");

        JSONObject claims = new JSONObject();
        claims.put("iss", saClientEmail);
        claims.put("scope", FCM_SCOPE);
        claims.put("aud", TOKEN_URL);
        claims.put("iat", nowSec);
        claims.put("exp", nowSec + 3600L);

        String encodedHeader = b64url(header.toString().getBytes(StandardCharsets.UTF_8));
        String encodedClaims = b64url(claims.toString().getBytes(StandardCharsets.UTF_8));
        String signingInput  = encodedHeader + "." + encodedClaims;

        PrivateKey privateKey = loadRsaPrivateKey(saPrivateKey);
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
        byte[] signature = sig.sign();

        return signingInput + "." + b64url(signature);
    }

    private static PrivateKey loadRsaPrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.decode(base64Key, Base64.DEFAULT);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private static String b64url(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    // ── Service account loader ────────────────────────────────────────────────

    private static synchronized void loadServiceAccount() {
        if (saLoaded || saLoadFailed || appCtx == null) return;
        try {
            InputStream is = appCtx.getAssets().open("service-account.json");
            // Read fully — is.available() is unreliable for assets
            byte[] buf = new byte[4096];
            StringBuilder sb = new StringBuilder();
            int bytesRead;
            while ((bytesRead = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
            }
            is.close();

            String raw = sb.toString().trim();
            Log.d(TAG, "service-account.json raw length: " + raw.length() + " chars");

            JSONObject json = new JSONObject(raw);
            saProjectId   = json.getString("project_id");
            saClientEmail = json.getString("client_email");

            // Strip PEM headers and all whitespace — keep only the base64 body
            String rawKey = json.getString("private_key");
            saPrivateKey  = rawKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            saLoaded = true;
            Log.d(TAG, "service-account.json loaded OK — project: " + saProjectId
                    + ", email: " + saClientEmail
                    + ", key length: " + saPrivateKey.length());
        } catch (Exception e) {
            saLoadFailed = true;
            Log.e(TAG, "Could not load assets/service-account.json. " +
                    "Go to Firebase Console → Project Settings → Service Accounts → " +
                    "Generate new private key, rename to service-account.json, " +
                    "and place it in app/src/main/assets/", e);
        }
    }
}
