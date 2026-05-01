package com.example.unikart.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.firebase.ProductRepository;
import com.example.unikart.utils.CloudinaryUploader;
import com.example.unikart.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";

    private TextInputEditText etProductName;
    private TextInputEditText etDescription;
    private TextInputEditText etPrice;
    private RadioGroup rgProductType;
    private RadioButton rbForSale;
    private MaterialButton btnSubmit;
    private ConstraintLayout uploadArea;
    private ProgressBar progressBar;
    private TextView tvUploadStatus;
    private ImageView ivImagePreview;
    private LinearLayout uploadPlaceholder;
    private TextView tvChangeImage;

    private ProductRepository productRepository;
    private Uri selectedImageUri;
    private String uploadedImageUrl = "";

    // ── Permission launcher ──────────────────────────────────────────────────
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    launchImagePicker();
                } else {
                    showSnackbar("Permission denied — cannot pick image");
                }
            }
    );

    // ── Image picker launcher ────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
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

                        // Show preview immediately — don't wait for upload
                        showImagePreview(uri);
                        // Start Cloudinary upload in background
                        uploadImageToCloudinary(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        productRepository = new ProductRepository();
        initViews();
    }

    private void initViews() {
        etProductName   = findViewById(R.id.etProductName);
        etDescription   = findViewById(R.id.etDescription);
        etPrice         = findViewById(R.id.etPrice);
        rgProductType   = findViewById(R.id.rgProductType);
        rbForSale       = findViewById(R.id.rbForSale);
        btnSubmit       = findViewById(R.id.btnSubmit);
        uploadArea      = findViewById(R.id.uploadArea);
        progressBar     = findViewById(R.id.progressBar);
        tvUploadStatus  = findViewById(R.id.tvUploadStatus);
        ivImagePreview  = findViewById(R.id.ivImagePreview);
        uploadPlaceholder = findViewById(R.id.uploadPlaceholder);
        tvChangeImage   = findViewById(R.id.tvChangeImage);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        uploadArea.setOnClickListener(v -> openImagePicker());

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                submitProduct();
            }
        });
    }

    // ── Image picker ─────────────────────────────────────────────────────────

    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            launchImagePicker();
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select product image"));
    }

    // ── Image preview ─────────────────────────────────────────────────────────

    private void showImagePreview(Uri uri) {
        // Show the image immediately using Glide
        ivImagePreview.setVisibility(View.VISIBLE);
        tvChangeImage.setVisibility(View.VISIBLE);
        uploadPlaceholder.setVisibility(View.GONE);

        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(ivImagePreview);
    }

    // ── Cloudinary upload ─────────────────────────────────────────────────────

    private void uploadImageToCloudinary(Uri uri) {
        setUploadLoading(true);
        showUploadStatus("Uploading image...", false);

        CloudinaryUploader.upload(this, uri, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                setUploadLoading(false);
                uploadedImageUrl = imageUrl;
                showUploadStatus("✓ Image uploaded", true);
                Log.d(TAG, "Cloudinary upload success: " + imageUrl);
            }

            @Override
            public void onFailure(String error) {
                setUploadLoading(false);
                uploadedImageUrl = "";
                showUploadStatus("Upload failed: " + error, false);
                Log.e(TAG, "Cloudinary upload failed: " + error);
            }
        });
    }

    private void showUploadStatus(String message, boolean success) {
        if (tvUploadStatus != null) {
            tvUploadStatus.setVisibility(View.VISIBLE);
            tvUploadStatus.setText(message);
            tvUploadStatus.setTextColor(getResources().getColor(
                    success ? android.R.color.holo_green_dark : android.R.color.holo_orange_dark,
                    null));
        }
    }

    private void setUploadLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    // ── Form validation ───────────────────────────────────────────────────────

    private boolean validateForm() {
        String name     = etProductName.getText() != null ? etProductName.getText().toString().trim() : "";
        String desc     = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            showSnackbar("Please enter a product name");
            return false;
        }
        if (TextUtils.isEmpty(desc)) {
            showSnackbar("Please add a description");
            return false;
        }
        if (TextUtils.isEmpty(priceStr)) {
            showSnackbar("Please set a price");
            return false;
        }
        try {
            double price = Double.parseDouble(priceStr);
            if (price < Constants.MIN_PRODUCT_PRICE || price > Constants.MAX_PRODUCT_PRICE) {
                showSnackbar("Price must be between ₹" + Constants.MIN_PRODUCT_PRICE
                        + " and ₹" + Constants.MAX_PRODUCT_PRICE);
                return false;
            }
        } catch (NumberFormatException e) {
            showSnackbar("Please enter a valid price");
            return false;
        }
        // Image is optional — product can be listed without one
        return true;
    }

    // ── Submit product ────────────────────────────────────────────────────────

    private void submitProduct() {
        String title       = etProductName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        double price       = Double.parseDouble(etPrice.getText().toString().trim());
        String type        = (rbForSale != null && rbForSale.isChecked())
                ? Constants.PRODUCT_TYPE_BUY
                : Constants.PRODUCT_TYPE_RENT;

        setFormLoading(true);

        productRepository.addProduct(title, description, price, type,
                Constants.CATEGORY_OTHER, "Good", uploadedImageUrl,
                new ProductRepository.ProductCallback() {
                    @Override
                    public void onSuccess(String message) {
                        setFormLoading(false);
                        Log.d(TAG, "Product added successfully");
                        Snackbar.make(findViewById(android.R.id.content),
                                "Product Listed!", Snackbar.LENGTH_SHORT).show();

                        uploadArea.postDelayed(() -> {
                            Intent intent = new Intent(AddProductActivity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        }, 600);
                    }

                    @Override
                    public void onFailure(String error) {
                        setFormLoading(false);
                        Log.e(TAG, "addProduct failed: " + error);
                        showSnackbar("Failed to list product: " + error);
                    }
                });
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void setFormLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        uploadArea.setEnabled(!loading);
        etProductName.setEnabled(!loading);
        etDescription.setEnabled(!loading);
        etPrice.setEnabled(!loading);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
