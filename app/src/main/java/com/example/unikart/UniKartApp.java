package com.example.unikart;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

public class UniKartApp extends Application {

    private static final String TAG = "UniKartApp";

    @Override
    public void onCreate() {
        super.onCreate();
        initFirebase();
    }

    private void initFirebase() {
        try {
            // FirebaseApp auto-initializes from google-services.json
            // This call is safe — if already initialized it returns the existing instance
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "FirebaseApp initialized manually");
            } else {
                Log.d(TAG, "FirebaseApp already initialized");
            }

            FirebaseApp app = FirebaseApp.getInstance();
            FirebaseOptions options = app.getOptions();

            Log.d(TAG, "=== Firebase Config Diagnostics ===");
            Log.d(TAG, "Project ID     : " + options.getProjectId());
            Log.d(TAG, "App ID         : " + options.getApplicationId());
            Log.d(TAG, "API Key        : " + maskKey(options.getApiKey()));
            Log.d(TAG, "Storage Bucket : " + options.getStorageBucket());
            Log.d(TAG, "Package Name   : " + getPackageName());
            Log.d(TAG, "Auth instance  : " + (FirebaseAuth.getInstance() != null ? "OK" : "NULL"));
            Log.d(TAG, "===================================");

            // Detect placeholder config
            String apiKey = options.getApiKey();
            if (apiKey == null || apiKey.contains("Dummy") || apiKey.contains("Replace") || apiKey.length() < 30) {
                Log.e(TAG, "FATAL: google-services.json contains a placeholder API key.");
                Log.e(TAG, "Download the real google-services.json from Firebase Console and place it in app/");
            }

        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage(), e);
        }
    }

    private String maskKey(String key) {
        if (key == null) return "null";
        if (key.length() <= 8) return "****";
        return key.substring(0, 8) + "..." + key.substring(key.length() - 4);
    }
}
