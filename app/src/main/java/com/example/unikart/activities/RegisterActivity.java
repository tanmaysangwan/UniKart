package com.example.unikart.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unikart.R;
import com.example.unikart.firebase.AuthRepository;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etStudentId;
    private TextInputEditText etPassword;
    private MaterialButton btnCreateAccount;
    private ProgressBar progressBar;
    private View rootView;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository();
        initViews();
        setupListeners();
    }

    private void initViews() {
        rootView = findViewById(android.R.id.content);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        progressBar = findViewById(R.id.progressBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        TextView tvLoginLink = findViewById(R.id.tvLoginLink);
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupListeners() {
        btnCreateAccount.setOnClickListener(v -> {
            if (validateForm()) {
                registerUser();
            }
        });
    }

    private boolean validateForm() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String studentId = etStudentId.getText() != null ? etStudentId.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            showError("Please enter your full name");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            showError("Please enter your university email");
            return false;
        }
        if (!Constants.isValidUniversityEmail(email)) {
            showError("Email domain not allowed. Allowed: " + Constants.ALLOWED_EMAIL_DOMAINS.toString());
            return false;
        }
        if (TextUtils.isEmpty(studentId)) {
            showError("Please enter your student ID");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Please enter a password");
            return false;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    private void registerUser() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        Log.d(TAG, "Starting registration for: " + email);
        setLoading(true);

        authRepository.registerUser(name, email, studentId, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Registration success: " + message);
                setLoading(false);

                if (authRepository.getCurrentUser() != null) {
                    String uid = authRepository.getCurrentUser().getUid();
                    sessionManager.createLoginSession(uid, name, email, studentId);

                    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }, 800);
                } else {
                    showError("Registration completed but session failed. Please log in.");
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Registration failed: " + error);
                setLoading(false);
                showError(error);
            }
        });
    }

    private void showError(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark, null));
        snackbar.setTextColor(getResources().getColor(android.R.color.white, null));
        snackbar.show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnCreateAccount.setEnabled(!loading);
        etFullName.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etStudentId.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
