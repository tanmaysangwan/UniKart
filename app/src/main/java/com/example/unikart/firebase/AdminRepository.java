package com.example.unikart.firebase;

import android.util.Log;

import com.example.unikart.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminRepository {

    private static final String TAG = "AdminRepository";

    public static final String DEMO_EMAIL    = "campus.store@bmu.edu.in";
    public static final String DEMO_PASSWORD = "12345678";
    public static final String DEMO_NAME     = "Campus Store";
    public static final String DEMO_STUDENT_ID = "CAMPUS001";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public interface AdminCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public AdminRepository() {
        FirebaseManager mgr = FirebaseManager.getInstance();
        auth = mgr.getAuth();
        firestore = mgr.getFirestore();
    }

    /**
     * Creates the demo seller account if it doesn't exist.
     * Signs in with demo credentials to verify, or creates if sign-in fails.
     * After creation, re-signs in as the original user.
     */
    public void ensureDemoAccountExists(AdminCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : null;

        // Try signing in as demo account first
        auth.signInWithEmailAndPassword(DEMO_EMAIL, DEMO_PASSWORD)
                .addOnSuccessListener(result -> {
                    FirebaseUser demoUser = result.getUser();
                    if (demoUser == null) {
                        restoreUser(currentUid, callback, "Demo account sign-in returned null");
                        return;
                    }
                    String demoUid = demoUser.getUid();
                    Log.d(TAG, "Demo account exists, uid=" + demoUid);

                    // Ensure Firestore doc exists
                    ensureDemoFirestoreDoc(demoUid, () -> {
                        // Re-seed products with real UID
                        updateSeedProductsOwner(demoUid, () -> {
                            restoreUser(currentUid, callback, "Demo account ready. UID: " + demoUid);
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("no user record") || msg.contains("USER_NOT_FOUND")
                            || msg.contains("INVALID_LOGIN_CREDENTIALS") || msg.contains("EMAIL_NOT_FOUND")) {
                        // Account doesn't exist — create it
                        createDemoAccount(currentUid, callback);
                    } else {
                        Log.e(TAG, "Demo sign-in failed unexpectedly: " + msg);
                        restoreUser(currentUid, callback, "Could not verify demo account: " + msg);
                    }
                });
    }

    private void createDemoAccount(String originalUid, AdminCallback callback) {
        auth.createUserWithEmailAndPassword(DEMO_EMAIL, DEMO_PASSWORD)
                .addOnSuccessListener(result -> {
                    FirebaseUser demoUser = result.getUser();
                    if (demoUser == null) {
                        restoreUser(originalUid, callback, "Demo account creation returned null");
                        return;
                    }
                    String demoUid = demoUser.getUid();
                    Log.d(TAG, "Demo account created, uid=" + demoUid);

                    ensureDemoFirestoreDoc(demoUid, () ->
                            updateSeedProductsOwner(demoUid, () ->
                                    restoreUser(originalUid, callback,
                                            "Demo account created! UID: " + demoUid)));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createDemoAccount failed", e);
                    restoreUser(originalUid, callback, "Failed to create demo account: " + e.getMessage());
                });
    }

    private void ensureDemoFirestoreDoc(String demoUid, Runnable onDone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", demoUid);
        userData.put("name", DEMO_NAME);
        userData.put("email", DEMO_EMAIL);
        userData.put("studentId", DEMO_STUDENT_ID);
        userData.put("rating", 5.0);
        userData.put("reviewCount", 0);
        userData.put("createdAt", System.currentTimeMillis());

        firestore.collection(Constants.COLLECTION_USERS)
                .document(demoUid)
                .set(userData)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "ensureDemoFirestoreDoc failed", task.getException());
                    }
                    onDone.run();
                });
    }

    private void updateSeedProductsOwner(String demoUid, Runnable onDone) {
        // Update all products owned by the placeholder seed ID to use the real UID
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .whereEqualTo("ownerId", "campus_store_seed")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        Log.d(TAG, "No seed products to update");
                        onDone.run();
                        return;
                    }

                    final int[] remaining = {snapshot.size()};
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("ownerId", demoUid);
                        update.put("ownerName", DEMO_NAME);
                        doc.getReference().update(update)
                                .addOnCompleteListener(task -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        Log.d(TAG, "Seed products updated to real UID: " + demoUid);
                                        onDone.run();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "updateSeedProductsOwner failed", e);
                    onDone.run();
                });
    }

    private void restoreUser(String originalUid, AdminCallback callback, String successMessage) {
        if (originalUid == null) {
            // Was not logged in before — sign out
            auth.signOut();
            callback.onSuccess(successMessage);
            return;
        }

        // Check if we're already the original user
        FirebaseUser current = auth.getCurrentUser();
        if (current != null && current.getUid().equals(originalUid)) {
            callback.onSuccess(successMessage);
            return;
        }

        // We can't re-sign in as original user without their password.
        // Sign out so they can log back in manually.
        auth.signOut();
        callback.onSuccess(successMessage + "\n\nPlease log back in with your account.");
    }

    /**
     * Clears all products from Firestore (for dev reset).
     */
    public void clearAllProducts(AdminCallback callback) {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess("No products to clear");
                        return;
                    }
                    final int[] remaining = {snapshot.size()};
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        doc.getReference().delete()
                                .addOnCompleteListener(task -> {
                                    remaining[0]--;
                                    if (remaining[0] == 0) {
                                        callback.onSuccess("Cleared " + snapshot.size() + " products");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Clear failed: " + e.getMessage()));
    }

    /**
     * Repairs all products with missing or broken images.
     * Assigns category-appropriate images based on product title and category.
     */
    public void repairListingImages(AdminCallback callback) {
        firestore.collection(Constants.COLLECTION_PRODUCTS)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess("No products found");
                        return;
                    }

                    int totalProducts = snapshot.size();
                    final int[] repairedCount = {0};
                    final int[] remaining = {totalProducts};

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
                        String imageUrl = doc.getString("imageUrl");
                        String category = doc.getString("category");
                        String title = doc.getString("title");

                        // Check if image needs repair
                        boolean needsRepair = imageUrl == null || 
                                            imageUrl.isEmpty() || 
                                            imageUrl.equals("https://via.placeholder.com/400") ||
                                            !imageUrl.startsWith("http");

                        if (needsRepair) {
                            String newImageUrl = getImageForProduct(title, category);
                            Map<String, Object> update = new HashMap<>();
                            update.put("imageUrl", newImageUrl);

                            doc.getReference().update(update)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            repairedCount[0]++;
                                        }
                                        remaining[0]--;
                                        if (remaining[0] == 0) {
                                            callback.onSuccess("Repaired " + repairedCount[0] + " of " + totalProducts + " products");
                                        }
                                    });
                        } else {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                callback.onSuccess("Repaired " + repairedCount[0] + " of " + totalProducts + " products");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Repair failed: " + e.getMessage()));
    }

    /**
     * Rebuilds the entire marketplace inventory:
     * 1. Repairs all product images
     * 2. Adds new expanded inventory
     */
    public void rebuildMarketplaceInventory(AdminCallback callback) {
        ProductRepository productRepo = new ProductRepository();
        productRepo.rebuildMarketplaceInventory(new ProductRepository.ProductCallback() {
            @Override
            public void onSuccess(String message) {
                callback.onSuccess(message);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    /**
     * Returns an appropriate image URL based on product title and category.
     * Uses Unsplash for high-quality, free-to-use images.
     */
    private String getImageForProduct(String title, String category) {
        if (title == null) title = "";
        if (category == null) category = "";
        
        String titleLower = title.toLowerCase();
        String categoryLower = category.toLowerCase();

        // Books category
        if (categoryLower.contains("book")) {
            if (titleLower.contains("physics") || titleLower.contains("science")) {
                return "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&q=80";
            }
            if (titleLower.contains("chemistry")) {
                return "https://images.unsplash.com/photo-1603126857599-f6e157fa2fe6?w=800&q=80";
            }
            if (titleLower.contains("math") || titleLower.contains("calculus")) {
                return "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80";
            }
            return "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=800&q=80";
        }

        // Electronics category
        if (categoryLower.contains("electronic")) {
            if (titleLower.contains("laptop") || titleLower.contains("macbook")) {
                return "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80";
            }
            if (titleLower.contains("monitor") || titleLower.contains("screen")) {
                return "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&q=80";
            }
            if (titleLower.contains("headphone") || titleLower.contains("earphone")) {
                return "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80";
            }
            if (titleLower.contains("calculator")) {
                return "https://images.unsplash.com/photo-1611224923853-80b023f02d71?w=800&q=80";
            }
            if (titleLower.contains("mouse") || titleLower.contains("keyboard")) {
                return "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80";
            }
            return "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=800&q=80";
        }

        // Furniture category
        if (categoryLower.contains("furniture")) {
            if (titleLower.contains("chair") || titleLower.contains("seat")) {
                return "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=800&q=80";
            }
            if (titleLower.contains("desk") || titleLower.contains("table")) {
                return "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800&q=80";
            }
            if (titleLower.contains("mattress") || titleLower.contains("bed")) {
                return "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=800&q=80";
            }
            if (titleLower.contains("shelf") || titleLower.contains("rack")) {
                return "https://images.unsplash.com/photo-1594620302200-9a762244a156?w=800&q=80";
            }
            return "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800&q=80";
        }

        // Sports category
        if (categoryLower.contains("sport")) {
            if (titleLower.contains("football") || titleLower.contains("soccer")) {
                return "https://images.unsplash.com/photo-1614632537423-1e6c2e7e0aab?w=800&q=80";
            }
            if (titleLower.contains("cycle") || titleLower.contains("bike") || titleLower.contains("bicycle")) {
                return "https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&q=80";
            }
            if (titleLower.contains("cricket") || titleLower.contains("bat")) {
                return "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800&q=80";
            }
            if (titleLower.contains("gym") || titleLower.contains("dumbbell")) {
                return "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80";
            }
            return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800&q=80";
        }

        // Clothing/Fashion category
        if (categoryLower.contains("cloth") || categoryLower.contains("fashion")) {
            if (titleLower.contains("shoe") || titleLower.contains("sneaker")) {
                return "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80";
            }
            if (titleLower.contains("bag") || titleLower.contains("backpack")) {
                return "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&q=80";
            }
            if (titleLower.contains("jacket") || titleLower.contains("coat")) {
                return "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&q=80";
            }
            if (titleLower.contains("watch")) {
                return "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80";
            }
            return "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=800&q=80";
        }

        // Other/General items
        if (titleLower.contains("bottle") || titleLower.contains("flask")) {
            return "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&q=80";
        }
        if (titleLower.contains("lamp") || titleLower.contains("light")) {
            return "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&q=80";
        }
        if (titleLower.contains("plant") || titleLower.contains("pot")) {
            return "https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=800&q=80";
        }
        if (titleLower.contains("mug") || titleLower.contains("cup")) {
            return "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=800&q=80";
        }

        // Default fallback based on category
        switch (categoryLower) {
            case "books":
                return "https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=800&q=80";
            case "electronics":
                return "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=800&q=80";
            case "furniture":
                return "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800&q=80";
            case "sports":
                return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800&q=80";
            case "clothing":
                return "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=800&q=80";
            default:
                return "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80";
        }
    }
}
