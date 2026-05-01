package com.example.unikart.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unikart.R;
import com.example.unikart.adapters.ReviewAdapter;
import com.example.unikart.firebase.OrderRepository;
import com.example.unikart.models.Review;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewsActivity extends AppCompatActivity {

    private static final String TAG = "ReviewsActivity";

    private MaterialToolbar toolbar;
    private TextView tvAverageRating;
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
        setupToolbar();
        setupRecyclerView();
        loadReviews();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvAverageRating = findViewById(R.id.tvAverageRating);
        tvTotalReviews = findViewById(R.id.tvTotalReviews);
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reviews & Ratings");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
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
        tvTotalReviews.setText("0 Reviews");
    }
}
