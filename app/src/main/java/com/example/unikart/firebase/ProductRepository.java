package com.example.unikart.firebase;

import android.util.Log;

import com.example.unikart.models.Product;
import com.example.unikart.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;
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

    public interface ProductDetailCallback {
        void onSuccess(Product product);
        void onFailure(String error);
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

    // ─── Get All Products ─────────────────────────────────────────────────────

    public void getAllProducts(ProductListCallback callback) {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("isAvailable", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Product> products = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            products.add(mapDocToProduct(doc));
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping malformed product doc: " + doc.getId(), e);
                        }
                    }
                    Log.d(TAG, "Loaded " + products.size() + " products");
                    enrichProductsWithSellerRatings(products, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllProducts failed", e);
                    if (e.getMessage() != null && e.getMessage().contains("index")) {
                        getAllProductsFallback(callback);
                    } else {
                        callback.onFailure("Could not load products: " + e.getMessage());
                    }
                });
    }

    private void getAllProductsFallback(ProductListCallback callback) {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("isAvailable", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Product> products = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            products.add(mapDocToProduct(doc));
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping doc: " + doc.getId(), e);
                        }
                    }
                    enrichProductsWithSellerRatings(products, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllProductsFallback failed", e);
                    callback.onFailure("Could not load products");
                });
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
                                    p.setSellerRating(rating != null ? rating : 0.0);
                                    p.setSellerReviewCount(reviewCount != null ? reviewCount.intValue() : 0);
                                }
                            }
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) callback.onSuccess(products);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to load seller data for " + sellerId, e);
                        remaining[0]--;
                        if (remaining[0] == 0) callback.onSuccess(products);
                    });
        }
    }

    // ─── Get Product By ID ────────────────────────────────────────────────────

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
        
        Boolean available = doc.getBoolean("available");
        p.setAvailable(available != null ? available : true); // default true for old products

        return p;
    }

    private String safeString(com.google.firebase.firestore.DocumentSnapshot doc, String field, String fallback) {
        String val = doc.getString(field);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }
}
