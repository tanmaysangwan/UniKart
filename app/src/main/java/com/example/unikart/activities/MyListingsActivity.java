package com.example.unikart.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.models.Product;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyListingsActivity extends AppCompatActivity {

    private static final String TAG = "MyListingsActivity";

    private RecyclerView rvListings;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private MyListingAdapter adapter;
    private final List<Product> products = new ArrayList<>();
    private SessionManager sessionManager;

    // Refresh after returning from EditListingActivity
    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadMyProducts();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_listings);

        sessionManager = new SessionManager(this);

        rvListings    = findViewById(R.id.rvListings);
        emptyState    = findViewById(R.id.emptyState);
        progressBar   = findViewById(R.id.progressBar);

        FrameLayout btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        MaterialButton btnAdd = findViewById(R.id.btnAddListing);
        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddProductActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        adapter = new MyListingAdapter(
                products,
                product -> openProductDetail(product),   // card tap → detail view
                product -> openEdit(product),            // edit button
                product -> confirmDelete(product)        // delete button
        );
        rvListings.setLayoutManager(new LinearLayoutManager(this));
        rvListings.setAdapter(adapter);

        loadMyProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning from ProductDetailActivity (in case edit happened there)
        loadMyProducts();
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private void loadMyProducts() {
        String uid = sessionManager.getUserId();
        if (uid == null) return;

        progressBar.setVisibility(View.VISIBLE);
        rvListings.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("ownerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    products.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        products.add(mapDoc(doc));
                    }
                    showResults();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Ordered query failed, trying fallback", e);
                    FirebaseFirestore.getInstance()
                            .collection(Constants.COLLECTION_PRODUCTS)
                            .whereEqualTo("ownerId", uid)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                products.clear();
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap2) {
                                    products.add(mapDoc(doc));
                                }
                                showResults();
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "loadMyProducts failed", e2);
                                progressBar.setVisibility(View.GONE);
                                emptyState.setVisibility(View.VISIBLE);
                            });
                });
    }

    private void showResults() {
        progressBar.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        if (products.isEmpty()) {
            rvListings.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvListings.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private Product mapDoc(com.google.firebase.firestore.QueryDocumentSnapshot doc) {
        Product p = new Product();
        p.setId(safe(doc, "productId", doc.getId()));
        p.setName(safe(doc, "title", "Unnamed"));
        p.setDescription(safe(doc, "description", ""));
        p.setType(safe(doc, "type", Constants.PRODUCT_TYPE_BUY));
        p.setCategory(safe(doc, "category", Constants.CATEGORY_OTHER));
        p.setCondition(safe(doc, "condition", "Good"));
        p.setImageUrl(safe(doc, "imageUrl", ""));
        Double price = doc.getDouble("price");
        p.setPrice(price != null ? price : 0.0);
        Long maxRent = doc.getLong("maxRentDays");
        p.setMaxRentDays(maxRent != null ? maxRent.intValue() : 0);
        return p;
    }

    private String safe(com.google.firebase.firestore.DocumentSnapshot doc, String field, String fallback) {
        String v = doc.getString(field);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void openProductDetail(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openEdit(Product product) {
        Intent intent = new Intent(this, EditListingActivity.class);
        intent.putExtra(EditListingActivity.EXTRA_PRODUCT_ID, product.getId());
        editLauncher.launch(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void confirmDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Listing")
                .setMessage("Delete \"" + product.getName() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteProduct(product))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct(Product product) {
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_PRODUCTS)
                .document(product.getId())
                .delete()
                .addOnSuccessListener(v -> {
                    Snackbar.make(rvListings, "Listing deleted", Snackbar.LENGTH_SHORT).show();
                    loadMyProducts();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rvListings, "Delete failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Adapter
    // ══════════════════════════════════════════════════════════════════════════

    static class MyListingAdapter extends RecyclerView.Adapter<MyListingAdapter.VH> {

        interface Action { void run(Product p); }

        private final List<Product> items;
        private final Action onTap, onEdit, onDelete;

        MyListingAdapter(List<Product> items, Action onTap, Action onEdit, Action onDelete) {
            this.items    = items;
            this.onTap    = onTap;
            this.onEdit   = onEdit;
            this.onDelete = onDelete;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_listing, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Product p = items.get(position);

            h.tvName.setText(p.getName());
            h.tvCategory.setText(Constants.categoryEmoji(p.getCategory()) + " " + p.getCategory());
            h.tvCondition.setText(p.getCondition());

            boolean isRent = Constants.PRODUCT_TYPE_RENT.equals(p.getType());
            if (isRent) {
                h.tvPrice.setText(String.format(Locale.getDefault(), "₹ %.0f/day", p.getPrice()));
                h.tvBadge.setText("FOR RENT");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_rent);
                h.tvBadge.setTextColor(h.itemView.getContext().getResources().getColor(R.color.badge_rent_text, null));
            } else {
                h.tvPrice.setText(String.format(Locale.getDefault(), "₹ %.0f", p.getPrice()));
                h.tvBadge.setText("FOR SALE");
                h.tvBadge.setBackgroundResource(R.drawable.bg_badge_sale);
                h.tvBadge.setTextColor(h.itemView.getContext().getResources().getColor(R.color.badge_buy_text, null));
            }

            String url = p.getImageUrl();
            if (url != null && url.startsWith("http")) {
                h.ivImage.setVisibility(View.VISIBLE);
                h.tvEmoji.setVisibility(View.GONE);
                Glide.with(h.itemView.getContext())
                        .load(url)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(h.ivImage);
            } else {
                h.ivImage.setVisibility(View.GONE);
                h.tvEmoji.setVisibility(View.VISIBLE);
                h.tvEmoji.setText(Constants.categoryEmoji(p.getCategory()));
            }

            h.itemView.setOnClickListener(v -> onTap.run(p));
            h.btnEdit.setOnClickListener(v -> onEdit.run(p));
            h.btnDelete.setOnClickListener(v -> onDelete.run(p));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvEmoji, tvName, tvBadge, tvCategory, tvPrice, tvCondition;
            MaterialButton btnEdit, btnDelete;

            VH(@NonNull View v) {
                super(v);
                ivImage     = v.findViewById(R.id.ivProductImage);
                tvEmoji     = v.findViewById(R.id.tvImageEmoji);
                tvName      = v.findViewById(R.id.tvProductName);
                tvBadge     = v.findViewById(R.id.tvBadge);
                tvCategory  = v.findViewById(R.id.tvCategory);
                tvPrice     = v.findViewById(R.id.tvPrice);
                tvCondition = v.findViewById(R.id.tvCondition);
                btnEdit     = v.findViewById(R.id.btnEdit);
                btnDelete   = v.findViewById(R.id.btnDelete);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
