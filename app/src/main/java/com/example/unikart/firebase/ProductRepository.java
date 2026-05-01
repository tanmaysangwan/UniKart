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
    private static final String SEED_OWNER_ID = "campus_store_seed";
    private static final String SEED_OWNER_NAME = "Campus Store";

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

    // ─── Add Product ─────────────────────────────────────────────────────────

    public void addProduct(String title, String description, double price, String type,
                           String category, String condition, String imageUrl, ProductCallback callback) {

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
        productData.put("isAvailable", true);
        productData.put("createdAt", System.currentTimeMillis());

        // Fetch owner name, then write product
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
                    // Still save product even if user fetch fails
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
                            Product p = mapDocToProduct(doc);
                            products.add(p);
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping malformed product doc: " + doc.getId(), e);
                        }
                    }
                    Log.d(TAG, "Loaded " + products.size() + " products");
                    
                    // Load seller ratings for all products
                    enrichProductsWithSellerRatings(products, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAllProducts failed", e);
                    // If index error, try without orderBy
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
    
    /**
     * Enriches products with seller rating data by batch-loading unique sellers.
     * This avoids excessive Firestore calls by loading each seller only once.
     */
    private void enrichProductsWithSellerRatings(List<Product> products, ProductListCallback callback) {
        if (products.isEmpty()) {
            callback.onSuccess(products);
            return;
        }
        
        // Get unique seller IDs
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
        
        // Load seller data for unique sellers
        final int[] remaining = {productsBySeller.size()};
        for (String sellerId : productsBySeller.keySet()) {
            firestore.collection(Constants.COLLECTION_USERS)
                    .document(sellerId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Double rating = doc.getDouble("rating");
                            Long reviewCount = doc.getLong("reviewCount");
                            
                            // Apply rating to all products from this seller
                            List<Product> sellerProducts = productsBySeller.get(sellerId);
                            if (sellerProducts != null) {
                                for (Product p : sellerProducts) {
                                    p.setSellerRating(rating != null ? rating : 0.0);
                                    p.setSellerReviewCount(reviewCount != null ? reviewCount.intValue() : 0);
                                }
                            }
                        }
                        
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            callback.onSuccess(products);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Failed to load seller data for " + sellerId, e);
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            callback.onSuccess(products);
                        }
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

    // ─── Seed Marketplace Data ────────────────────────────────────────────────

    /**
     * Checks if products collection is empty. If so, seeds 12 sample products.
     * Safe to call on every launch — only seeds once.
     */
    public void seedMarketplaceIfEmpty(ProductCallback callback) {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d(TAG, "Products empty — seeding marketplace");
                        writeSeedProducts(callback);
                    } else {
                        Log.d(TAG, "Products already exist — skipping seed");
                        callback.onSuccess("already_seeded");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Seed check failed — skipping seed", e);
                    callback.onFailure(e.getMessage());
                });
    }

    private void writeSeedProducts(ProductCallback callback) {
        List<Map<String, Object>> seeds = buildSeedData();
        final int[] remaining = {seeds.size()};
        final boolean[] anyFailed = {false};

        for (Map<String, Object> product : seeds) {
            String productId = (String) product.get("productId");
            firestore.collection(Constants.COLLECTION_PRODUCTS)
                    .document(productId)
                    .set(product)
                    .addOnSuccessListener(v -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            Log.d(TAG, "Seed complete");
                            callback.onSuccess("seeded");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Seed write failed for " + productId, e);
                        anyFailed[0] = true;
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            callback.onSuccess("seeded_partial");
                        }
                    });
        }
    }

    private List<Map<String, Object>> buildSeedData() {
        List<Map<String, Object>> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        list.add(seed("Engineering Mathematics Book",
                "Complete textbook for B.Tech first year. Covers calculus, linear algebra, and differential equations. Lightly used, no missing pages.",
                380, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_BOOKS, "Good", 
                "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80", now - 1000));

        list.add(seed("Scientific Calculator (Casio FX-991)",
                "Casio FX-991EX ClassWiz. Works perfectly. Comes with original cover. Essential for engineering exams.",
                650, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Like New",
                "https://images.unsplash.com/photo-1611224923853-80b023f02d71?w=800&q=80", now - 2000));

        list.add(seed("Study Table",
                "Wooden study table, 4 feet wide. Sturdy and spacious. Ideal for hostel room. Self-pickup only.",
                1800, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_FURNITURE, "Good",
                "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800&q=80", now - 3000));

        list.add(seed("Office Chair",
                "Comfortable mesh back office chair with adjustable height. Used for one semester. No damage.",
                1200, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_FURNITURE, "Good",
                "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=800&q=80", now - 4000));

        list.add(seed("Monitor 24 inch (Full HD)",
                "Dell 24\" Full HD IPS monitor. 1080p, 60Hz. HDMI and VGA ports. Perfect for coding and study.",
                5500, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Good",
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&q=80", now - 5000));

        list.add(seed("Mechanical Keyboard",
                "Redragon K552 TKL mechanical keyboard. Blue switches. Great for typing and coding. Barely used.",
                1400, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Like New",
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80", now - 6000));

        list.add(seed("Bicycle (Hero Sprint)",
                "Hero Sprint 21-speed mountain bike. Good condition. Ideal for campus commute. Tyres recently changed.",
                3200, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_SPORTS, "Good",
                "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&q=80", now - 7000));

        list.add(seed("Lab Coat (Size M)",
                "White lab coat, size medium. Used for one semester of chemistry labs. Washed and clean.",
                250, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_CLOTHING, "Good",
                "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&q=80", now - 8000));

        list.add(seed("Laptop Backpack",
                "Wildcraft 30L backpack. Fits 15.6\" laptop. Multiple compartments. Waterproof base. Barely used.",
                900, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_OTHER, "Like New",
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&q=80", now - 9000));

        list.add(seed("Noise Cancelling Headphones",
                "boAt Rockerz 450 wireless headphones. 15-hour battery. Foldable design. Great for studying.",
                1100, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Good",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80", now - 10000));

        list.add(seed("Single Mattress",
                "5-inch foam mattress, single size (72x36 inches). Used for one year. Clean and comfortable.",
                1500, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_FURNITURE, "Good",
                "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=800&q=80", now - 11000));

        list.add(seed("Steel Water Bottle (1L)",
                "Milton Thermosteel 1-litre insulated bottle. Keeps water cold for 24 hours. No dents or leaks.",
                350, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_OTHER, "Like New",
                "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&q=80", now - 12000));

        return list;
    }

    private Map<String, Object> seed(String title, String description, double price,
                                     String type, String category, String condition, String imageUrl, long createdAt) {
        String productId = firestore.collection(Constants.COLLECTION_PRODUCTS).document().getId();
        Map<String, Object> m = new HashMap<>();
        m.put("productId", productId);
        m.put("ownerId", SEED_OWNER_ID);
        m.put("ownerName", SEED_OWNER_NAME);
        m.put("title", title);
        m.put("description", description);
        m.put("price", price);
        m.put("type", type);
        m.put("category", category);
        m.put("condition", condition);
        m.put("imageUrl", imageUrl != null ? imageUrl : "");
        m.put("isAvailable", true);
        m.put("createdAt", createdAt);
        return m;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

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

        Long createdAt = doc.getLong("createdAt");
        p.setTimestamp(createdAt != null ? createdAt : 0L);

        return p;
    }

    private String safeString(com.google.firebase.firestore.DocumentSnapshot doc, String field, String fallback) {
        String val = doc.getString(field);
        return (val != null && !val.isEmpty()) ? val : fallback;
    }

    /**
     * Rebuilds marketplace inventory:
     * 1. Repairs old seed product images
     * 2. Adds new diverse products if missing
     * 3. Avoids duplicates
     */
    public void rebuildMarketplaceInventory(ProductCallback callback) {
        Log.d(TAG, "Starting marketplace inventory rebuild...");
        
        // First, repair any existing products with missing images
        repairAllProductImages(() -> {
            // Then add new products if they don't exist
            addExpandedInventory(callback);
        });
    }
    
    private void repairAllProductImages(Runnable onComplete) {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    Map<String, String> imageMap = getProductImageMap();
                    final int[] remaining = {snapshot.size()};
                    final int[] updated = {0};

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        String title = doc.getString("title");
                        String currentImageUrl = doc.getString("imageUrl");

                        // Update if image is missing/empty/emoji and we have a real image
                        boolean needsUpdate = (currentImageUrl == null || currentImageUrl.isEmpty() || 
                                             currentImageUrl.startsWith("http") == false);
                        
                        if (needsUpdate && title != null && imageMap.containsKey(title)) {
                            Map<String, Object> update = new HashMap<>();
                            update.put("imageUrl", imageMap.get(title));

                            doc.getReference().update(update)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) updated[0]++;
                                        remaining[0]--;
                                        if (remaining[0] == 0) {
                                            Log.d(TAG, "Repaired " + updated[0] + " product images");
                                            onComplete.run();
                                        }
                                    });
                        } else {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                onComplete.run();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "repairAllProductImages failed", e);
                    onComplete.run(); // Continue anyway
                });
    }
    
    private void addExpandedInventory(ProductCallback callback) {
        // Check which products already exist by title
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Build set of existing titles
                    java.util.Set<String> existingTitles = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        String title = doc.getString("title");
                        if (title != null) existingTitles.add(title);
                    }

                    // Get expanded product list
                    List<Map<String, Object>> allProducts = buildExpandedInventory();
                    
                    // Filter to only new products
                    List<Map<String, Object>> newProducts = new ArrayList<>();
                    for (Map<String, Object> product : allProducts) {
                        String title = (String) product.get("title");
                        if (!existingTitles.contains(title)) {
                            newProducts.add(product);
                        }
                    }

                    if (newProducts.isEmpty()) {
                        callback.onSuccess("Inventory already complete (0 added)");
                        return;
                    }

                    // Write new products
                    final int[] remaining = {newProducts.size()};
                    final int[] added = {0};

                    for (Map<String, Object> product : newProducts) {
                        String productId = (String) product.get("productId");
                        firestore.collection(Constants.COLLECTION_PRODUCTS)
                                .document(productId)
                                .set(product)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) added[0]++;
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        Log.d(TAG, "Added " + added[0] + " new products");
                                        callback.onSuccess("Inventory rebuilt: " + added[0] + " products added");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addExpandedInventory failed", e);
                    callback.onFailure("Failed to expand inventory: " + e.getMessage());
                });
    }
    
    private Map<String, String> getProductImageMap() {
        Map<String, String> map = new HashMap<>();
        
        // Original 12 products
        map.put("Engineering Mathematics Book", "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80");
        map.put("Scientific Calculator (Casio FX-991)", "https://images.unsplash.com/photo-1611224923853-80b023f02d71?w=800&q=80");
        map.put("Study Table", "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800&q=80");
        map.put("Office Chair", "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=800&q=80");
        map.put("Monitor 24 inch (Full HD)", "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&q=80");
        map.put("Mechanical Keyboard", "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80");
        map.put("Bicycle (Hero Sprint)", "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&q=80");
        map.put("Lab Coat (Size M)", "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&q=80");
        map.put("Laptop Backpack", "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&q=80");
        map.put("Noise Cancelling Headphones", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80");
        map.put("Single Mattress", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=800&q=80");
        map.put("Steel Water Bottle (1L)", "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&q=80");
        
        // New expanded products
        map.put("DBMS Complete Guide", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&q=80");
        map.put("Java Programming Book", "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&q=80");
        map.put("Data Structures Notes", "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&q=80");
        map.put("Logitech Wireless Mouse", "https://images.unsplash.com/photo-1527814050087-3793815479db?w=800&q=80");
        map.put("RGB Gaming Keyboard", "https://images.unsplash.com/photo-1595225476474-87563907a212?w=800&q=80");
        map.put("Dell 27\" Monitor", "https://images.unsplash.com/photo-1585792180666-f7347c490ee2?w=800&q=80");
        map.put("Power Bank 20000mAh", "https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=800&q=80");
        map.put("Wireless Earbuds", "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=800&q=80");
        map.put("Bean Bag Chair", "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800&q=80");
        map.put("LED Study Lamp", "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&q=80");
        map.put("Foldable Study Table", "https://images.unsplash.com/photo-1595428774223-ef52624120d2?w=800&q=80");
        map.put("Nike Hoodie (Size L)", "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800&q=80");
        map.put("Adidas Running Shoes", "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80");
        map.put("College Backpack 40L", "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800&q=80");
        map.put("Cricket Bat (Kashmir Willow)", "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800&q=80");
        map.put("Football (Size 5)", "https://images.unsplash.com/photo-1614632537423-1e6c2e7e0aab?w=800&q=80");
        map.put("Dumbbells Set (5kg x 2)", "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80");
        map.put("Electric Kettle 1.5L", "https://images.unsplash.com/photo-1563822249366-3efbb5c8f9e6?w=800&q=80");
        map.put("Steam Iron", "https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=800&q=80");
        map.put("Clothes Hanger Set", "https://images.unsplash.com/photo-1582735689369-4fe89db7114c?w=800&q=80");
        map.put("Cotton Bedsheet Set", "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=80");
        map.put("Extension Board 6 Socket", "https://images.unsplash.com/photo-1591696331111-ef9586a5b17a?w=800&q=80");
        map.put("Graphing Calculator", "https://images.unsplash.com/photo-1587145820266-a5951ee6f620?w=800&q=80");
        map.put("Portable Whiteboard", "https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?w=800&q=80");
        map.put("Bicycle Helmet", "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80");
        map.put("USB-C Hub 7-in-1", "https://images.unsplash.com/photo-1625948515291-69613efd103f?w=800&q=80");
        map.put("Desk Organizer Set", "https://images.unsplash.com/photo-1611532736597-de2d4265fba3?w=800&q=80");
        map.put("Yoga Mat", "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&q=80");
        map.put("Bluetooth Speaker", "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&q=80");
        
        return map;
    }
    
    private List<Map<String, Object>> buildExpandedInventory() {
        List<Map<String, Object>> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        long offset = 13000; // Start after original 12 products

        // BOOKS (3 items)
        list.add(seed("DBMS Complete Guide",
                "Database Management Systems textbook by Ramakrishnan. Perfect for CS students. All chapters intact with highlighted notes.",
                420, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_BOOKS, "Good",
                "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&q=80", now - offset++));

        list.add(seed("Java Programming Book",
                "Head First Java 3rd Edition. Best book for learning Java. Minimal wear, no torn pages. Great for beginners.",
                480, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_BOOKS, "Like New",
                "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&q=80", now - offset++));

        list.add(seed("Data Structures Notes",
                "Complete handwritten notes for Data Structures course. Covers arrays, linked lists, trees, graphs, sorting algorithms.",
                150, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_BOOKS, "Good",
                "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&q=80", now - offset++));

        // ELECTRONICS (5 items)
        list.add(seed("Logitech Wireless Mouse",
                "Logitech M331 Silent Plus wireless mouse. 2-year battery life. Works perfectly. Comes with USB receiver.",
                550, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Like New",
                "https://images.unsplash.com/photo-1527814050087-3793815479db?w=800&q=80", now - offset++));

        list.add(seed("RGB Gaming Keyboard",
                "Cosmic Byte CB-GK-16 RGB mechanical keyboard. Red switches. Customizable lighting. Used for 6 months.",
                1800, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Good",
                "https://images.unsplash.com/photo-1595225476474-87563907a212?w=800&q=80", now - offset++));

        list.add(seed("Dell 27\" Monitor",
                "Dell 27-inch QHD monitor. 2560x1440 resolution. IPS panel. Perfect for design work and coding. Minimal backlight bleed.",
                8500, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_ELECTRONICS, "Good",
                "https://images.unsplash.com/photo-1585792180666-f7347c490ee2?w=800&q=80", now - offset++));

        list.add(seed("Power Bank 20000mAh",
                "Mi 20000mAh power bank. Fast charging support. Can charge phone 4-5 times. Barely used, like new condition.",
                1200, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Like New",
                "https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=800&q=80", now - offset++));

        list.add(seed("Wireless Earbuds",
                "boAt Airdopes 441 TWS earbuds. 5-hour playback. Touch controls. Charging case included. Works great.",
                1300, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Good",
                "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=800&q=80", now - offset++));

        // FURNITURE (3 items)
        list.add(seed("Bean Bag Chair",
                "Large bean bag filled with premium beans. Comfortable for studying or gaming. Blue color. Washable cover.",
                1600, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_FURNITURE, "Good",
                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800&q=80", now - offset++));

        list.add(seed("LED Study Lamp",
                "Adjustable LED desk lamp. 3 brightness levels. Eye-friendly warm light. USB powered. Perfect for late-night study.",
                450, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_FURNITURE, "Like New",
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&q=80", now - offset++));

        list.add(seed("Foldable Study Table",
                "Portable foldable laptop table. Height adjustable. Can be used on bed or floor. Lightweight and sturdy.",
                850, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_FURNITURE, "Good",
                "https://images.unsplash.com/photo-1595428774223-ef52624120d2?w=800&q=80", now - offset++));

        // CLOTHING (3 items)
        list.add(seed("Nike Hoodie (Size L)",
                "Nike Sportswear Club hoodie, size large. Black color. Soft fleece inside. Worn only a few times. Perfect for winter.",
                1400, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_CLOTHING, "Like New",
                "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800&q=80", now - offset++));

        list.add(seed("Adidas Running Shoes",
                "Adidas Ultraboost running shoes, size 9 UK. Barely used. Excellent cushioning. Great for jogging and gym.",
                3200, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_CLOTHING, "Like New",
                "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80", now - offset++));

        list.add(seed("College Backpack 40L",
                "American Tourister 40L backpack. Multiple compartments. Laptop sleeve. Rain cover included. Used for one semester.",
                1100, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_CLOTHING, "Good",
                "https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800&q=80", now - offset++));

        // SPORTS (3 items)
        list.add(seed("Cricket Bat (Kashmir Willow)",
                "SS Kashmir Willow cricket bat. Short handle. Well-oiled and maintained. Perfect for leather ball cricket.",
                1800, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_SPORTS, "Good",
                "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800&q=80", now - offset++));

        list.add(seed("Football (Size 5)",
                "Nivia Storm football, size 5. Official match ball. Good grip. Slightly used but holds air perfectly.",
                650, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_SPORTS, "Good",
                "https://images.unsplash.com/photo-1614632537423-1e6c2e7e0aab?w=800&q=80", now - offset++));

        list.add(seed("Dumbbells Set (5kg x 2)",
                "Pair of 5kg dumbbells with rubber coating. Non-slip grip. Perfect for home workout. Barely used.",
                1200, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_SPORTS, "Like New",
                "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80", now - offset++));

        // HOSTEL / DAILY USE (5 items)
        list.add(seed("Electric Kettle 1.5L",
                "Prestige electric kettle, 1.5 litre capacity. Auto shut-off. Perfect for making tea, coffee, or Maggi. Works great.",
                550, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_OTHER, "Good",
                "https://images.unsplash.com/photo-1563822249366-3efbb5c8f9e6?w=800&q=80", now - offset++));

        list.add(seed("Steam Iron",
                "Philips steam iron. 1200W. Non-stick soleplate. Used occasionally. Heats up quickly. No scratches.",
                750, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_OTHER, "Good",
                "https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=800&q=80", now - offset++));

        list.add(seed("Clothes Hanger Set",
                "Set of 12 plastic hangers. Sturdy and durable. Space-saving design. Perfect for hostel wardrobe.",
                180, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_OTHER, "Like New",
                "https://images.unsplash.com/photo-1582735689369-4fe89db7114c?w=800&q=80", now - offset++));

        list.add(seed("Cotton Bedsheet Set",
                "Double bedsheet with 2 pillow covers. 100% cotton. Floral print. Washed and clean. Comfortable fabric.",
                450, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_OTHER, "Good",
                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=80", now - offset++));

        list.add(seed("Extension Board 6 Socket",
                "Anchor 6-socket extension board with 3-meter cable. Individual switches. Surge protection. Essential for hostel room.",
                380, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_OTHER, "Like New",
                "https://images.unsplash.com/photo-1591696331111-ef9586a5b17a?w=800&q=80", now - offset++));

        // MISC (8 items)
        list.add(seed("Graphing Calculator",
                "Texas Instruments TI-84 Plus graphing calculator. Perfect for advanced math and engineering. Barely used.",
                3500, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Like New",
                "https://images.unsplash.com/photo-1587145820266-a5951ee6f620?w=800&q=80", now - offset++));

        list.add(seed("Portable Whiteboard",
                "Small portable whiteboard (2x3 feet). Magnetic surface. Comes with markers and eraser. Great for group study.",
                650, Constants.PRODUCT_TYPE_RENT, Constants.CATEGORY_OTHER, "Good",
                "https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?w=800&q=80", now - offset++));

        list.add(seed("Bicycle Helmet",
                "Adjustable bicycle helmet. Lightweight with ventilation. Safety certified. Used only a few times.",
                450, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_SPORTS, "Like New",
                "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80", now - offset++));

        list.add(seed("USB-C Hub 7-in-1",
                "7-in-1 USB-C hub with HDMI, USB 3.0, SD card reader, and charging port. Perfect for MacBook and modern laptops.",
                1100, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Like New",
                "https://images.unsplash.com/photo-1625948515291-69613efd103f?w=800&q=80", now - offset++));

        list.add(seed("Desk Organizer Set",
                "Wooden desk organizer with pen holder, phone stand, and drawer. Keeps desk tidy. Minimalist design.",
                550, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_OTHER, "Good",
                "https://images.unsplash.com/photo-1611532736597-de2d4265fba3?w=800&q=80", now - offset++));

        list.add(seed("Yoga Mat",
                "6mm thick yoga mat with carrying strap. Non-slip surface. Perfect for yoga, exercise, or meditation.",
                550, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_SPORTS, "Good",
                "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&q=80", now - offset++));

        list.add(seed("Bluetooth Speaker",
                "JBL Go 2 portable Bluetooth speaker. Waterproof. 5-hour battery. Loud and clear sound. Great for parties.",
                1600, Constants.PRODUCT_TYPE_BUY, Constants.CATEGORY_ELECTRONICS, "Good",
                "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=800&q=80", now - offset++));

        return list;
    }

    /**
     * Updates all existing seed products with proper image URLs.
     * This is a one-time repair for products that were seeded without images.
     */
    public void repairSeedProductImages(ProductCallback callback) {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("ownerId", SEED_OWNER_ID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess("No seed products found");
                        return;
                    }

                    // Map of product titles to image URLs
                    Map<String, String> imageMap = new HashMap<>();
                    imageMap.put("Engineering Mathematics Book", "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80");
                    imageMap.put("Scientific Calculator (Casio FX-991)", "https://images.unsplash.com/photo-1611224923853-80b023f02d71?w=800&q=80");
                    imageMap.put("Study Table", "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800&q=80");
                    imageMap.put("Office Chair", "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=800&q=80");
                    imageMap.put("Monitor 24 inch (Full HD)", "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&q=80");
                    imageMap.put("Mechanical Keyboard", "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80");
                    imageMap.put("Bicycle (Hero Sprint)", "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&q=80");
                    imageMap.put("Lab Coat (Size M)", "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&q=80");
                    imageMap.put("Laptop Backpack", "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&q=80");
                    imageMap.put("Noise Cancelling Headphones", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80");
                    imageMap.put("Single Mattress", "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=800&q=80");
                    imageMap.put("Steel Water Bottle (1L)", "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&q=80");

                    final int[] remaining = {snapshot.size()};
                    final int[] updated = {0};

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        String title = doc.getString("title");
                        String currentImageUrl = doc.getString("imageUrl");

                        // Only update if image is missing or empty
                        if ((currentImageUrl == null || currentImageUrl.isEmpty()) && title != null && imageMap.containsKey(title)) {
                            Map<String, Object> update = new HashMap<>();
                            update.put("imageUrl", imageMap.get(title));

                            doc.getReference().update(update)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            updated[0]++;
                                        }
                                        remaining[0]--;
                                        if (remaining[0] == 0) {
                                            Log.d(TAG, "Repaired " + updated[0] + " seed product images");
                                            callback.onSuccess("Updated " + updated[0] + " products");
                                        }
                                    });
                        } else {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                callback.onSuccess("Updated " + updated[0] + " products");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "repairSeedProductImages failed", e);
                    callback.onFailure("Repair failed: " + e.getMessage());
                });
    }
}
