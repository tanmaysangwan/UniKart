package com.example.unikart.activities;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.adapters.ReviewAdapter;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.firebase.OrderRepository;
import com.example.unikart.models.Review;
import com.example.unikart.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read-only public profile screen for any user.
 * Launched from ChatActivity when the user taps the other person's name/avatar.
 *
 * Required intent extra: "user_id" (String)
 * Optional intent extras: "user_name", "user_avatar" (for instant header display before Firestore loads)
 */
public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";

    // Intent extra keys
    public static final String EXTRA_USER_ID     = "user_id";
    public static final String EXTRA_USER_NAME   = "user_name";
    public static final String EXTRA_USER_AVATAR = "user_avatar";

    private ImageView ivProfileAvatar;
    private TextView tvUserName;
    private TextView tvUniversity;
    private TextView tvStudentId;
    private TextView tvListingsCount;
    private TextView tvRating;
    private TextView tvReviewCount;
    private TextView tvAverageRating;
    private RatingBar ratingBarAverage;
    private TextView tvTotalReviews;
    private RecyclerView rvReviews;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    private OrderRepository orderRepository;
    private ReviewAdapter reviewAdapter;
    private final List<Review> reviewList = new ArrayList<>();

    private String targetUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        orderRepository = new OrderRepository();

        targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        String preloadName   = getIntent().getStringExtra(EXTRA_USER_NAME);
        String preloadAvatar = getIntent().getStringExtra(EXTRA_USER_AVATAR);

        initViews(preloadName, preloadAvatar);

        if (targetUserId == null || targetUserId.isEmpty()) {
            Log.e(TAG, "No user_id passed — finishing");
            finish();
            return;
        }

        loadUserData();
        loadStats();
        loadReviews();
    }

    private void initViews(String preloadName, String preloadAvatar) {
        ivProfileAvatar  = findViewById(R.id.ivProfileAvatar);
        tvUserName       = findViewById(R.id.tvUserName);
        tvUniversity     = findViewById(R.id.tvUniversity);
        tvStudentId      = findViewById(R.id.tvStudentId);
        tvListingsCount  = findViewById(R.id.tvListingsCount);
        tvRating         = findViewById(R.id.tvRating);
        tvReviewCount    = findViewById(R.id.tvReviewCount);
        tvAverageRating  = findViewById(R.id.tvAverageRating);
        ratingBarAverage = findViewById(R.id.ratingBarAverage);
        tvTotalReviews   = findViewById(R.id.tvTotalReviews);
        rvReviews        = findViewById(R.id.rvReviews);
        emptyState       = findViewById(R.id.emptyState);
        progressBar      = findViewById(R.id.progressBar);

        // Status bar spacer
        View statusBarSpacer = findViewById(R.id.statusBarSpacer);
        if (statusBarSpacer != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                statusBarSpacer.setOnApplyWindowInsetsListener((v, insets) -> {
                    int h = insets.getInsets(WindowInsets.Type.statusBars()).top;
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    lp.height = h;
                    v.setLayoutParams(lp);
                    return insets;
                });
            } else {
                statusBarSpacer.setOnApplyWindowInsetsListener((v, insets) -> {
                    int h = insets.getSystemWindowInsetTop();
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    lp.height = h;
                    v.setLayoutParams(lp);
                    return insets;
                });
            }
        }

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        // Pre-populate with data passed from ChatActivity for instant display
        if (preloadName != null && !preloadName.isEmpty()) {
            tvUserName.setText(preloadName);
        }
        if (preloadAvatar != null && !preloadAvatar.isEmpty()) {
            Glide.with(this)
                    .load(preloadAvatar)
                    .placeholder(R.drawable.bg_avatar_placeholder)
                    .circleCrop()
                    .into(ivProfileAvatar);
        }

        // Reviews RecyclerView
        reviewAdapter = new ReviewAdapter(this, reviewList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);
        rvReviews.setNestedScrollingEnabled(false);
    }

    private void loadUserData() {
        FirebaseFirestore db = FirebaseManager.getInstance().getFirestore();
        db.collection(Constants.COLLECTION_USERS).document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name       = doc.getString("name");
                    String email      = doc.getString("email");
                    String studentId  = doc.getString("studentId");
                    String avatarUrl  = doc.getString("profilePicture");
                    Double rating     = doc.getDouble("rating");
                    Long   revCount   = doc.getLong("reviewCount");

                    if (tvUserName != null && name != null && !name.isEmpty()) {
                        tvUserName.setText(name);
                    }
                    if (tvUniversity != null) {
                        tvUniversity.setText(email != null ? email : "");
                    }
                    if (tvStudentId != null) {
                        tvStudentId.setText(studentId != null && !studentId.isEmpty()
                                ? "ID: " + studentId : "");
                    }

                    // Rating in stats card
                    if (tvRating != null) {
                        tvRating.setText(rating != null && rating > 0
                                ? String.format(Locale.getDefault(), "%.1f", rating) : "—");
                    }
                    if (tvReviewCount != null) {
                        tvReviewCount.setText(revCount != null ? String.valueOf(revCount) : "0");
                    }

                    // Profile picture
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.bg_avatar_placeholder)
                                .circleCrop()
                                .into(ivProfileAvatar);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "loadUserData failed", e));
    }

    private void loadStats() {
        FirebaseFirestore db = FirebaseManager.getInstance().getFirestore();
        db.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("ownerId", targetUserId)
                .whereEqualTo("isAvailable", true)
                .get()
                .addOnSuccessListener(snap -> {
                    if (tvListingsCount != null) {
                        tvListingsCount.setText(String.valueOf(snap.size()));
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "loadStats listings failed", e));
    }

    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        rvReviews.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        orderRepository.getReviewsForUser(targetUserId, new OrderRepository.ReviewListCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (reviews == null || reviews.isEmpty()) {
                        showEmptyState();
                        return;
                    }

                    reviewList.clear();
                    reviewList.addAll(reviews);
                    reviewAdapter.notifyDataSetChanged();

                    // Average rating
                    float total = 0;
                    for (Review r : reviews) total += r.getRating();
                    float avg = total / reviews.size();

                    if (tvAverageRating != null) {
                        tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
                    }
                    if (ratingBarAverage != null) {
                        ratingBarAverage.setRating(avg);
                    }
                    if (tvTotalReviews != null) {
                        tvTotalReviews.setText(reviews.size() + " Review" + (reviews.size() == 1 ? "" : "s"));
                    }

                    rvReviews.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "loadReviews failed: " + error);
                    progressBar.setVisibility(View.GONE);
                    showEmptyState();
                });
            }
        });
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        rvReviews.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        if (tvAverageRating != null) tvAverageRating.setText("—");
        if (tvTotalReviews != null) tvTotalReviews.setText("0 Reviews");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
