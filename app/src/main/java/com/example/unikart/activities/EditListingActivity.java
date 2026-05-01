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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.firebase.ProductRepository;
import com.example.unikart.utils.CloudinaryUploader;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditListingActivity extends AppCompatActivity {

    private static final String TAG = "EditListingActivity";
    public static final String EXTRA_PRODUCT_ID = "product_id";

    private TextInputEditText etTitle, etDescription, etPrice;
    private AutoCompleteTextView spinnerCategory, spinnerCondition;
    private RadioGroup rgType;
    private RadioButton rbBuy, rbRent;
    private ImageView ivProductImage;
    private MaterialButton btnChangeImage, btnSave, btnDelete;
    private ProgressBar progressBar;
    private View rootView;

    private String productId;
    private String currentImageUrl = "";
    private Uri selectedImageUri;
    private boolean imageChanged = false;

    private SessionManager sessionManager;
    private ProductRepository productRepository;
    private FirebaseFirestore firestore;

    // Image picker launcher
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        imageChanged = true;
                        Glide.with(this)
                                .load(selectedImageUri)
                                .centerCrop()
                                .into(ivProductImage);
                    }
                }
            }
    );

    // Gallery permission launcher
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_listing);

        sessionManager = new SessionManager(this);
        productRepository = new ProductRepository();
        firestore = FirebaseFirestore.getInstance();
        rootView = findViewById(android.R.id.content);

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null || productId.isEmpty()) {
            showError("Invalid product ID");
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupCategorySpinner();
        setupConditionSpinner();
        loadProductData();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        rgType = findViewById(R.id.rgType);
        rbBuy = findViewById(R.id.rbBuy);
        rbRent = findViewById(R.id.rbRent);
        ivProductImage = findViewById(R.id.ivProductImage);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Listing");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                Constants.ALL_CATEGORIES
        );
        spinnerCategory.setAdapter(adapter);
    }

    private void setupConditionSpinner() {
        String[] conditions = {"Good", "Like New", "Fair"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                conditions
        );
        spinnerCondition.setAdapter(adapter);
    }

    private void loadProductData() {
        showLoading(true);
        
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showError("Product not found");
                        finish();
                        return;
                    }

                    // Check ownership
                    String ownerId = doc.getString("ownerId");
                    String currentUserId = sessionManager.getUserId();
                    if (ownerId == null || !ownerId.equals(currentUserId)) {
                        showError("You don't have permission to edit this listing");
                        finish();
                        return;
                    }

                    // Pre-fill data
                    String title = doc.getString("title");
                    String description = doc.getString("description");
                    Double price = doc.getDouble("price");
                    String type = doc.getString("type");
                    String category = doc.getString("category");
                    String condition = doc.getString("condition");
                    currentImageUrl = doc.getString("imageUrl");

                    etTitle.setText(title);
                    etDescription.setText(description);
                    if (price != null) {
                        etPrice.setText(String.valueOf(price.intValue()));
                    }

                    // Set type
                    if (Constants.PRODUCT_TYPE_RENT.equals(type)) {
                        rbRent.setChecked(true);
                    } else {
                        rbBuy.setChecked(true);
                    }

                    // Set category
                    if (category != null) {
                        spinnerCategory.setText(category, false);
                    }

                    // Set condition
                    if (condition != null) {
                        spinnerCondition.setText(condition, false);
                    }

                    // Load image
                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(currentImageUrl)
                                .placeholder(R.drawable.bg_image_placeholder)
                                .centerCrop()
                                .into(ivProductImage);
                    }

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load product", e);
                    showError("Failed to load product: " + e.getMessage());
                    showLoading(false);
                    finish();
                });
    }

    private void setupListeners() {
        btnChangeImage.setOnClickListener(v -> showImageOptions());
        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void showImageOptions() {
        String[] options = {"Choose from Gallery", "Remove Image", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Change Product Image")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            checkGalleryPermissionAndLaunch();
                            break;
                        case 1:
                            removeImage();
                            break;
                    }
                })
                .show();
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

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void removeImage() {
        imageChanged = true;
        selectedImageUri = null;
        currentImageUrl = "";
        ivProductImage.setImageResource(R.drawable.bg_image_placeholder);
        showInfo("Image will be removed when you save");
    }

    private void saveChanges() {
        // Validate inputs
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String category = spinnerCategory.getText().toString().trim();
        String condition = spinnerCondition.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Title required");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description required");
            etDescription.requestFocus();
            return;
        }

        if (priceStr.isEmpty()) {
            etPrice.setError("Price required");
            etPrice.requestFocus();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < Constants.MIN_PRODUCT_PRICE || price > Constants.MAX_PRODUCT_PRICE) {
                etPrice.setError("Price must be between ₹" + Constants.MIN_PRODUCT_PRICE + " and ₹" + Constants.MAX_PRODUCT_PRICE);
                etPrice.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etPrice.setError("Invalid price");
            etPrice.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            showError("Please select a category");
            return;
        }

        if (condition.isEmpty()) {
            showError("Please select a condition");
            return;
        }

        String type = rbRent.isChecked() ? Constants.PRODUCT_TYPE_RENT : Constants.PRODUCT_TYPE_BUY;

        // If image changed, upload new image first
        if (imageChanged && selectedImageUri != null) {
            uploadImageAndUpdate(title, description, price, type, category, condition);
        } else {
            // No image change, update directly
            updateProduct(title, description, price, type, category, condition, currentImageUrl);
        }
    }

    private void uploadImageAndUpdate(String title, String description, double price,
                                      String type, String category, String condition) {
        showLoading(true);
        showInfo("Uploading image...");

        CloudinaryUploader.upload(this, selectedImageUri, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    showInfo("Image uploaded successfully");
                    updateProduct(title, description, price, type, category, condition, imageUrl);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Image upload failed: " + error);
                });
            }
        });
    }

    private void updateProduct(String title, String description, double price,
                               String type, String category, String condition, String imageUrl) {
        showLoading(true);
        showInfo("Updating listing...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("type", type);
        updates.put("category", category);
        updates.put("condition", condition);
        updates.put("imageUrl", imageUrl != null ? imageUrl : "");
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    showLoading(false);
                    showInfo("Listing updated successfully");
                    
                    // Return result
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated", true);
                    setResult(RESULT_OK, resultIntent);
                    
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to update product", e);
                    showError("Failed to update listing: " + e.getMessage());
                });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Listing")
                .setMessage("Are you sure you want to delete this listing? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteListing())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteListing() {
        showLoading(true);
        showInfo("Deleting listing...");

        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .delete()
                .addOnSuccessListener(v -> {
                    showLoading(false);
                    showInfo("Listing deleted successfully");
                    
                    // Return result
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("deleted", true);
                    setResult(RESULT_OK, resultIntent);
                    
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Failed to delete product", e);
                    showError("Failed to delete listing: " + e.getMessage());
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!show);
        btnDelete.setEnabled(!show);
        btnChangeImage.setEnabled(!show);
    }

    private void showInfo(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Snackbar snack = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snack.setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark, null));
        snack.setTextColor(0xFFFFFFFF);
        snack.show();
    }
}
