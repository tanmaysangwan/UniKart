package com.example.unikart.models;

public class Review {
    private String reviewId;
    private String orderId;
    private String productId;
    private String productTitle;      // Added for display
    private String reviewerId;
    private String reviewerName;
    private String reviewerProfilePic; // Added for display
    private String revieweeId;         // person being reviewed
    private float rating;              // 1-5
    private String comment;
    private long timestamp;
    private String transactionType;    // Added: BUY or RENT

    public Review() {}

    public String getReviewId()             { return reviewId != null ? reviewId : ""; }
    public void setReviewId(String v)       { this.reviewId = v; }

    public String getOrderId()              { return orderId != null ? orderId : ""; }
    public void setOrderId(String v)        { this.orderId = v; }

    public String getProductId()            { return productId != null ? productId : ""; }
    public void setProductId(String v)      { this.productId = v; }

    public String getProductTitle()         { return productTitle != null ? productTitle : ""; }
    public void setProductTitle(String v)   { this.productTitle = v; }

    public String getReviewerId()           { return reviewerId != null ? reviewerId : ""; }
    public void setReviewerId(String v)     { this.reviewerId = v; }

    public String getReviewerName()         { return reviewerName != null ? reviewerName : ""; }
    public void setReviewerName(String v)   { this.reviewerName = v; }

    public String getReviewerProfilePic()   { return reviewerProfilePic != null ? reviewerProfilePic : ""; }
    public void setReviewerProfilePic(String v) { this.reviewerProfilePic = v; }

    public String getRevieweeId()           { return revieweeId != null ? revieweeId : ""; }
    public void setRevieweeId(String v)     { this.revieweeId = v; }

    public float getRating()                { return rating; }
    public void setRating(float v)          { this.rating = v; }

    public String getComment()              { return comment != null ? comment : ""; }
    public void setComment(String v)        { this.comment = v; }

    public long getTimestamp()              { return timestamp; }
    public void setTimestamp(long v)        { this.timestamp = v; }

    public String getTransactionType()      { return transactionType != null ? transactionType : ""; }
    public void setTransactionType(String v) { this.transactionType = v; }
}
