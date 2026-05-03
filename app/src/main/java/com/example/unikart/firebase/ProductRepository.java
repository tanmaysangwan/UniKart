package com.example.unikart.firebase;

import android.util.Log;

import com.example.unikart.models.Product;
import com.example.unikart.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductRepository {

    private static final String TAG = "ProductRepository";

    private final FirebaseFirestore firestore;
    private final FirebaseManager firebaseManager;

    public interface ProductCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface ProductListCallback {
        void onSuccess(List<Product> products);
        void onFailure(String error);
    }

    public interface ProductListListener {
        void onProducts(List<Product> products);
        void onError(String error);
    }

    public interface ProductDetailCallback {
        void onSuccess(Product product);
        void onFailure(String error);
    }

    public interface ProductDetailListener {
        void onProduct(Product product);
        void onError(String error);
    }

    public ProductRepository() {
        firebaseManager = FirebaseManager.getInstance();
        this.firestore = firebaseManager.getFirestore();
    }

    // ─── Add Product ──────────────────────────────────────────────────────────

    public void addProduct(String title, String description, double price, String type,
                           String category, String condition, String imageUrl, int maxRentDays,
                           ProductCallback callback) {

        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onFailure("Not logged in");
            return;
        }

        String productId = firestore.collection(Constants.COLLECTION_PRODUCTS).document().getId();

        Map<String, Object> productData = new HashMap<>();
        productData.put("productId", productId);
        productData.put("ownerId", currentUserId);
        productData.put("title", title != null ? title : "");
        productData.put("description", description != null ? description : "");
        productData.put("price", price);
        productData.put("type", type != null ? type : Constants.PRODUCT_TYPE_BUY);
        productData.put("category", category != null ? category : Constants.CATEGORY_OTHER);
        productData.put("condition", condition != null ? condition : "Good");
        productData.put("imageUrl", imageUrl != null ? imageUrl : "");
        productData.put("available", true);
        productData.put("createdAt", System.currentTimeMillis());
        if (Constants.PRODUCT_TYPE_RENT.equals(type) && maxRentDays > 0) {
            productData.put("maxRentDays", maxRentDays);
        }

        firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    String ownerName = doc.getString("name");
                    productData.put("ownerName", ownerName != null ? ownerName : "Unknown");

                    firestore.collection(Constants.COLLECTION_PRODUCTS)
                            .document(productId)
                            .set(productData)
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "Product added: " + productId);
                                callback.onSuccess("Product listed successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "addProduct write failed", e);
                                callback.onFailure("Failed to save product: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Could not fetch user name, using fallback", e);
                    productData.put("ownerName", "Unknown");
                    firestore.collection(Constants.COLLECTION_PRODUCTS)
                            .document(productId)
                            .set(productData)
                            .addOnSuccessListener(v -> callback.onSuccess("Product listed successfully"))
                            .addOnFailureListener(e2 -> callback.onFailure("Failed to save product: " + e2.getMessage()));
                });
    }

    // ─── Get All Products (One-time fetch - deprecated, use listener) ────────

    public void getAllProducts(ProductListCallback callback) {
        // Fetch all products and filter available ones in code
        // This handles both "available" and legacy "isAvailable" fields
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Product> products = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Product product = mapDocToProduct(doc);
                            // Only include available products
                            if (product.isAvailable()) {
                                products.add(product);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping malformed product doc: " + doc.getId(), e);
                        }
                    }
                    Log.d(TAG, "Loaded " + products.size() + " available products");
                    enrichProductsWithSellerRatings(products, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllProducts failed", e);
                    callback.onFailure("Could not load products: " + e.getMessage());
                });
    }

    // ─── Listen to All Products (Real-time) ──────────────────────────────────

    /**
     * Attaches a real-time listener to all available products.
     * Returns ListenerRegistration so caller can remove it in onStop/onDestroy.
     */
    public ListenerRegistration listenToAllProducts(ProductListListener listener) {
        return firestore.collection(Constants.COLLECTION_PRODUCTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenToAllProducts error", error);
                        listener.onError("Could not load products: " + error.getMessage());
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<Product> products = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Product product = mapDocToProduct(doc);
                            // Only include available products
                            if (product.isAvailable()) {
                                products.add(product);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping malformed product doc: " + doc.getId(), e);
                        }
                    }
                    Log.d(TAG, "Real-time update: " + products.size() + " available products");
                    enrichProductsWithSellerRatingsForListener(products, listener);
                });
    }

    private void enrichProductsWithSellerRatingsForListener(List<Product> products, ProductListListener listener) {
        if (products.isEmpty()) {
            listener.onProducts(products);
            return;
        }

        Map<String, List<Product>> productsBySeller = new HashMap<>();
        for (Product p : products) {
            String sellerId = p.getSellerId();
            if (sellerId != null && !sellerId.isEmpty()) {
                if (!productsBySeller.containsKey(sellerId)) {
                    productsBySeller.put(sellerId, new ArrayList<>());
                }
                productsBySeller.get(sellerId).add(p);
            }
        }

        if (productsBySeller.isEmpty()) {
            listener.onProducts(products);
            return;
        }

        final int[] remaining = {productsBySeller.size()};
        for (String sellerId : productsBySeller.keySet()) {
            firestore.collection(Constants.COLLECTION_USERS)
                    .document(sellerId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Double rating = doc.getDouble("rating");
                            Long reviewCount = doc.getLong("reviewCount");
                            List<Product> sellerProducts = productsBySeller.get(sellerId);
                            if (sellerProducts != null) {
                                for (Product p : sellerProducts) {
                                    int count = reviewCount != null ? reviewCount.intValue() : 0;
                                    if (count > 0 && rating != null && rating > 0) {
                                        p.setSellerRating(rating);
                                    } else {
                                        p.setSellerRating(0.0);
                                    }
                                    p.setSellerReviewCount(count);
                                }
                            }
                        } else {
                            List<Product> sellerProducts = productsBySeller.get(sellerId);
                            if (sellerProducts != null) {
                                for (Product p : sellerProducts) {
                                    p.setSellerRating(0.0);
                                    p.setSellerReviewCount(0);
                                }
                            }
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) listener.onProducts(products);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to load seller data for " + sellerId, e);
                        List<Product> sellerProducts = productsBySeller.get(sellerId);
                        if (sellerProducts != null) {
                            for (Product p : sellerProducts) {
                                p.setSellerRating(0.0);
                                p.setSellerReviewCount(0);
                            }
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) listener.onProducts(products);
                    });
        }
    }

    private void enrichProductsWithSellerRatings(List<Product> products, ProductListCallback callback) {
        if (products.isEmpty()) {
            callback.onSuccess(products);
            return;
        }

        Map<String, List<Product>> productsBySeller = new HashMap<>();
        for (Product p : products) {
            String sellerId = p.getSellerId();
            if (sellerId != null && !sellerId.isEmpty()) {
                if (!productsBySeller.containsKey(sellerId)) {
                    productsBySeller.put(sellerId, new ArrayList<>());
                }
                productsBySeller.get(sellerId).add(p);
            }
        }

        if (productsBySeller.isEmpty()) {
            callback.onSuccess(products);
            return;
        }

        final int[] remaining = {productsBySeller.size()};
        for (String sellerId : productsBySeller.keySet()) {
            firestore.collection(Constants.COLLECTION_USERS)
                    .document(sellerId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Double rating = doc.getDouble("rating");
                            Long reviewCount = doc.getLong("reviewCount");
                            List<Product> sellerProducts = productsBySeller.get(sellerId);
                            if (sellerProducts != null) {
                                for (Product p : sellerProducts) {
                                    int count = reviewCount != null ? reviewCount.intValue() : 0;
                                    // Only set rating if seller has reviews, otherwise force to 0
                                    if (count > 0 && rating != null && rating > 0) {
                                        p.setSellerRating(rating);
                                    } else {
                                        p.setSellerRating(0.0);
                                    }
                                    p.setSellerReviewCount(count);
                                }
                            }
                        } else {
                            // Seller document doesn't exist, set defaults
                            List<Product> sellerProducts = productsBySeller.get(sellerId);
                            if (sellerProducts != null) {
                                for (Product p : sellerProducts) {
                                    p.setSellerRating(0.0);
                                    p.setSellerReviewCount(0);
                                }
                            }
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) callback.onSuccess(products);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to load seller data for " + sellerId, e);
                        // On failure, set defaults to avoid showing incorrect ratings
                        List<Product> sellerProducts = productsBySeller.get(sellerId);
                        if (sellerProducts != null) {
                            for (Product p : sellerProducts) {
                                p.setSellerRating(0.0);
                                p.setSellerReviewCount(0);
                            }
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) callback.onSuccess(products);
                    });
        }
    }

    // ─── Get Product By ID (One-time fetch - deprecated, use listener) ───────

    public void getProductById(String productId, ProductDetailCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onFailure("Invalid product ID");
            return;
        }

        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        try {
                            callback.onSuccess(mapDocToProduct(doc));
                        } catch (Exception e) {
                            Log.e(TAG, "mapDocToProduct failed", e);
                            callback.onFailure("Error reading product data");
                        }
                    } else {
                        callback.onFailure("Product not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getProductById failed", e);
                    callback.onFailure("Could not load product: " + e.getMessage());
                });
    }

    // ─── Listen to Product By ID (Real-time) ─────────────────────────────────

    /**
     * Attaches a real-time listener to a specific product.
     * Returns ListenerRegistration so caller can remove it in onStop/onDestroy.
     */
    public ListenerRegistration listenToProductById(String productId, ProductDetailListener listener) {
        if (productId == null || productId.isEmpty()) {
            listener.onError("Invalid product ID");
            return null;
        }

        return firestore.collection(Constants.COLLECTION_PRODUCTS)
                .document(productId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenToProductById error", error);
                        listener.onError("Could not load product: " + error.getMessage());
                        return;
                    }
                    if (documentSnapshot == null || !documentSnapshot.exists()) {
                        listener.onError("Product not found");
                        return;
                    }

                    try {
                        Product product = mapDocToProduct(documentSnapshot);
                        listener.onProduct(product);
                    } catch (Exception e) {
                        Log.e(TAG, "mapDocToProduct failed", e);
                        listener.onError("Error reading product data");
                    }
                });
    }

    // ─── Create Request ───────────────────────────────────────────────────────

    public void createProductRequest(String productId, String sellerId, String type, ProductCallback callback) {
        String currentUserId = firebaseManager.getCurrentUserId();
        if (currentUserId == null) {
            callback.onFailure("Not logged in");
            return;
        }

        String orderId = firestore.collection(Constants.COLLECTION_ORDERS).document().getId();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("productId", productId != null ? productId : "");
        orderData.put("buyerId", currentUserId);
        orderData.put("sellerId", sellerId != null ? sellerId : "");
        orderData.put("type", type != null ? type : Constants.PRODUCT_TYPE_BUY);
        orderData.put("status", Constants.ORDER_STATUS_REQUESTED);
        orderData.put("requestedAt", System.currentTimeMillis());

        firestore.collection(Constants.COLLECTION_ORDERS)
                .document(orderId)
                .set(orderData)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Order created: " + orderId);
                    callback.onSuccess("Request sent successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createProductRequest failed", e);
                    callback.onFailure("Failed to send request: " + e.getMessage());
                });
    }

    // ─── Update Product Availability ──────────────────────────────────────────

    public void updateProductAvailability(String productId, boolean available, ProductCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("available", available);
        
        firestore.collection(Constants.COLLECTION_PRODUCTS).document(productId)
                .update(update)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Product " + productId + " availability → " + available);
                    callback.onSuccess("Product availability updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateProductAvailability failed", e);
                    callback.onFailure("Failed to update availability: " + e.getMessage());
                });
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Maps Firestore document to Product model.
     * Handles backward compatibility for "isAvailable" vs "available" field.
     */
    private Product mapDocToProduct(com.google.firebase.firestore.DocumentSnapshot doc) {
        Product p = new Product();
        p.setId(safeString(doc, "productId", doc.getId()));
        p.setName(safeString(doc, "title", "Unnamed Product"));
        p.setDescription(safeString(doc, "description", ""));
        p.setType(safeString(doc, "type", Constants.PRODUCT_TYPE_BUY));
        p.setCategory(safeString(doc, "category", Constants.CATEGORY_OTHER));
        p.setCondition(safeString(doc, "condition", "Good"));
        p.setSellerName(safeString(doc, "ownerName", "Unknown"));
        p.setSellerId(safeString(doc, "ownerId", ""));
        p.setImageUrl(safeString(doc, "imageUrl", ""));

        Double price = doc.getDouble("price");
        p.setPrice(price != null ? price : 0.0);

        Long maxRentDays = doc.getLong("maxRentDays");
        p.setMaxRentDays(maxRentDays != null ? maxRentDays.intValue() : 0);

        Long createdAt = doc.getLong("createdAt");
        p.setTimestamp(createdAt != null ? createdAt : 0L);
        
        // Handle both "available" (current) and "isAvailable" (legacy) fields
        Boolean available = doc.getBoolean("available");
        if (available == null) {
            available = doc.getBoolean("isAvailable");
        }
        // Default to true for old products without any availability field
        p.setAvailable(available != null ? available : true);

        return p;
    }

    private String safeString(com.google.firebase.firestore.DocumentSnapshot doc, String field, String fallback) {
        String val = doc.getString(field);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }
}
