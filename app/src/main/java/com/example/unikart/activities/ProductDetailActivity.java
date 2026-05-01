package com.example.unikart.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.firebase.OrderRepository;
import com.example.unikart.firebase.ProductRepository;
import com.example.unikart.models.Product;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";

    private ImageView ivProductImage;
    private TextView tvProductTitle;
    private TextView tvPrice;
    private TextView tvBadge;
    private TextView tvDescription;
    private TextView tvSellerName;
    private TextView tvSellerRating;
    private MaterialButton btnChat;
    private MaterialButton btnRequest;
    private MaterialButton btnEdit;
    private ProgressBar progressBar;

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private SessionManager sessionManager;
    private Product currentProduct;

    // Edit listing launcher
    private final ActivityResultLauncher<Intent> editListingLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    boolean updated = result.getData().getBooleanExtra("updated", false);
                    boolean deleted = result.getData().getBooleanExtra("deleted", false);
                    
                    if (deleted) {
                        Snackbar.make(findViewById(android.R.id.content), 
                                "Listing deleted", Snackbar.LENGTH_SHORT).show();
                        finish();
                    } else if (updated) {
                        Snackbar.make(findViewById(android.R.id.content), 
                                "Listing updated", Snackbar.LENGTH_SHORT).show();
                        loadProductData(); // Refresh data
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productRepository = new ProductRepository();
        orderRepository   = new OrderRepository();
        sessionManager    = new SessionManager(this);
        initViews();
        loadProductData();
    }

    private void initViews() {
        ivProductImage  = findViewById(R.id.ivProductImage);
        tvProductTitle  = findViewById(R.id.tvProductTitle);
        tvPrice         = findViewById(R.id.tvPrice);
        tvBadge         = findViewById(R.id.tvBadge);
        tvDescription   = findViewById(R.id.tvDescription);
        tvSellerName    = findViewById(R.id.tvSellerName);
        tvSellerRating  = findViewById(R.id.tvSellerRating);
        btnChat         = findViewById(R.id.btnChat);
        btnRequest      = findViewById(R.id.btnRequest);
        btnEdit         = findViewById(R.id.btnEdit);
        progressBar     = findViewById(R.id.progressBar);

        FrameLayout btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        if (btnChat != null) {
            btnChat.setOnClickListener(v -> {
                if (currentProduct != null) {
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("seller_id",    currentProduct.getSellerId());
                    intent.putExtra("seller_name",  currentProduct.getSellerName());
                    intent.putExtra("product_id",   currentProduct.getId());
                    intent.putExtra("product_title", currentProduct.getName());
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }

        if (btnRequest != null) {
            btnRequest.setOnClickListener(v -> showRequestDialog());
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> openEditListing());
        }
    }

    private void loadProductData() {
        String productId = getIntent().getStringExtra("product_id");
        if (productId == null || productId.isEmpty()) {
            Log.e(TAG, "No product_id in intent");
            finish();
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        productRepository.getProductById(productId, new ProductRepository.ProductDetailCallback() {
            @Override
            public void onSuccess(Product product) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                currentProduct = product;
                displayProduct(product);
            }

            @Override
            public void onFailure(String error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Log.e(TAG, "loadProductData failed: " + error);
                if (tvProductTitle != null) tvProductTitle.setText("Product unavailable");
                if (tvDescription  != null) tvDescription.setText(error);
            }
        });
    }

    private void displayProduct(Product product) {
        if (product == null) return;

        tvProductTitle.setText(product.getName());
        tvPrice.setText(String.format(Locale.getDefault(), "₹ %.0f", product.getPrice()));
        tvDescription.setText(product.getDescription());
        tvSellerName.setText(product.getSellerName());
        
        // Check if current user is the owner
        String currentUserId = sessionManager.getUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(product.getSellerId());
        
        // Show/hide buttons based on ownership
        if (isOwner) {
            // Owner sees Edit button, no Chat/Request buttons
            if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
            if (btnChat != null) btnChat.setVisibility(View.GONE);
            if (btnRequest != null) btnRequest.setVisibility(View.GONE);
        } else {
            // Non-owner sees Chat/Request buttons, no Edit button
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            if (btnChat != null) btnChat.setVisibility(View.VISIBLE);
            if (btnRequest != null) btnRequest.setVisibility(View.VISIBLE);
        }
        
        // Display seller rating
        if (tvSellerRating != null) {
            if (product.getSellerReviewCount() > 0) {
                tvSellerRating.setVisibility(View.VISIBLE);
                tvSellerRating.setText(String.format(Locale.getDefault(), 
                        "⭐ %.1f (%d)", product.getSellerRating(), product.getSellerReviewCount()));
            } else {
                tvSellerRating.setVisibility(View.GONE);
            }
        }

        String imageUrl = product.getImageUrl();
        if (ivProductImage != null && imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(ivProductImage);
        }

        if (Constants.PRODUCT_TYPE_BUY.equals(product.getType())) {
            tvBadge.setText(R.string.badge_buy);
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_buy_text));
            Drawable bg = ContextCompat.getDrawable(this, R.drawable.bg_badge_sale);
            tvBadge.setBackground(bg);
        } else {
            tvBadge.setText(R.string.badge_rent);
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_rent_text));
            Drawable bg = ContextCompat.getDrawable(this, R.drawable.bg_badge_rent);
            tvBadge.setBackground(bg);
        }
    }

    private void openEditListing() {
        if (currentProduct == null) return;
        
        Intent intent = new Intent(this, EditListingActivity.class);
        intent.putExtra(EditListingActivity.EXTRA_PRODUCT_ID, currentProduct.getId());
        editListingLauncher.launch(intent);
    }

    private void showRequestDialog() {
        if (currentProduct == null) return;

        String actionLabel = Constants.PRODUCT_TYPE_RENT.equals(currentProduct.getType())
                ? "Rent" : "Buy";

        new AlertDialog.Builder(this)
                .setTitle(actionLabel + " Request")
                .setMessage("Send a " + actionLabel.toLowerCase() + " request to "
                        + currentProduct.getSellerName()
                        + " for \"" + currentProduct.getName() + "\"?\n\n"
                        + "Price: ₹" + String.format(Locale.getDefault(), "%.0f", currentProduct.getPrice()))
                .setPositiveButton("Send Request", (dialog, which) -> sendRequest())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendRequest() {
        if (currentProduct == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (btnRequest  != null) btnRequest.setEnabled(false);

        orderRepository.createOrder(
                currentProduct.getId(),
                currentProduct.getName(),
                currentProduct.getImageUrl(),
                currentProduct.getPrice(),
                currentProduct.getSellerId(),
                currentProduct.getSellerName(),
                currentProduct.getType(),
                new OrderRepository.OrderCallback() {
                    @Override
                    public void onSuccess(String orderId) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (btnRequest  != null) btnRequest.setEnabled(true);

                        new AlertDialog.Builder(ProductDetailActivity.this)
                                .setTitle("Request Sent!")
                                .setMessage("Your request has been sent to "
                                        + currentProduct.getSellerName()
                                        + ".\n\nYou can track it in My Orders.")
                                .setPositiveButton("View Orders", (d, w) -> {
                                    startActivity(new Intent(ProductDetailActivity.this, OrdersActivity.class));
                                })
                                .setNegativeButton("OK", null)
                                .show();
                    }

                    @Override
                    public void onFailure(String error) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (btnRequest  != null) btnRequest.setEnabled(true);
                        Log.e(TAG, "sendRequest failed: " + error);
                        Snackbar.make(findViewById(android.R.id.content),
                                "Could not send request. Try again.", Snackbar.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
