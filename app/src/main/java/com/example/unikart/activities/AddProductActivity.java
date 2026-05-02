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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.firebase.ProductRepository;
import com.example.unikart.utils.CloudinaryUploader;
import com.example.unikart.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";

    private TextInputEditText etProductName;
    private TextInputEditText etDescription;
    private TextInputEditText etPrice;
    private TextInputEditText etMaxRentDays;
    private TextInputLayout tilMaxRentDays;
    private AutoCompleteTextView spinnerCategory;
    private AutoCompleteTextView spinnerCondition;
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
    private Uri cameraImageUri;          // URI for the temp camera file
    private String uploadedImageUrl = "";

    // ── Permission launcher (gallery) ────────────────────────────────────────
    private final ActivityResultLauncher<String> galleryPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    launchGalleryPicker();
                } else {
                    showSnackbar("Storage permission denied — cannot pick image");
                }
            }
    );

    // ── Permission launcher (camera) ─────────────────────────────────────────
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    showSnackbar("Camera permission denied");
                }
            }
    );

    // ── Gallery picker launcher ──────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> galleryPickerLauncher = registerForActivityResult(
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
                        showImagePreview(uri);
                        uploadImageToCloudinary(uri);
                    }
                }
            }
    );

    // ── Camera launcher ──────────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    showImagePreview(cameraImageUri);
                    uploadImageToCloudinary(cameraImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        productRepository = new ProductRepository();
        initViews();
        setupCategorySpinner();
        setupConditionSpinner();
        setupTypeToggle();
    }

    private void initViews() {
        etProductName    = findViewById(R.id.etProductName);
        etDescription    = findViewById(R.id.etDescription);
        etPrice          = findViewById(R.id.etPrice);
        etMaxRentDays    = findViewById(R.id.etMaxRentDays);
        tilMaxRentDays   = findViewById(R.id.tilMaxRentDays);
        spinnerCategory  = findViewById(R.id.spinnerCategory);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        rgProductType    = findViewById(R.id.rgProductType);
        rbForSale        = findViewById(R.id.rbForSale);
        btnSubmit        = findViewById(R.id.btnSubmit);
        uploadArea       = findViewById(R.id.uploadArea);
        progressBar      = findViewById(R.id.progressBar);
        tvUploadStatus   = findViewById(R.id.tvUploadStatus);
        ivImagePreview   = findViewById(R.id.ivImagePreview);
        uploadPlaceholder = findViewById(R.id.uploadPlaceholder);
        tvChangeImage    = findViewById(R.id.tvChangeImage);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Tap on upload area → show source chooser dialog
        uploadArea.setOnClickListener(v -> showImageSourceDialog());

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                submitProduct();
            }
        });
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

    private void setupTypeToggle() {
        rgProductType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isRent = checkedId == R.id.rbForRent;
            tilMaxRentDays.setVisibility(isRent ? View.VISIBLE : View.GONE);
        });
    }

    // ── Image source dialog ───────────────────────────────────────────────────

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(new String[]{"Take a Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        File photoFile;
        try {
            photoFile = createTempImageFile();
        } catch (IOException e) {
            showSnackbar("Could not create image file");
            return;
        }
        cameraImageUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                photoFile
        );
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(intent);
    }

    private File createTempImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "PRODUCT_" + timeStamp + "_";
        File storageDir = getExternalCacheDir();
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    // ── Gallery ───────────────────────────────────────────────────────────────

    private void openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                launchGalleryPicker();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                launchGalleryPicker();
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        } else {
            launchGalleryPicker();
        }
    }

    private void launchGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryPickerLauncher.launch(Intent.createChooser(intent, "Select product image"));
    }

    // ── Image preview ─────────────────────────────────────────────────────────

    private void showImagePreview(Uri uri) {
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
        String name      = etProductName.getText() != null ? etProductName.getText().toString().trim() : "";
        String desc      = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String priceStr  = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String category  = spinnerCategory.getText() != null ? spinnerCategory.getText().toString().trim() : "";
        String condition = spinnerCondition.getText() != null ? spinnerCondition.getText().toString().trim() : "";

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
        if (TextUtils.isEmpty(category)) {
            showSnackbar("Please select a category");
            return false;
        }
        if (TextUtils.isEmpty(condition)) {
            showSnackbar("Please select a condition");
            return false;
        }
        boolean isRent = rgProductType.getCheckedRadioButtonId() == R.id.rbForRent;
        if (isRent) {
            String maxDaysStr = etMaxRentDays.getText() != null ? etMaxRentDays.getText().toString().trim() : "";
            if (TextUtils.isEmpty(maxDaysStr)) {
                showSnackbar("Please enter the maximum number of rent days");
                return false;
            }
            try {
                int maxDays = Integer.parseInt(maxDaysStr);
                if (maxDays < 1) {
                    showSnackbar("Max rent days must be at least 1");
                    return false;
                }
            } catch (NumberFormatException e) {
                showSnackbar("Please enter a valid number of days");
                return false;
            }
        }
        return true;
    }

    // ── Submit product ────────────────────────────────────────────────────────

    private void submitProduct() {
        String title       = etProductName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        double price       = Double.parseDouble(etPrice.getText().toString().trim());
        String category    = spinnerCategory.getText().toString().trim();
        String condition   = spinnerCondition.getText().toString().trim();
        boolean isRent     = rgProductType.getCheckedRadioButtonId() == R.id.rbForRent;
        String type        = isRent ? Constants.PRODUCT_TYPE_RENT : Constants.PRODUCT_TYPE_BUY;
        int maxRentDays    = 0;
        if (isRent && etMaxRentDays.getText() != null && !etMaxRentDays.getText().toString().trim().isEmpty()) {
            try { maxRentDays = Integer.parseInt(etMaxRentDays.getText().toString().trim()); } catch (NumberFormatException ignored) {}
        }

        setFormLoading(true);

        productRepository.addProduct(title, description, price, type,
                category.isEmpty() ? Constants.CATEGORY_OTHER : category,
                condition.isEmpty() ? "Good" : condition,
                uploadedImageUrl, maxRentDays,
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
        spinnerCategory.setEnabled(!loading);
        spinnerCondition.setEnabled(!loading);
        if (etMaxRentDays != null) etMaxRentDays.setEnabled(!loading);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
