package com.example.unikart.utils;

import java.util.Arrays;
import java.util.List;

public class Constants {

    // Firestore Collections
    public static final String COLLECTION_USERS    = "users";
    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_CHATS    = "chats";
    public static final String COLLECTION_MESSAGES = "messages";
    public static final String COLLECTION_ORDERS   = "orders";
    public static final String COLLECTION_REVIEWS  = "reviews";

    // Product Types
    public static final String PRODUCT_TYPE_BUY  = "BUY";
    public static final String PRODUCT_TYPE_RENT = "RENT";

    // Order Status — full lifecycle
    public static final String ORDER_STATUS_REQUESTED      = "REQUESTED";
    public static final String ORDER_STATUS_ACCEPTED       = "ACCEPTED";
    public static final String ORDER_STATUS_REJECTED       = "REJECTED";
    public static final String ORDER_STATUS_HANDED_OVER    = "HANDED_OVER";
    public static final String ORDER_STATUS_COMPLETED      = "COMPLETED";
    public static final String ORDER_STATUS_RETURN_PENDING = "RETURN_PENDING";
    public static final String ORDER_STATUS_RETURNED       = "RETURNED";

    // Product Categories
    public static final String CATEGORY_BOOKS       = "Books";
    public static final String CATEGORY_ELECTRONICS = "Electronics";
    public static final String CATEGORY_FURNITURE   = "Furniture";
    public static final String CATEGORY_CLOTHING    = "Clothing";
    public static final String CATEGORY_SPORTS      = "Sports";
    public static final String CATEGORY_OTHER       = "Other";

    public static final List<String> ALL_CATEGORIES = Arrays.asList(
            CATEGORY_BOOKS, CATEGORY_ELECTRONICS, CATEGORY_FURNITURE,
            CATEGORY_CLOTHING, CATEGORY_SPORTS, CATEGORY_OTHER
    );

    // Allowed University Email Domains
    public static final List<String> ALLOWED_EMAIL_DOMAINS = Arrays.asList(
            "@bmu.edu.in",
            "@student.bmu.edu.in",
            "@university.edu",
            "@college.edu"
    );

    // Storage Paths
    public static final String STORAGE_PRODUCTS = "product_images";

    // Demo Account
    public static final String DEMO_SELLER_EMAIL = "campus.store@bmu.edu.in";
    public static final String DEMO_SELLER_NAME  = "Campus Store";

    // Validation
    public static final int OTP_LENGTH        = 6;
    public static final int MIN_PRODUCT_PRICE = 1;
    public static final int MAX_PRODUCT_PRICE = 100000;

    private Constants() {}

    public static boolean isValidUniversityEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String lower = email.toLowerCase();
        for (String domain : ALLOWED_EMAIL_DOMAINS) {
            if (lower.endsWith(domain.toLowerCase())) return true;
        }
        return false;
    }

    /** Human-readable label for an order status. */
    public static String statusLabel(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case ORDER_STATUS_REQUESTED:      return "Requested";
            case ORDER_STATUS_ACCEPTED:       return "Accepted";
            case ORDER_STATUS_REJECTED:       return "Rejected";
            case ORDER_STATUS_HANDED_OVER:    return "Handed Over";
            case ORDER_STATUS_COMPLETED:      return "Completed";
            case ORDER_STATUS_RETURN_PENDING: return "Return Pending";
            case ORDER_STATUS_RETURNED:       return "Returned";
            default:                          return status;
        }
    }
}
