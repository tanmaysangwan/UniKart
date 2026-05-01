package com.example.unikart.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.InputStream;

public class DiagnosticsActivity extends AppCompatActivity {

    private static final String TAG = "UniKartDiag";
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF0D1117);
        scrollView.setPadding(0, 0, 0, 40);

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(32, 80, 32, 40);
        scrollView.addView(container);
        setContentView(scrollView);

        addTitle("UniKart Firebase Diagnostics");
        addDivider();
        runDiagnostics();
        addDivider();
        addShaSection();
        addDivider();
        addFixInstructions();
    }

    private void runDiagnostics() {
        // 1. Package name
        addRow("Package Name", getPackageName(), true);

        // 2. Firebase App
        boolean firebaseInit = !FirebaseApp.getApps(this).isEmpty();
        addRow("FirebaseApp Initialized", firebaseInit ? "YES" : "NO — CRITICAL", firebaseInit);

        if (!firebaseInit) {
            addError("FirebaseApp is not initialized. google-services.json is missing or invalid.");
            return;
        }

        FirebaseApp app = FirebaseApp.getInstance();
        FirebaseOptions options = app.getOptions();

        // 3. Project ID
        String projectId = options.getProjectId();
        boolean projectOk = projectId != null && !projectId.equals("unikart-project") && !projectId.contains("placeholder");
        addRow("Project ID", projectId != null ? projectId : "null", projectOk);

        // 4. App ID
        String appId = options.getApplicationId();
        boolean appIdOk = appId != null && !appId.contains("abcdef") && !appId.contains("1234567890");
        addRow("App ID (mobilesdk_app_id)", appId != null ? appId : "null", appIdOk);

        // 5. API Key
        String apiKey = options.getApiKey();
        boolean apiKeyOk = apiKey != null
                && !apiKey.contains("Dummy")
                && !apiKey.contains("Replace")
                && apiKey.startsWith("AIzaSy")
                && apiKey.length() >= 39;
        String maskedKey = apiKey != null
                ? (apiKey.length() > 12 ? apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4) : "TOO SHORT")
                : "null";
        addRow("API Key", maskedKey, apiKeyOk);

        if (!apiKeyOk) {
            addError("API key is a placeholder. This is why you get 'API key not valid'.\nReplace app/google-services.json with the real file from Firebase Console.");
        }

        // 6. Storage bucket
        String bucket = options.getStorageBucket();
        boolean bucketOk = bucket != null && !bucket.contains("unikart-project.appspot.com") || (bucket != null && !bucket.equals("unikart-project.appspot.com"));
        addRow("Storage Bucket", bucket != null ? bucket : "null", bucket != null && !bucket.isEmpty());

        // 7. Auth instance
        FirebaseAuth auth = FirebaseAuth.getInstance();
        addRow("FirebaseAuth Instance", auth != null ? "OK" : "NULL", auth != null);

        // 8. Current user
        String currentUser = auth != null && auth.getCurrentUser() != null
                ? auth.getCurrentUser().getEmail()
                : "Not logged in";
        addRow("Current User", currentUser, true);

        // 9. Firestore
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            addRow("Firestore Instance", db != null ? "OK" : "NULL", db != null);
        } catch (Exception e) {
            addRow("Firestore Instance", "ERROR: " + e.getMessage(), false);
        }

        // 10. Overall verdict
        addDivider();
        if (!apiKeyOk || !projectOk || !appIdOk) {
            addError("VERDICT: google-services.json is a PLACEHOLDER.\nDownload the real file from Firebase Console and replace app/google-services.json");
        } else {
            addSuccess("VERDICT: Firebase config looks valid. If auth still fails, check:\n1. Email/Password enabled in Firebase Console\n2. SHA-1 added to Firebase project (see below)");
        }
    }

    private void addShaSection() {
        addSectionTitle("SHA Certificate Fingerprints");
        addInfo("For physical device auth, add SHA-1 and SHA-256 to Firebase Console:\nProject Settings > Your apps > Add fingerprint");
        addInfo("Run this command in your project root to get SHA keys:\n\n./gradlew signingReport\n\nOr use Android Studio:\nGradle panel > app > Tasks > android > signingReport");

        // Try to read the debug keystore SHA
        addInfo("Debug keystore is usually at:\n~/.android/debug.keystore\n\nDefault password: android\nDefault alias: androiddebugkey");

        Button copyCmd = new Button(this);
        copyCmd.setText("Copy keytool command");
        copyCmd.setBackgroundColor(0xFF1F6FEB);
        copyCmd.setTextColor(0xFFFFFFFF);
        copyCmd.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 16;
        copyCmd.setLayoutParams(lp);
        copyCmd.setOnClickListener(v -> {
            String cmd = "keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android";
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("keytool", cmd));
            Toast.makeText(this, "Command copied to clipboard", Toast.LENGTH_SHORT).show();
        });
        container.addView(copyCmd);
    }

    private void addFixInstructions() {
        addSectionTitle("How to Fix");
        addInfo("1. Go to https://console.firebase.google.com/");
        addInfo("2. Open your project (or create one named UniKart)");
        addInfo("3. Project Settings (gear icon) > Your apps");
        addInfo("4. If no Android app: Add app\n   Package: com.example.unikart");
        addInfo("5. Download google-services.json");
        addInfo("6. Replace app/google-services.json with the downloaded file");
        addInfo("7. In Firebase Console:\n   Authentication > Sign-in method > Email/Password > Enable\n   Firestore Database > Create database > Test mode\n   Storage > Get started > Test mode");
        addInfo("8. Add SHA-1 fingerprint:\n   Project Settings > Your apps > Add fingerprint\n   (Run ./gradlew signingReport to get it)");
        addInfo("9. Sync Gradle and rebuild");
    }

    // ---- UI helpers ----

    private void addTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFE6EDF3);
        tv.setTextSize(20);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 0, 0, 8);
        container.addView(tv);
    }

    private void addSectionTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF58A6FF);
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 24, 0, 8);
        container.addView(tv);
    }

    private void addRow(String label, String value, boolean ok) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 6);

        TextView status = new TextView(this);
        status.setText(ok ? "✓" : "✗");
        status.setTextColor(ok ? 0xFF3FB950 : 0xFFF85149);
        status.setTextSize(14);
        status.setMinWidth(40);
        row.addView(status);

        TextView labelTv = new TextView(this);
        labelTv.setText(label + ": ");
        labelTv.setTextColor(0xFF8B949E);
        labelTv.setTextSize(13);
        labelTv.setMinWidth(300);
        row.addView(labelTv);

        TextView valueTv = new TextView(this);
        valueTv.setText(value);
        valueTv.setTextColor(ok ? 0xFFE6EDF3 : 0xFFF85149);
        valueTv.setTextSize(13);
        row.addView(valueTv);

        container.addView(row);
    }

    private void addError(String text) {
        TextView tv = new TextView(this);
        tv.setText("⚠ " + text);
        tv.setTextColor(0xFFF85149);
        tv.setTextSize(13);
        tv.setBackgroundColor(0xFF3D1F1F);
        tv.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 8;
        lp.bottomMargin = 8;
        tv.setLayoutParams(lp);
        container.addView(tv);
    }

    private void addSuccess(String text) {
        TextView tv = new TextView(this);
        tv.setText("✓ " + text);
        tv.setTextColor(0xFF3FB950);
        tv.setTextSize(13);
        tv.setBackgroundColor(0xFF1A3A1F);
        tv.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 8;
        lp.bottomMargin = 8;
        tv.setLayoutParams(lp);
        container.addView(tv);
    }

    private void addInfo(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFADBBC4);
        tv.setTextSize(13);
        tv.setPadding(0, 6, 0, 6);
        container.addView(tv);
    }

    private void addDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(0xFF30363D);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        lp.topMargin = 16;
        lp.bottomMargin = 16;
        divider.setLayoutParams(lp);
        container.addView(divider);
    }
}
