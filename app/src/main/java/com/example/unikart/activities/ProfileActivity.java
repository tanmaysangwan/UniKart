package com.example.unikart.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.firebase.AdminRepository;
import com.example.unikart.firebase.AuthRepository;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.firebase.OrderRepository;
import com.example.unikart.firebase.ProductRepository;
import com.example.unikart.utils.CloudinaryUploader;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private SessionManager sessionManager;
    private AuthRepository authRepository;
    private AdminRepository adminRepository;
    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private View rootView;

    private TextView tvBoughtCount;
    private TextView tvSoldCount;
    private TextView tvRating;
    private TextView tvUserName;
    private TextView tvStudentId;
    private ImageView ivProfileAvatar;
    
    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private String uploadedImageUrl = "";

    // Edit Profile launcher
    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Refresh profile data after editing
                    loadUserData();
                    loadStats();
                }
            }
    );

    // Permission launchers
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    showError("Camera permission denied");
                }
            }
    );

    private final ActivityResultLauncher<String> galleryPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    launchGallery();
                } else {
                    showError("Storage permission denied");
                }
            }
    );

    // Camera launcher
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        uploadProfilePicture();
                    }
                }
            }
    );

    // Gallery launcher
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        selectedImageUri = uri;
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}
                        uploadProfilePicture();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager    = new SessionManager(this);
        authRepository    = new AuthRepository();
        adminRepository   = new AdminRepository();
        productRepository = new ProductRepository();
        orderRepository   = new OrderRepository();
        rootView          = findViewById(android.R.id.content);

        initViews();
        loadUserData();
        loadStats();
    }

    private void initViews() {
        MaterialCardView btnEditProfile = findViewById(R.id.btnEditProfile);
        MaterialCardView btnMyListings = findViewById(R.id.btnMyListings);
        MaterialCardView btnReviews    = findViewById(R.id.btnReviews);
        MaterialButton btnLogout   = findViewById(R.id.btnLogout);
        TextView btnDevTools       = findViewById(R.id.btnDevTools);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvStudentId = findViewById(R.id.tvStudentId);

        tvBoughtCount = findViewById(R.id.tvBoughtCount);
        tvSoldCount   = findViewById(R.id.tvSoldCount);
        tvRating      = findViewById(R.id.tvRating);

        // Edit Profile button
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditProfileActivity.class);
                editProfileLauncher.launch(intent);
            });
        }

        // Profile picture click
        if (ivProfileAvatar != null) {
            ivProfileAvatar.setOnClickListener(v -> showProfilePictureOptions());
        }

        btnMyListings.setOnClickListener(v -> {
            startActivity(new Intent(this, MyListingsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnReviews.setOnClickListener(v -> {
            // Navigate to Reviews screen
            Intent intent = new Intent(this, ReviewsActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        btnLogout.setOnClickListener(v -> {
            authRepository.logout();
            sessionManager.logout();
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        if (btnDevTools != null) {
            btnDevTools.setOnClickListener(v -> showDevToolsDialog());
        }

        setupBottomNavigation(bottomNavigation);
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        if (bottomNavigation == null) return;
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }
            if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddProductActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatsListActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            if (id == R.id.nav_orders) {
                startActivity(new Intent(this, OrdersActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            if (id == R.id.nav_profile) return true;
            return false;
        });
    }

    /** Finds the first TextView child of the nth stat column in the stats card. */
    private TextView findStatCount(int index) {
        try {
            // Stats card > LinearLayout > child LinearLayout[index] > first TextView
            View statsCard = findViewById(R.id.statsCard);
            if (statsCard == null) return null;
            LinearLayout row = (LinearLayout) ((com.google.android.material.card.MaterialCardView) statsCard)
                    .getChildAt(0);
            // Skip dividers — get actual LinearLayouts
            int found = 0;
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (child instanceof LinearLayout) {
                    if (found == index) {
                        return (TextView) ((LinearLayout) child).getChildAt(0);
                    }
                    found++;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "findStatCount failed for index " + index, e);
        }
        return null;
    }

    private void loadUserData() {
        String uid = sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;
        
        // Load from Firestore to get latest data
        FirebaseFirestore db = FirebaseManager.getInstance().getFirestore();
        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String studentId = doc.getString("studentId");
                        String profilePicUrl = doc.getString("profilePicture");
                        
                        // Update UI
                        if (tvUserName != null) {
                            tvUserName.setText(name != null && !name.isEmpty() ? name : "Student");
                        }
                        
                        TextView tvUniversity = findViewById(R.id.tvUniversity);
                        if (tvUniversity != null) {
                            tvUniversity.setText(email != null ? email : "");
                        }
                        
                        if (tvStudentId != null) {
                            tvStudentId.setText(studentId != null && !studentId.isEmpty() ? "ID: " + studentId : "");
                        }
                        
                        // Update session
                        if (name != null) sessionManager.saveUserName(name);
                        
                        // Load profile picture
                        if (ivProfileAvatar != null) {
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.ic_user_placeholder)
                                        .error(R.drawable.ic_user_placeholder)
                                        .circleCrop()
                                        .into(ivProfileAvatar);
                            } else {
                                // No profile picture - show user icon
                                ivProfileAvatar.setImageResource(R.drawable.ic_user_placeholder);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    // Fallback to session data
                    String name = sessionManager.getUserName();
                    String email = sessionManager.getUserEmail();
                    String studentId = sessionManager.getStudentId();
                    
                    if (tvUserName != null) {
                        tvUserName.setText(name != null && !name.isEmpty() ? name : "Student");
                    }
                    TextView tvUniversity = findViewById(R.id.tvUniversity);
                    if (tvUniversity != null) {
                        tvUniversity.setText(email != null ? email : "");
                    }
                    if (tvStudentId != null) {
                        tvStudentId.setText(studentId != null && !studentId.isEmpty() ? "ID: " + studentId : "");
                    }
                });
    }
    
    private void loadProfilePicture() {
        // This method is now integrated into loadUserData()
    }

    private void showProfilePictureOptions() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Update Profile Picture")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: checkCameraPermissionAndLaunch(); break;
                        case 1: checkGalleryPermissionAndLaunch(); break;
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkGalleryPermissionAndLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                launchGallery();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                launchGallery();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = File.createTempFile("profile_", ".jpg", getCacheDir());
            } catch (IOException e) {
                Log.e(TAG, "Error creating temp file", e);
                showError("Could not create temp file");
                return;
            }

            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraLauncher.launch(intent);
            }
        } else {
            showError("No camera app found");
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void uploadProfilePicture() {
        if (selectedImageUri == null) return;

        showInfo("Uploading profile picture...");

        CloudinaryUploader.upload(this, selectedImageUri, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    uploadedImageUrl = imageUrl;
                    saveProfilePictureToFirestore(imageUrl);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Upload failed: " + error);
                    showError("Upload failed: " + error);
                });
            }
        });
    }

    private void saveProfilePictureToFirestore(String imageUrl) {
        String uid = sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;

        FirebaseFirestore db = FirebaseManager.getInstance().getFirestore();
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePicture", imageUrl);

        db.collection(Constants.COLLECTION_USERS).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showInfo("Profile picture updated!");
                    loadProfilePicture();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save profile picture", e);
                    showError("Failed to save: " + e.getMessage());
                });
    }

    private void loadStats() {
        String uid = sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;

        FirebaseFirestore db = FirebaseManager.getInstance().getFirestore();

        // Bought count (completed orders as buyer)
        db.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("buyerId", uid)
                .whereIn("status", java.util.Arrays.asList(
                    Constants.ORDER_STATUS_COMPLETED,
                    Constants.ORDER_STATUS_RETURNED
                ))
                .get()
                .addOnSuccessListener(snap -> {
                    if (tvBoughtCount != null) tvBoughtCount.setText(String.valueOf(snap.size()));
                });

        // Sold count (completed orders as seller)
        db.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("sellerId", uid)
                .whereIn("status", java.util.Arrays.asList(
                    Constants.ORDER_STATUS_COMPLETED,
                    Constants.ORDER_STATUS_RETURNED
                ))
                .get()
                .addOnSuccessListener(snap -> {
                    if (tvSoldCount != null) tvSoldCount.setText(String.valueOf(snap.size()));
                });

        // Rating from Firestore user doc
        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    Double rating = doc.getDouble("rating");
                    Integer reviewCount = doc.getLong("reviewCount") != null
                            ? doc.getLong("reviewCount").intValue() : 0;
                    if (tvRating != null) {
                        tvRating.setText(rating != null && rating > 0
                                ? String.format(Locale.getDefault(), "%.1f", rating)
                                : "—");
                    }
                });
    }

    // ── Dev tools ─────────────────────────────────────────────────────────────

    private void showDevToolsDialog() {
        String[] options = {
                "Create Demo Seller Account",
                "Delete Campus Store Listings",
                "Clear All Products",
                "Project Info / Viva Mode",
                "Firebase Diagnostics",
                "Cancel"
        };
        new AlertDialog.Builder(this)
                .setTitle("Developer Tools")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: createDemoAccount(); break;
                        case 1: deleteSeedProducts(); break;
                        case 2: clearProducts(); break;
                        case 3: startActivity(new Intent(this, ProjectInfoActivity.class)); break;
                        case 4: startActivity(new Intent(this, DiagnosticsActivity.class)); break;
                    }
                })
                .show();
    }

    private void createDemoAccount() {
        showInfo("Creating demo account...");
        adminRepository.ensureDemoAccountExists(new AdminRepository.AdminCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(ProfileActivity.this)
                            .setTitle("Done")
                            .setMessage(message + "\n\nEmail: " + AdminRepository.DEMO_EMAIL
                                    + "\nPassword: " + AdminRepository.DEMO_PASSWORD)
                            .setPositiveButton("OK", null)
                            .show();
                    if (!authRepository.isUserLoggedIn()) {
                        sessionManager.logout();
                        Intent i = new Intent(ProfileActivity.this, WelcomeActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    }
                });
            }
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> showError(error));
            }
        });
    }

    private void deleteSeedProducts() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Campus Store Listings?")
                .setMessage("This will permanently delete all listings by \"Campus Store\" from the marketplace.")
                .setPositiveButton("Delete", (d, w) -> {
                    showInfo("Deleting Campus Store listings...");
                    adminRepository.deleteSeedProducts(new AdminRepository.AdminCallback() {
                        @Override
                        public void onSuccess(String msg) {
                            runOnUiThread(() -> {
                                showInfo(msg);
                                loadStats();
                            });
                        }
                        @Override
                        public void onFailure(String err) {
                            runOnUiThread(() -> showError(err));
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearProducts() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Products?")
                .setMessage("This will delete all products. Cannot be undone.")
                .setPositiveButton("Clear", (d, w) ->
                        adminRepository.clearAllProducts(new AdminRepository.AdminCallback() {
                            @Override public void onSuccess(String msg) { runOnUiThread(() -> showInfo(msg)); }
                            @Override public void onFailure(String err) { runOnUiThread(() -> showError(err)); }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showInfo(String msg) {
        Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG).show();
    }

    private void showError(String msg) {
        Snackbar snack = Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG);
        snack.setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark, null));
        snack.setTextColor(0xFFFFFFFF);
        snack.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
