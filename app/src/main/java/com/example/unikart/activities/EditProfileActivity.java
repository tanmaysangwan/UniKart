package com.example.unikart.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.utils.CloudinaryUploader;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    private ImageView ivProfilePhoto;
    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etStudentId;
    private TextInputEditText etPhone;
    private TextInputEditText etDepartment;
    private TextInputEditText etYear;
    private TextInputEditText etMeetupLocation;
    private TextInputEditText etBio;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private Uri selectedImageUri;
    private String currentProfilePictureUrl;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);
        firestore = FirebaseManager.getInstance().getFirestore();
        currentUserId = sessionManager.getUserId();

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupImagePickers();
        loadUserData();
    }

    private void initViews() {
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etStudentId = findViewById(R.id.etStudentId);
        etPhone = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);
        etYear = findViewById(R.id.etYear);
        etMeetupLocation = findViewById(R.id.etMeetupLocation);
        etBio = findViewById(R.id.etBio);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        View btnBack = findViewById(R.id.btnBack);
        View btnChangePhoto = findViewById(R.id.btnChangePhoto);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> showPhotoOptions());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfile());
        }
    }

    private void setupImagePickers() {
        // Gallery launcher
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            displaySelectedImage(selectedImageUri);
                        }
                    }
                }
        );

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (cameraImageUri != null) {
                            selectedImageUri = cameraImageUri;
                            displaySelectedImage(cameraImageUri);
                        }
                    }
                }
        );
    }

    private void showPhotoOptions() {
        String[] options = {"Choose from Gallery", "Take Photo", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Change Profile Photo")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openGallery();
                            break;
                        case 1:
                            openCamera();
                            break;
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = File.createTempFile("profile_", ".jpg", getCacheDir());
            } catch (Exception e) {
                Log.e(TAG, "Error creating temp file", e);
                Toast.makeText(this, "Could not create temp file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                cameraImageUri = androidx.core.content.FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraLauncher.launch(intent);
            }
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void displaySelectedImage(Uri imageUri) {
        Glide.with(this)
                .load(imageUri)
                .placeholder(R.drawable.ic_user_placeholder)
                .error(R.drawable.ic_user_placeholder)
                .circleCrop()
                .into(ivProfilePhoto);
    }

    private void loadUserData() {
        showLoading(true);

        firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    showLoading(false);
                    if (doc.exists()) {
                        // Load data into fields
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String studentId = doc.getString("studentId");
                        String phone = doc.getString("phone");
                        String department = doc.getString("department");
                        String year = doc.getString("year");
                        String meetupLocation = doc.getString("meetupLocation");
                        String bio = doc.getString("bio");
                        currentProfilePictureUrl = doc.getString("profilePicture");

                        if (etFullName != null) etFullName.setText(name);
                        if (etEmail != null) etEmail.setText(email);
                        if (etStudentId != null) etStudentId.setText(studentId);
                        if (etPhone != null) etPhone.setText(phone);
                        if (etDepartment != null) etDepartment.setText(department);
                        if (etYear != null) etYear.setText(year);
                        if (etMeetupLocation != null) etMeetupLocation.setText(meetupLocation);
                        if (etBio != null) etBio.setText(bio);

                        // Load profile photo
                        if (ivProfilePhoto != null) {
                            if (currentProfilePictureUrl != null && !currentProfilePictureUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(currentProfilePictureUrl)
                                        .placeholder(R.drawable.ic_user_placeholder)
                                        .error(R.drawable.ic_user_placeholder)
                                        .circleCrop()
                                        .into(ivProfilePhoto);
                            } else {
                                ivProfilePhoto.setImageResource(R.drawable.ic_user_placeholder);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to load user data", e);
                    showError("Failed to load profile data");
                });
    }

    private void saveProfile() {
        // Validate required fields
        String name = getText(etFullName);
        String studentId = getText(etStudentId);

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Name is required");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(studentId)) {
            etStudentId.setError("Student ID is required");
            etStudentId.requestFocus();
            return;
        }

        showLoading(true);
        btnSave.setEnabled(false);

        // If new image selected, upload it first
        if (selectedImageUri != null) {
            uploadProfilePhoto();
        } else {
            updateFirestoreProfile(currentProfilePictureUrl);
        }
    }

    private void uploadProfilePhoto() {
        CloudinaryUploader.upload(this, selectedImageUri, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                Log.d(TAG, "Profile photo uploaded: " + imageUrl);
                updateFirestoreProfile(imageUrl);
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                btnSave.setEnabled(true);
                Log.e(TAG, "Upload failed: " + error);
                showError("Failed to upload photo: " + error);
            }
        });
    }

    private void updateFirestoreProfile(String profilePictureUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", getText(etFullName));
        updates.put("studentId", getText(etStudentId));
        updates.put("phone", getText(etPhone));
        updates.put("department", getText(etDepartment));
        updates.put("year", getText(etYear));
        updates.put("meetupLocation", getText(etMeetupLocation));
        updates.put("bio", getText(etBio));

        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            updates.put("profilePicture", profilePictureUrl);
        }

        firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    btnSave.setEnabled(true);

                    // Update session manager
                    sessionManager.saveUserName(getText(etFullName));

                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Return to profile with result
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    btnSave.setEnabled(true);
                    Log.e(TAG, "Failed to update profile", e);
                    showError("Failed to save profile: " + e.getMessage());
                });
    }

    private String getText(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(!show);
        }
    }

    private void showError(String message) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark, null));
        snackbar.setTextColor(0xFFFFFFFF);
        snackbar.show();
    }
}
