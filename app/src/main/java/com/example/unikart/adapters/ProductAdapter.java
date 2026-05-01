package com.example.unikart.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.models.Product;
import com.example.unikart.utils.Constants;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> products;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(List<Product> products) {
        this.products = products;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        if (position < products.size()) {
            holder.bind(products.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvProductName;
        private final TextView tvPrice;
        private final TextView tvBadge;
        private final TextView tvSellerName;
        private final TextView tvSellerRating;
        private final ImageView ivProductImage;
        private final TextView tvImageEmoji;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvSellerName = itemView.findViewById(R.id.tvSellerName);
            tvSellerRating = itemView.findViewById(R.id.tvSellerRating);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvImageEmoji = itemView.findViewById(R.id.tvImageEmoji);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION && pos < products.size()) {
                    listener.onProductClick(products.get(pos));
                }
            });
        }

        public void bind(Product product) {
            if (product == null) return;

            // Name
            tvProductName.setText(product.getName() != null ? product.getName() : "Unnamed Product");

            // Price
            tvPrice.setText(String.format(Locale.getDefault(), "₹ %.0f", product.getPrice()));

            // Seller
            String seller = product.getSellerName() != null ? product.getSellerName() : "Unknown";
            tvSellerName.setText("by " + seller);

            // Badge — use Constants.PRODUCT_TYPE_BUY ("BUY") as the canonical check
            String type = product.getType();
            if (Constants.PRODUCT_TYPE_BUY.equals(type)) {
                tvBadge.setText("FOR SALE");
                tvBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_buy_text));
                Drawable bg = ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_badge_sale);
                tvBadge.setBackground(bg);
            } else {
                tvBadge.setText("FOR RENT");
                tvBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_rent_text));
                Drawable bg = ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_badge_rent);
                tvBadge.setBackground(bg);
            }

            // Image
            String imageUrl = product.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                ivProductImage.setVisibility(View.VISIBLE);
                tvImageEmoji.setVisibility(View.GONE);
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(ivProductImage);
            } else {
                ivProductImage.setVisibility(View.GONE);
                tvImageEmoji.setVisibility(View.VISIBLE);
                // Show category emoji
                tvImageEmoji.setText(getCategoryEmoji(product));
            }
            
            // Seller rating
            if (product.getSellerReviewCount() > 0) {
                tvSellerRating.setVisibility(View.VISIBLE);
                if (product.getSellerReviewCount() == 0) {
                    tvSellerRating.setText("⭐ New");
                } else {
                    tvSellerRating.setText(String.format(Locale.getDefault(), 
                            "⭐ %.1f (%d)", product.getSellerRating(), product.getSellerReviewCount()));
                }
            } else {
                tvSellerRating.setVisibility(View.GONE);
            }
        }

        private String getCategoryEmoji(Product product) {
            String cat = product.getCategory() != null ? product.getCategory() : "";
            switch (cat) {
                case Constants.CATEGORY_BOOKS:      return "📚";
                case Constants.CATEGORY_ELECTRONICS: return "💻";
                case Constants.CATEGORY_FURNITURE:  return "🪑";
                case Constants.CATEGORY_SPORTS:     return "⚽";
                case Constants.CATEGORY_CLOTHING:   return "👕";
                default:                            return "📦";
            }
        }
    }
}
