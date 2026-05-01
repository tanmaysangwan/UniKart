package com.example.unikart.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unikart.R;
import com.example.unikart.firebase.FirebaseManager;
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
            Intent intent;
            // Check both Firebase Auth and Session Manager
            if (firebaseManager.isUserLoggedIn() && sessionManager.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                // Clear session if Firebase auth is not valid
                sessionManager.logout();
                intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
