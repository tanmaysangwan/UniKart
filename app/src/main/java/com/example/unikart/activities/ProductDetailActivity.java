package com.example.unikart.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
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
import com.example.unikart.models.Review;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
    private TextView tvCategory;
    private TextView tvMaxRentDays;
    private MaterialButton btnChat;
    private MaterialButton btnRequest;
    private MaterialButton btnEdit;
    private ProgressBar progressBar;
    // Reviews
    private LinearLayout llReviews;
    private TextView tvReviewCount;
    private TextView tvNoReviews;

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private SessionManager sessionManager;
    private Product currentProduct;
    private ListenerRegistration productListener;

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
        tvCategory      = findViewById(R.id.tvCategory);
        tvMaxRentDays   = findViewById(R.id.tvMaxRentDays);
        btnChat         = findViewById(R.id.btnChat);
        btnRequest      = findViewById(R.id.btnRequest);
        btnEdit         = findViewById(R.id.btnEdit);
        progressBar     = findViewById(R.id.progressBar);
        llReviews       = findViewById(R.id.llReviews);
        tvReviewCount   = findViewById(R.id.tvReviewCount);
        tvNoReviews     = findViewById(R.id.tvNoReviews);

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

        // Remove existing listener if any
        if (productListener != null) {
            productListener.remove();
            productListener = null;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        productListener = productRepository.listenToProductById(productId, new ProductRepository.ProductDetailListener() {
            @Override
            public void onProduct(Product product) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                currentProduct = product;
                displayProduct(product);
                loadProductReviews(product.getId());
            }

            @Override
            public void onError(String error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Log.e(TAG, "loadProductData failed: " + error);
                if (tvProductTitle != null) tvProductTitle.setText("Product unavailable");
                if (tvDescription  != null) tvDescription.setText(error);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove listener to prevent memory leaks
        if (productListener != null) {
            productListener.remove();
            productListener = null;
        }
    }

    private void displayProduct(Product product) {
        if (product == null) return;

        tvProductTitle.setText(product.getName());
        tvDescription.setText(product.getDescription());
        tvSellerName.setText(product.getSellerName());

        boolean isRent = Constants.PRODUCT_TYPE_RENT.equals(product.getType());
        boolean isAvailable = product.isAvailable();

        // Price label — show "per day" for rent items
        if (isRent) {
            tvPrice.setText(String.format(Locale.getDefault(), "₹ %.0f/day", product.getPrice()));
        } else {
            tvPrice.setText(String.format(Locale.getDefault(), "₹ %.0f", product.getPrice()));
        }

        // Category
        if (tvCategory != null) {
            String cat = product.getCategory();
            String emoji = Constants.categoryEmoji(cat);
            tvCategory.setText(emoji + " " + (cat != null && !cat.isEmpty() ? cat : "Other"));
        }

        // Max rent days
        if (tvMaxRentDays != null) {
            if (isRent && product.getMaxRentDays() > 0) {
                tvMaxRentDays.setVisibility(View.VISIBLE);
                tvMaxRentDays.setText("🗓 Max " + product.getMaxRentDays() + " days");
            } else {
                tvMaxRentDays.setVisibility(View.GONE);
            }
        }

        // Check if current user is the owner
        String currentUserId = sessionManager.getUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(product.getSellerId());
        
        // Show/hide buttons based on ownership and availability
        if (isOwner) {
            // Owner sees Edit button, no Chat/Request buttons
            if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
            if (btnChat != null) btnChat.setVisibility(View.GONE);
            if (btnRequest != null) btnRequest.setVisibility(View.GONE);
        } else {
            // Non-owner sees Chat/Request buttons, no Edit button
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            
            // Chat button always visible (even for unavailable items - for future bookings)
            if (btnChat != null) btnChat.setVisibility(View.VISIBLE);
            
            if (btnRequest != null) {
                btnRequest.setVisibility(View.VISIBLE);
                if (!isAvailable) {
                    // Disable buy/rent button if unavailable, but show appropriate message
                    if (isRent) {
                        btnRequest.setText("Currently Rented");
                    } else {
                        btnRequest.setText("Sold Out");
                    }
                    btnRequest.setEnabled(false);
                    btnRequest.setAlpha(0.5f);
                } else {
                    btnRequest.setText(isRent ? "Rent Now" : "Buy Now");
                    btnRequest.setEnabled(true);
                    btnRequest.setAlpha(1f);
                }
            }
        }
        
        // Display seller rating - only show if seller has reviews AND a positive rating
        if (tvSellerRating != null) {
            if (product.getSellerReviewCount() > 0 && product.getSellerRating() > 0) {
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

        // Badge — show unavailable or buy/rent
        if (!isAvailable) {
            if (isRent) {
                tvBadge.setText("RENTED OUT");
            } else {
                tvBadge.setText("SOLD OUT");
            }
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            Drawable bg = ContextCompat.getDrawable(this, R.drawable.bg_icon_container);
            tvBadge.setBackground(bg);
        } else if (Constants.PRODUCT_TYPE_BUY.equals(product.getType())) {
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

    private void loadProductReviews(String productId) {
        if (llReviews == null) return;
        orderRepository.getReviewsForProduct(productId, new OrderRepository.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> {
                    llReviews.removeAllViews();
                    if (reviews == null || reviews.isEmpty()) {
                        if (tvNoReviews != null) tvNoReviews.setVisibility(View.VISIBLE);
                        if (tvReviewCount != null) tvReviewCount.setVisibility(View.GONE);
                        return;
                    }
                    if (tvNoReviews != null) tvNoReviews.setVisibility(View.GONE);
                    if (tvReviewCount != null) {
                        tvReviewCount.setVisibility(View.VISIBLE);
                        // Compute average
                        float total = 0;
                        for (Review r : reviews) total += r.getRating();
                        float avg = total / reviews.size();
                        tvReviewCount.setText(String.format(Locale.getDefault(),
                                "⭐ %.1f  ·  %d review%s", avg, reviews.size(), reviews.size() == 1 ? "" : "s"));
                    }
                    LayoutInflater inflater = LayoutInflater.from(ProductDetailActivity.this);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    for (Review r : reviews) {
                        View card = inflater.inflate(R.layout.item_review, llReviews, false);
                        TextView tvName    = card.findViewById(R.id.tvReviewerName);
                        RatingBar rb       = card.findViewById(R.id.ratingBar);
                        TextView tvRating  = card.findViewById(R.id.tvRating);
                        TextView tvComment = card.findViewById(R.id.tvComment);
                        TextView tvDate    = card.findViewById(R.id.tvDate);
                        ImageView ivAvatar = card.findViewById(R.id.ivReviewerAvatar);
                        LinearLayout layoutProduct = card.findViewById(R.id.layoutProductInfo);
                        if (layoutProduct != null) layoutProduct.setVisibility(View.GONE);

                        tvName.setText(r.getReviewerName());
                        rb.setRating(r.getRating());
                        tvRating.setText(String.format(Locale.getDefault(), "%.1f", r.getRating()));
                        if (r.getComment() != null && !r.getComment().isEmpty()) {
                            tvComment.setText(r.getComment());
                            tvComment.setVisibility(View.VISIBLE);
                        } else {
                            tvComment.setVisibility(View.GONE);
                        }
                        tvDate.setText(r.getTimestamp() > 0 ? sdf.format(new Date(r.getTimestamp())) : "");

                        String pic = r.getReviewerProfilePic();
                        if (pic != null && !pic.isEmpty()) {
                            Glide.with(ProductDetailActivity.this).load(pic)
                                    .placeholder(R.drawable.bg_avatar_placeholder).circleCrop().into(ivAvatar);
                        } else {
                            ivAvatar.setImageResource(R.drawable.bg_avatar_placeholder);
                        }
                        llReviews.addView(card);
                    }
                });
            }
            @Override
            public void onFailure(String error) {
                Log.w(TAG, "loadProductReviews failed: " + error);
                runOnUiThread(() -> {
                    if (tvNoReviews != null) tvNoReviews.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void openEditListing() {
        if (currentProduct == null) return;
        
        Intent intent = new Intent(this, EditListingActivity.class);
        intent.putExtra(EditListingActivity.EXTRA_PRODUCT_ID, currentProduct.getId());
        editListingLauncher.launch(intent);
    }

    private void showRequestDialog() {
        if (currentProduct == null) return;

        boolean isRent = Constants.PRODUCT_TYPE_RENT.equals(currentProduct.getType());

        if (isRent) {
            // For rent: ask how many days
            View dialogView = getLayoutInflater().inflate(android.R.layout.activity_list_item, null);
            // Use a simple EditText dialog
            final EditText etDays = new EditText(this);
            etDays.setHint("Number of days");
            etDays.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            etDays.setPadding(48, 24, 48, 24);

            int maxDays = currentProduct.getMaxRentDays();
            String maxDaysNote = maxDays > 0 ? "\nMax allowed: " + maxDays + " days" : "";

            new AlertDialog.Builder(this)
                    .setTitle("Rent Request")
                    .setMessage("How many days do you want to rent \"" + currentProduct.getName() + "\"?\n"
                            + "Price: ₹" + String.format(Locale.getDefault(), "%.0f", currentProduct.getPrice()) + "/day"
                            + maxDaysNote)
                    .setView(etDays)
                    .setPositiveButton("Send Request", (dialog, which) -> {
                        String daysStr = etDays.getText().toString().trim();
                        if (daysStr.isEmpty()) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    "Please enter number of days", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        int days;
                        try {
                            days = Integer.parseInt(daysStr);
                        } catch (NumberFormatException e) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    "Invalid number of days", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        if (days < 1) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    "Must be at least 1 day", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        if (maxDays > 0 && days > maxDays) {
                            Snackbar.make(findViewById(android.R.id.content),
                                    "Max allowed is " + maxDays + " days", Snackbar.LENGTH_SHORT).show();
                            return;
                        }
                        double total = currentProduct.getPrice() * days;
                        new AlertDialog.Builder(this)
                                .setTitle("Confirm Rent Request")
                                .setMessage("Rent \"" + currentProduct.getName() + "\" for " + days + " day(s)?\n\n"
                                        + "Rate: ₹" + String.format(Locale.getDefault(), "%.0f", currentProduct.getPrice()) + "/day\n"
                                        + "Total: ₹" + String.format(Locale.getDefault(), "%.0f", total))
                                .setPositiveButton("Confirm", (d2, w2) -> sendRequest(days))
                                .setNegativeButton("Cancel", null)
                                .show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // For buy: simple confirmation
            new AlertDialog.Builder(this)
                    .setTitle("Buy Request")
                    .setMessage("Send a buy request to "
                            + currentProduct.getSellerName()
                            + " for \"" + currentProduct.getName() + "\"?\n\n"
                            + "Price: ₹" + String.format(Locale.getDefault(), "%.0f", currentProduct.getPrice()))
                    .setPositiveButton("Send Request", (dialog, which) -> sendRequest(0))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void sendRequest(int rentDays) {
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
                rentDays,
                new OrderRepository.OrderCallback() {
                    @Override
                    public void onSuccess(String orderId) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (btnRequest  != null) btnRequest.setEnabled(true);

                        boolean isRent = Constants.PRODUCT_TYPE_RENT.equals(currentProduct.getType());
                        String successMsg = isRent
                                ? "Rent request sent for " + rentDays + " day(s)!\nTotal: ₹"
                                    + String.format(Locale.getDefault(), "%.0f", currentProduct.getPrice() * rentDays)
                                : "Your request has been sent to " + currentProduct.getSellerName() + ".";

                        new AlertDialog.Builder(ProductDetailActivity.this)
                                .setTitle("Request Sent!")
                                .setMessage(successMsg + "\n\nYou can track it in My Orders.")
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
