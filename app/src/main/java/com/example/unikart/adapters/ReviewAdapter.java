package com.example.unikart.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.unikart.R;
import com.example.unikart.models.Review;
import com.example.unikart.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Review> reviews;

    public ReviewAdapter(Context context, List<Review> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);

        // Reviewer name
        holder.tvReviewerName.setText(review.getReviewerName());

        // Rating
        holder.ratingBar.setRating(review.getRating());
        holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", review.getRating()));

        // Comment
        if (review.getComment() != null && !review.getComment().isEmpty()) {
            holder.tvComment.setText(review.getComment());
            holder.tvComment.setVisibility(View.VISIBLE);
        } else {
            holder.tvComment.setVisibility(View.GONE);
        }

        // Product info
        if (review.getProductTitle() != null && !review.getProductTitle().isEmpty()) {
            holder.layoutProductInfo.setVisibility(View.VISIBLE);
            holder.tvProductTitle.setText(review.getProductTitle());
            
            // Transaction type badge
            String type = review.getTransactionType();
            if (type != null && !type.isEmpty()) {
                holder.tvTransactionType.setText(type);
                holder.tvTransactionType.setVisibility(View.VISIBLE);
                
                // Set appropriate background
                if (Constants.PRODUCT_TYPE_RENT.equals(type)) {
                    holder.tvTransactionType.setBackgroundResource(R.drawable.bg_badge_rent);
                } else {
                    holder.tvTransactionType.setBackgroundResource(R.drawable.bg_badge_sale);
                }
            } else {
                holder.tvTransactionType.setVisibility(View.GONE);
            }
        } else {
            holder.layoutProductInfo.setVisibility(View.GONE);
        }

        // Date
        if (review.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(review.getTimestamp()));
            holder.tvDate.setText(dateStr);
        } else {
            holder.tvDate.setText("");
        }

        // Profile picture
        if (review.getReviewerProfilePic() != null && !review.getReviewerProfilePic().isEmpty()) {
            Glide.with(context)
                    .load(review.getReviewerProfilePic())
                    .placeholder(R.drawable.bg_avatar_placeholder)
                    .circleCrop()
                    .into(holder.ivReviewerAvatar);
        } else {
            Glide.with(context)
                    .load(R.drawable.bg_avatar_placeholder)
                    .circleCrop()
                    .into(holder.ivReviewerAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView ivReviewerAvatar;
        TextView tvReviewerName;
        RatingBar ratingBar;
        TextView tvRating;
        TextView tvComment;
        LinearLayout layoutProductInfo;
        TextView tvProductTitle;
        TextView tvTransactionType;
        TextView tvDate;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReviewerAvatar = itemView.findViewById(R.id.ivReviewerAvatar);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvComment = itemView.findViewById(R.id.tvComment);
            layoutProductInfo = itemView.findViewById(R.id.layoutProductInfo);
            tvProductTitle = itemView.findViewById(R.id.tvProductTitle);
            tvTransactionType = itemView.findViewById(R.id.tvTransactionType);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
