package com.example.unikart.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unikart.R;
import com.example.unikart.adapters.ReviewAdapter;
import com.example.unikart.firebase.OrderRepository;
import com.example.unikart.models.Review;
import com.example.unikart.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewsActivity extends AppCompatActivity {

    private static final String TAG = "ReviewsActivity";

    private View statusBarSpacer;
    private ImageButton btnBack;
    private TextView tvAverageRating;
    private RatingBar ratingBarAverage;
    private TextView tvTotalReviews;
    private RecyclerView rvReviews;
    private ProgressBar progressBar;
    private LinearLayout emptyState;

    private SessionManager sessionManager;
    private OrderRepository orderRepository;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        sessionManager = new SessionManager(this);
        orderRepository = new OrderRepository();

        initViews();
        setupWindowInsets();
        setupBackButton();
        setupRecyclerView();
        loadReviews();
    }

    private void initViews() {
        statusBarSpacer = findViewById(R.id.statusBarSpacer);
        btnBack = findViewById(R.id.btnBack);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        ratingBarAverage = findViewById(R.id.ratingBarAverage);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBarLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            statusBarSpacer.getLayoutParams().height = systemBars.top;
            statusBarSpacer.requestLayout();
            return insets;
        });
    }

    private void setupBackButton() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(this, reviewList);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);
    }

    private void loadReviews() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            showEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        rvReviews.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        orderRepository.getReviewsForUser(userId, new OrderRepository.ReviewListCallback() {
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

                    // Calculate average rating
                    float totalRating = 0;
                    for (Review r : reviews) {
                        totalRating += r.getRating();
                    }
                    float avgRating = totalRating / reviews.size();

                    tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", avgRating));
                    ratingBarAverage.setRating(avgRating);
                    tvTotalReviews.setText(String.format(Locale.getDefault(), "%d Reviews", reviews.size()));

                    rvReviews.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Failed to load reviews: " + error);
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
        
        // Set default values
        tvAverageRating.setText("0.0");
        ratingBarAverage.setRating(0);
        tvTotalReviews.setText("0 Reviews");
    }
}
