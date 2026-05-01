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
import com.example.unikart.models.User;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class  LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private View rootView;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        authRepository = new AuthRepository();
        initViews();
        setupListeners();
    }

    private void initViews() {
        rootView = findViewById(android.R.id.content);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnVerifyLogin);
        progressBar = findViewById(R.id.progressBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(email)) {
                showError("Please enter your email");
                return;
            }
            if (!Constants.isValidUniversityEmail(email)) {
                showError("Email domain not allowed. Allowed: " + Constants.ALLOWED_EMAIL_DOMAINS.toString());
                return;
            }
            if (TextUtils.isEmpty(password)) {
                showError("Please enter your password");
                return;
            }

            loginUser(email, password);
        });

        TextView tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser(String email, String password) {
        Log.d(TAG, "Starting login for: " + email);
        setLoading(true);

        authRepository.loginUser(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Login auth success");
                if (authRepository.getCurrentUser() != null) {
                    String uid = authRepository.getCurrentUser().getUid();

                    authRepository.getUserData(uid, new AuthRepository.UserCallback() {
                        @Override
                        public void onSuccess(User user) {
                            Log.d(TAG, "User data fetched: " + user.getName());
                            setLoading(false);
                            sessionManager.createLoginSession(
                                    user.getUid() != null ? user.getUid() : uid,
                                    user.getName() != null ? user.getName() : "Student",
                                    user.getEmail() != null ? user.getEmail() : email,
                                    user.getStudentId() != null ? user.getStudentId() : ""
                            );

                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "getUserData failed: " + error);
                            setLoading(false);
                            showError(error);
                        }
                    });
                } else {
                    setLoading(false);
                    showError("Login failed — no user returned");
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Login failed: " + error);
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
        btnLogin.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
