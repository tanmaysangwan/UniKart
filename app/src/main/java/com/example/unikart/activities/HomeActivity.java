package com.example.unikart.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.adapters.CategoryAdapter;
import com.example.unikart.adapters.ProductAdapter;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.firebase.ProductRepository;
import com.example.unikart.models.Category;
import com.example.unikart.models.FilterState;
import com.example.unikart.models.Product;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * HomeActivity - Main marketplace screen
 * 
 * Displays product feed with search, filtering, and category navigation.
 * Handles bottom navigation and user greeting.
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    // UI Components
    private RecyclerView rvCategories;
    private RecyclerView rvProducts;
    private BottomNavigationView bottomNavigation;
    private TextView tvGreeting;
    private TextView tvUserName;
    private View tvEmptyState;
    private ProgressBar progressBar;
    private EditText etSearch;

    // Data & Logic
    private SessionManager sessionManager;
    private ProductAdapter productAdapter;
    private ProductRepository productRepository;
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private FilterState filterState = new FilterState();
    private boolean isFirstLoad = true;

    // Android 13+ notification permission
    private final ActivityResultLauncher<String> notifPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    Log.d(TAG, "POST_NOTIFICATIONS permission " + (granted ? "granted" : "denied")));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        productRepository = new ProductRepository();

        requestNotificationPermissionIfNeeded();
        initViews();
        setupGreeting();
        setupRecyclerViews();
        setupSearch();
        setupBottomNavigation();
        loadProducts();
    }

    /**
     * Requests POST_NOTIFICATIONS permission on Android 13+ (API 33).
     * On older versions the permission is granted automatically.
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Initialize all view references
     */
    private void initViews() {
        rvCategories = findViewById(R.id.rvCategories);
        rvProducts = findViewById(R.id.rvProducts);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvUserName = findViewById(R.id.tvUserName);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);
        etSearch = findViewById(R.id.etSearch);

        View btnFilter = findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterSheet());
        }
    }

    /**
     * Setup personalized greeting based on time of day
     */
    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        tvGreeting.setText(greeting);
        
        String name = sessionManager.getUserName();
        tvUserName.setText(name != null && !name.isEmpty() ? name : "Student");
        
        loadProfilePicture();
    }
    
    /**
     * Load user profile picture from Firestore
     */
    private void loadProfilePicture() {
        ImageView ivAvatar = findViewById(R.id.ivAvatar);
        if (ivAvatar == null) return;
        
        String uid = sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;
        
        FirebaseFirestore db = FirebaseManager.getInstance().getFirestore();
        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String profilePicUrl = doc.getString("profilePicture");
                        if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profilePicUrl)
                                    .placeholder(R.drawable.bg_avatar_placeholder)
                                    .circleCrop()
                                    .into(ivAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to load profile picture", e));
    }

    /**
     * Setup category horizontal scroll and product grid
     */
    private void setupRecyclerViews() {
        // Categories
        rvCategories.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Build category list directly from Constants — single source of truth
        List<Category> categoryList = new ArrayList<>();
        for (String name : Constants.ALL_CATEGORIES) {
            categoryList.add(new Category(name, name, Constants.categoryEmoji(name)));
        }
        CategoryAdapter categoryAdapter = new CategoryAdapter(categoryList);
        categoryAdapter.setOnCategoryClickListener(category -> {
            if (category.getName().equals(filterState.getCategoryFilter())) {
                filterState.setCategoryFilter(null);
            } else {
                filterState.setCategoryFilter(category.getName());
            }
            applyFilters();
        });
        rvCategories.setAdapter(categoryAdapter);

        // Products
        rvProducts.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        rvProducts.setNestedScrollingEnabled(false);
        productAdapter = new ProductAdapter(filteredProducts);
        productAdapter.setOnProductClickListener(this::openProductDetail);
        rvProducts.setAdapter(productAdapter);
    }

    /**
     * Setup real-time search functionality
     */
    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterState.setSearchQuery(s.toString().trim());
                applyFilters();
            }
        });
    }

    /**
     * Show filter bottom sheet with all filter options
     */
    private void showFilterSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);
        sheet.setContentView(view);

        // Initialize filter controls
        RadioGroup rgType = view.findViewById(R.id.rgType);
        RadioButton rbAll = view.findViewById(R.id.rbAll);
        RadioButton rbBuy = view.findViewById(R.id.rbBuy);
        RadioButton rbRent = view.findViewById(R.id.rbRent);

        RadioGroup rgSort = view.findViewById(R.id.rgSort);
        RadioButton rbSortNone = view.findViewById(R.id.rbSortNone);
        RadioButton rbSortLow = view.findViewById(R.id.rbSortLow);
        RadioButton rbSortHigh = view.findViewById(R.id.rbSortHigh);
        RadioButton rbSortRatingHigh = view.findViewById(R.id.rbSortRatingHigh);
        RadioButton rbSortRatingLow = view.findViewById(R.id.rbSortRatingLow);
        RadioButton rbSortMostReviewed = view.findViewById(R.id.rbSortMostReviewed);
        RadioButton rbSortRecommended = view.findViewById(R.id.rbSortRecommended);
        
        RadioGroup rgRating = view.findViewById(R.id.rgRating);
        RadioButton rbRatingAll = view.findViewById(R.id.rbRatingAll);
        RadioButton rbRatingHighly = view.findViewById(R.id.rbRatingHighly);
        RadioButton rbRatingGood = view.findViewById(R.id.rbRatingGood);
        RadioButton rbRatingTrusted = view.findViewById(R.id.rbRatingTrusted);
        RadioButton rbRatingNew = view.findViewById(R.id.rbRatingNew);

        ChipGroup cgCategory = view.findViewById(R.id.cgCategory);
        ChipGroup cgCondition = view.findViewById(R.id.cgCondition);

        MaterialButton btnApply = view.findViewById(R.id.btnApplyFilter);
        MaterialButton btnReset = view.findViewById(R.id.btnResetFilter);

        // Restore current filter state
        restoreFilterState(rbAll, rbBuy, rbRent, rbSortNone, rbSortLow, rbSortHigh,
                rbSortRatingHigh, rbSortRatingLow, rbSortMostReviewed, rbSortRecommended,
                rbRatingAll, rbRatingHighly, rbRatingGood, rbRatingTrusted, rbRatingNew);

        // Setup category chips
        for (String cat : Constants.ALL_CATEGORIES) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setChecked(cat.equals(filterState.getCategoryFilter()));
            cgCategory.addView(chip);
        }

        // Setup condition chips
        String[] conditions = {"Like New", "Good", "Fair"};
        for (String cond : conditions) {
            Chip chip = new Chip(this);
            chip.setText(cond);
            chip.setCheckable(true);
            chip.setChecked(cond.equals(filterState.getConditionFilter()));
            cgCondition.addView(chip);
        }

        // Apply button
        btnApply.setOnClickListener(v -> {
            applyFilterSelections(rgType, rgSort, rgRating, cgCategory, cgCondition);
            applyFilters();
            sheet.dismiss();
        });

        // Reset button
        btnReset.setOnClickListener(v -> {
            filterState.reset();
            if (etSearch != null) etSearch.setText("");
            applyFilters();
            sheet.dismiss();
        });

        sheet.show();
    }

    /**
     * Restore current filter state to UI controls
     */
    private void restoreFilterState(RadioButton rbAll, RadioButton rbBuy, RadioButton rbRent,
                                    RadioButton rbSortNone, RadioButton rbSortLow, RadioButton rbSortHigh,
                                    RadioButton rbSortRatingHigh, RadioButton rbSortRatingLow,
                                    RadioButton rbSortMostReviewed, RadioButton rbSortRecommended,
                                    RadioButton rbRatingAll, RadioButton rbRatingHighly,
                                    RadioButton rbRatingGood, RadioButton rbRatingTrusted, RadioButton rbRatingNew) {
        // Type filter
        if (Constants.PRODUCT_TYPE_BUY.equals(filterState.getTypeFilter())) rbBuy.setChecked(true);
        else if (Constants.PRODUCT_TYPE_RENT.equals(filterState.getTypeFilter())) rbRent.setChecked(true);
        else rbAll.setChecked(true);

        // Sort order
        switch (filterState.getSortOrder()) {
            case PRICE_LOW_HIGH: rbSortLow.setChecked(true); break;
            case PRICE_HIGH_LOW: rbSortHigh.setChecked(true); break;
            case RATING_HIGH_LOW: rbSortRatingHigh.setChecked(true); break;
            case RATING_LOW_HIGH: rbSortRatingLow.setChecked(true); break;
            case MOST_REVIEWED: rbSortMostReviewed.setChecked(true); break;
            case RECOMMENDED: rbSortRecommended.setChecked(true); break;
            default: rbSortNone.setChecked(true); break;
        }
        
        // Rating filter
        switch (filterState.getRatingFilter()) {
            case HIGHLY_RATED: rbRatingHighly.setChecked(true); break;
            case GOOD_SELLERS: rbRatingGood.setChecked(true); break;
            case TRUSTED_SELLERS: rbRatingTrusted.setChecked(true); break;
            case NEW_SELLERS: rbRatingNew.setChecked(true); break;
            default: rbRatingAll.setChecked(true); break;
        }
    }

    /**
     * Apply selected filters from bottom sheet to filter state
     */
    private void applyFilterSelections(RadioGroup rgType, RadioGroup rgSort, RadioGroup rgRating,
                                       ChipGroup cgCategory, ChipGroup cgCondition) {
        // Type filter
        int typeId = rgType.getCheckedRadioButtonId();
        if (typeId == R.id.rbBuy) filterState.setTypeFilter(Constants.PRODUCT_TYPE_BUY);
        else if (typeId == R.id.rbRent) filterState.setTypeFilter(Constants.PRODUCT_TYPE_RENT);
        else filterState.setTypeFilter(null);

        // Sort order
        int sortId = rgSort.getCheckedRadioButtonId();
        if (sortId == R.id.rbSortLow) filterState.setSortOrder(FilterState.SortOrder.PRICE_LOW_HIGH);
        else if (sortId == R.id.rbSortHigh) filterState.setSortOrder(FilterState.SortOrder.PRICE_HIGH_LOW);
        else if (sortId == R.id.rbSortRatingHigh) filterState.setSortOrder(FilterState.SortOrder.RATING_HIGH_LOW);
        else if (sortId == R.id.rbSortRatingLow) filterState.setSortOrder(FilterState.SortOrder.RATING_LOW_HIGH);
        else if (sortId == R.id.rbSortMostReviewed) filterState.setSortOrder(FilterState.SortOrder.MOST_REVIEWED);
        else if (sortId == R.id.rbSortRecommended) filterState.setSortOrder(FilterState.SortOrder.RECOMMENDED);
        else filterState.setSortOrder(FilterState.SortOrder.NONE);
        
        // Rating filter
        int ratingId = rgRating.getCheckedRadioButtonId();
        if (ratingId == R.id.rbRatingHighly) filterState.setRatingFilter(FilterState.RatingFilter.HIGHLY_RATED);
        else if (ratingId == R.id.rbRatingGood) filterState.setRatingFilter(FilterState.RatingFilter.GOOD_SELLERS);
        else if (ratingId == R.id.rbRatingTrusted) filterState.setRatingFilter(FilterState.RatingFilter.TRUSTED_SELLERS);
        else if (ratingId == R.id.rbRatingNew) filterState.setRatingFilter(FilterState.RatingFilter.NEW_SELLERS);
        else filterState.setRatingFilter(FilterState.RatingFilter.ALL);

        // Category filter
        int catChipId = cgCategory.getCheckedChipId();
        if (catChipId != View.NO_ID) {
            Chip c = cgCategory.findViewById(catChipId);
            filterState.setCategoryFilter(c != null ? c.getText().toString() : null);
        } else {
            filterState.setCategoryFilter(null);
        }

        // Condition filter
        int condChipId = cgCondition.getCheckedChipId();
        if (condChipId != View.NO_ID) {
            Chip c = cgCondition.findViewById(condChipId);
            filterState.setConditionFilter(c != null ? c.getText().toString() : null);
        } else {
            filterState.setConditionFilter(null);
        }
    }

    /**
     * Apply all active filters and sorting to product list
     */
    private void applyFilters() {
        List<Product> result = new ArrayList<>();
        String query = filterState.getSearchQuery();

        // Filter products
        for (Product p : allProducts) {
            if (!matchesFilters(p, query)) continue;
            result.add(p);
        }

        // Sort products
        sortProducts(result);

        // Update UI
        filteredProducts.clear();
        filteredProducts.addAll(result);
        productAdapter.notifyDataSetChanged();

        tvEmptyState.setVisibility(filteredProducts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Check if product matches all active filters
     */
    private boolean matchesFilters(Product p, String query) {
        // Search query
        if (query != null && !query.isEmpty()) {
            boolean matchTitle = p.getName().toLowerCase().contains(query.toLowerCase());
            boolean matchCat = p.getCategory().toLowerCase().contains(query.toLowerCase());
            if (!matchTitle && !matchCat) return false;
        }
        
        // Type filter
        if (filterState.getTypeFilter() != null && !filterState.getTypeFilter().equals(p.getType())) {
            return false;
        }
        
        // Category filter
        if (filterState.getCategoryFilter() != null && !filterState.getCategoryFilter().equals(p.getCategory())) {
            return false;
        }
        
        // Condition filter
        if (filterState.getConditionFilter() != null && !filterState.getConditionFilter().equals(p.getCondition())) {
            return false;
        }
        
        // Rating filter
        FilterState.RatingFilter ratingFilter = filterState.getRatingFilter();
        if (ratingFilter != FilterState.RatingFilter.ALL) {
            switch (ratingFilter) {
                case HIGHLY_RATED:
                    if (p.getSellerRating() < 4.5) return false;
                    break;
                case GOOD_SELLERS:
                    if (p.getSellerRating() < 4.0) return false;
                    break;
                case TRUSTED_SELLERS:
                    if (p.getSellerReviewCount() < 10) return false;
                    break;
                case NEW_SELLERS:
                    if (p.getSellerReviewCount() > 0) return false;
                    break;
            }
        }

        return true;
    }

    /**
     * Sort products based on selected sort order
     */
    private void sortProducts(List<Product> products) {
        FilterState.SortOrder sortOrder = filterState.getSortOrder();
        switch (sortOrder) {
            case PRICE_LOW_HIGH:
                Collections.sort(products, (a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                break;
            case PRICE_HIGH_LOW:
                Collections.sort(products, (a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
            case RATING_HIGH_LOW:
                Collections.sort(products, (a, b) -> Double.compare(b.getSellerRating(), a.getSellerRating()));
                break;
            case RATING_LOW_HIGH:
                Collections.sort(products, (a, b) -> Double.compare(a.getSellerRating(), b.getSellerRating()));
                break;
            case MOST_REVIEWED:
                Collections.sort(products, (a, b) -> Integer.compare(b.getSellerReviewCount(), a.getSellerReviewCount()));
                break;
            case RECOMMENDED:
                // Weighted score: rating * log(reviews + 1)
                Collections.sort(products, (a, b) -> {
                    double scoreA = a.getSellerRating() * Math.log(a.getSellerReviewCount() + 1);
                    double scoreB = b.getSellerRating() * Math.log(b.getSellerReviewCount() + 1);
                    return Double.compare(scoreB, scoreA);
                });
                break;
        }
    }

    /**
     * Load all products from Firestore
     */
    private void loadProducts() {
        showLoading(true);
        tvEmptyState.setVisibility(View.GONE);

        productRepository.getAllProducts(new ProductRepository.ProductListCallback() {
            @Override
            public void onSuccess(List<Product> products) {
                showLoading(false);

                // Silently purge any leftover Campus Store seed listings
                List<Product> clean = new ArrayList<>();
                for (Product p : products) {
                    if ("Campus Store".equals(p.getSellerName())) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection(com.example.unikart.utils.Constants.COLLECTION_PRODUCTS)
                                .document(p.getId())
                                .delete();
                    } else {
                        clean.add(p);
                    }
                }

                allProducts.clear();
                allProducts.addAll(clean);
                applyFilters();
                Log.d(TAG, "Loaded " + clean.size() + " products (purged " + (products.size() - clean.size()) + " seed items)");
            }

            @Override
            public void onFailure(String error) {
                showLoading(false);
                Log.e(TAG, "loadProducts failed: " + error);
                if (allProducts.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Navigate to product detail screen
     */
    private void openProductDetail(Product product) {
        if (product == null || product.getId().isEmpty()) return;
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Setup bottom navigation bar
     */
    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
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
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        
        // Refresh products on resume (except first load)
        if (!isFirstLoad) {
            productRepository.getAllProducts(new ProductRepository.ProductListCallback() {
                @Override
                public void onSuccess(List<Product> products) {
                    allProducts.clear();
                    allProducts.addAll(products);
                    applyFilters();
                }
                @Override public void onFailure(String error) {
                    Log.w(TAG, "onResume refresh failed: " + error);
                }
            });
        }
        isFirstLoad = false;
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}
