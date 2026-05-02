package com.example.unikart.firebase;

import android.util.Log;

import com.example.unikart.models.Order;
import com.example.unikart.models.Review;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.NotificationSender;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {

    private static final String TAG = "OrderRepository";

    private final FirebaseFirestore firestore;
    private final FirebaseManager firebaseManager;

    public interface OrderCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface OrderListCallback {
        void onSuccess(List<Order> orders);
        void onFailure(String error);
    }

    public OrderRepository() {
        firebaseManager = FirebaseManager.getInstance();
        firestore = firebaseManager.getFirestore();
    }

    // ─── Create Order ─────────────────────────────────────────────────────────

    public void createOrder(String productId, String productTitle, String productImageUrl,
                            double price, String sellerId, String sellerName,
                            String type, int rentDays, OrderCallback callback) {
        String buyerId = firebaseManager.getCurrentUserId();
        if (buyerId == null) { callback.onFailure("Not logged in"); return; }

        String orderId = firestore.collection(Constants.COLLECTION_ORDERS).document().getId();

        // Fetch buyer name
        firestore.collection(Constants.COLLECTION_USERS).document(buyerId).get()
                .addOnSuccessListener(doc -> {
                    String buyerName = doc.getString("name");
                    if (buyerName == null) buyerName = "Unknown";
                    final String finalBuyerName = buyerName;

                    Map<String, Object> data = new HashMap<>();
                    data.put("orderId", orderId);
                    data.put("productId", productId != null ? productId : "");
                    data.put("productTitle", productTitle != null ? productTitle : "");
                    data.put("productImageUrl", productImageUrl != null ? productImageUrl : "");
                    data.put("price", price);
                    data.put("buyerId", buyerId);
                    data.put("buyerName", finalBuyerName);
                    data.put("sellerId", sellerId != null ? sellerId : "");
                    data.put("sellerName", sellerName != null ? sellerName : "");
                    data.put("type", type != null ? type : Constants.PRODUCT_TYPE_BUY);
                    data.put("status", Constants.ORDER_STATUS_REQUESTED);
                    data.put("requestedAt", System.currentTimeMillis());
                    data.put("updatedAt", System.currentTimeMillis());
                    boolean isRentOrder = Constants.PRODUCT_TYPE_RENT.equals(type);
                    int days = isRentOrder && rentDays > 0 ? rentDays : 1;
                    double totalPrice = isRentOrder ? price * days : price;
                    data.put("rentDays", isRentOrder ? days : 0);
                    data.put("totalPrice", totalPrice);

                    firestore.collection(Constants.COLLECTION_ORDERS).document(orderId)
                            .set(data)
                            .addOnSuccessListener(v -> {
                                Log.d(TAG, "Order created: " + orderId);
                                // Notify the seller about the new request
                                String orderType = type != null ? type : Constants.PRODUCT_TYPE_BUY;
                                String action = Constants.PRODUCT_TYPE_RENT.equals(orderType) ? "Rent" : "Buy";
                                String notifTitle = "New " + action + " Request";
                                String notifBody  = finalBuyerName + " wants to " + action.toLowerCase()
                                        + " \"" + (productTitle != null ? productTitle : "your item") + "\"";
                                NotificationSender.sendOrderNotification(
                                        sellerId, notifTitle, notifBody, true);
                                callback.onSuccess(orderId);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "createOrder failed", e);
                                callback.onFailure("Failed to create order: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Could not fetch buyer name", e);
                    // Still create order with fallback
                    Map<String, Object> data = new HashMap<>();
                    data.put("orderId", orderId);
                    data.put("productId", productId != null ? productId : "");
                    data.put("productTitle", productTitle != null ? productTitle : "");
                    data.put("productImageUrl", productImageUrl != null ? productImageUrl : "");
                    data.put("price", price);
                    data.put("buyerId", buyerId);
                    data.put("buyerName", "Unknown");
                    data.put("sellerId", sellerId != null ? sellerId : "");
                    data.put("sellerName", sellerName != null ? sellerName : "");
                    data.put("type", type != null ? type : Constants.PRODUCT_TYPE_BUY);
                    data.put("status", Constants.ORDER_STATUS_REQUESTED);
                    data.put("requestedAt", System.currentTimeMillis());
                    data.put("updatedAt", System.currentTimeMillis());
                    boolean isRentOrderFb = Constants.PRODUCT_TYPE_RENT.equals(type);
                    int daysFb = isRentOrderFb && rentDays > 0 ? rentDays : 1;
                    double totalPriceFb = isRentOrderFb ? price * daysFb : price;
                    data.put("rentDays", isRentOrderFb ? daysFb : 0);
                    data.put("totalPrice", totalPriceFb);
                    firestore.collection(Constants.COLLECTION_ORDERS).document(orderId)
                            .set(data)
                            .addOnSuccessListener(v -> callback.onSuccess(orderId))
                            .addOnFailureListener(e2 -> callback.onFailure(e2.getMessage()));
                });
    }

    // ─── Update Order Status ──────────────────────────────────────────────────

    public void updateOrderStatus(String orderId, String newStatus, OrderCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", newStatus);
        update.put("updatedAt", System.currentTimeMillis());

        firestore.collection(Constants.COLLECTION_ORDERS).document(orderId)
                .update(update)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Order " + orderId + " → " + newStatus);
                    
                    // Update product availability based on status
                    updateProductAvailabilityForOrder(orderId, newStatus);
                    
                    // Send push notification to the relevant party
                    sendOrderStatusNotification(orderId, newStatus);
                    callback.onSuccess(newStatus);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateOrderStatus failed", e);
                    callback.onFailure("Failed to update order: " + e.getMessage());
                });
    }
    
    /**
     * Update product availability based on order status:
     * - ACCEPTED: mark unavailable (item is now in transaction)
     * - For both BUY and RENT: stays unavailable after completion/return
     *   (seller must manually re-list via "Mark Available" button)
     */
    private void updateProductAvailabilityForOrder(String orderId, String newStatus) {
        firestore.collection(Constants.COLLECTION_ORDERS).document(orderId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    
                    String productId = doc.getString("productId");
                    if (productId == null || productId.isEmpty()) return;
                    
                    // Only mark unavailable when order is accepted
                    // Never auto-mark available - seller must do it manually
                    if (Constants.ORDER_STATUS_ACCEPTED.equals(newStatus)) {
                        Map<String, Object> productUpdate = new HashMap<>();
                        productUpdate.put("available", false);
                        firestore.collection(Constants.COLLECTION_PRODUCTS).document(productId)
                                .update(productUpdate)
                                .addOnSuccessListener(v2 -> 
                                    Log.d(TAG, "Product " + productId + " marked unavailable"))
                                .addOnFailureListener(e -> 
                                    Log.w(TAG, "Failed to update product availability", e));
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Could not fetch order for availability update", e));
    }

    /**
     * Reads the order document and sends a push notification to the appropriate party
     * based on the new status.
     */
    private void sendOrderStatusNotification(String orderId, String newStatus) {
        firestore.collection(Constants.COLLECTION_ORDERS).document(orderId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String buyerId    = doc.getString("buyerId");
                    String sellerId   = doc.getString("sellerId");
                    String buyerName  = doc.getString("buyerName");
                    String sellerName = doc.getString("sellerName");
                    String productTitle = doc.getString("productTitle");
                    String type       = doc.getString("type");
                    boolean isRent    = Constants.PRODUCT_TYPE_RENT.equals(type);

                    String title;
                    String body;
                    String recipientId;
                    boolean openSellerTab;

                    switch (newStatus) {
                        case Constants.ORDER_STATUS_ACCEPTED:
                            // Buyer is notified: their request was accepted
                            recipientId   = buyerId;
                            openSellerTab = false;
                            title = "Request Accepted!";
                            body  = sellerName + " accepted your "
                                    + (isRent ? "rent" : "buy") + " request for \""
                                    + productTitle + "\"";
                            break;

                        case Constants.ORDER_STATUS_REJECTED:
                            // Buyer is notified: their request was rejected
                            recipientId   = buyerId;
                            openSellerTab = false;
                            title = "Request Declined";
                            body  = sellerName + " declined your request for \""
                                    + productTitle + "\"";
                            break;

                        case Constants.ORDER_STATUS_HANDED_OVER:
                            // Buyer is notified: item has been handed over
                            recipientId   = buyerId;
                            openSellerTab = false;
                            title = "Item Handed Over";
                            body  = "\"" + productTitle + "\" has been marked as handed over by "
                                    + sellerName;
                            break;

                        case Constants.ORDER_STATUS_COMPLETED:
                            // Seller is notified: buyer confirmed receipt (BUY)
                            recipientId   = sellerId;
                            openSellerTab = true;
                            title = "Sale Completed!";
                            body  = buyerName + " confirmed receiving \"" + productTitle + "\"";
                            break;

                        case Constants.ORDER_STATUS_RETURN_PENDING:
                            // Seller is notified: buyer wants to return (RENT)
                            recipientId   = sellerId;
                            openSellerTab = true;
                            title = "Return Requested";
                            body  = buyerName + " wants to return \"" + productTitle + "\"";
                            break;

                        case Constants.ORDER_STATUS_RETURNED:
                            // Buyer is notified: seller confirmed return
                            recipientId   = buyerId;
                            openSellerTab = false;
                            title = "Return Confirmed";
                            body  = sellerName + " confirmed the return of \"" + productTitle + "\"";
                            break;

                        default:
                            return; // No notification for other statuses
                    }

                    if (recipientId != null && !recipientId.isEmpty()) {
                        NotificationSender.sendOrderNotification(
                                recipientId, title, body, openSellerTab);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "sendOrderStatusNotification: could not read order", e));
    }

    // ─── Get Orders as Buyer ──────────────────────────────────────────────────

    public void getOrdersAsBuyer(OrderListCallback callback) {
        String uid = firebaseManager.getCurrentUserId();
        if (uid == null) { callback.onFailure("Not logged in"); return; }

        firestore.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("buyerId", uid)
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        list.add(mapDoc(doc));
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getOrdersAsBuyer failed", e);
                    // Fallback without orderBy
                    getOrdersAsBuyerFallback(uid, callback);
                });
    }

    private void getOrdersAsBuyerFallback(String uid, OrderListCallback callback) {
        firestore.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("buyerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        list.add(mapDoc(doc));
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure("Could not load orders"));
    }

    // ─── Get Orders as Seller ─────────────────────────────────────────────────

    public void getOrdersAsSeller(OrderListCallback callback) {
        String uid = firebaseManager.getCurrentUserId();
        if (uid == null) { callback.onFailure("Not logged in"); return; }

        firestore.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("sellerId", uid)
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        list.add(mapDoc(doc));
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getOrdersAsSeller failed", e);
                    getOrdersAsSellerFallback(uid, callback);
                });
    }

    private void getOrdersAsSellerFallback(String uid, OrderListCallback callback) {
        firestore.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("sellerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Order> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        list.add(mapDoc(doc));
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure("Could not load orders"));
    }

    // ─── Submit Review ────────────────────────────────────────────────────────

    public void submitReview(String orderId, String productId, String revieweeId,
                             float rating, String comment, OrderCallback callback) {
        String reviewerId = firebaseManager.getCurrentUserId();
        if (reviewerId == null) { callback.onFailure("Not logged in"); return; }

        // Guard: prevent duplicate reviews for the same order by the same reviewer
        firestore.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("reviewerId", reviewerId)
                .limit(1)
                .get()
                .addOnSuccessListener(existing -> {
                    if (!existing.isEmpty()) {
                        callback.onFailure("You have already reviewed this order.");
                        return;
                    }
                    doSubmitReview(orderId, productId, revieweeId, rating, comment, reviewerId, callback);
                })
                .addOnFailureListener(e -> {
                    // If check fails, proceed anyway (UI already guards this)
                    doSubmitReview(orderId, productId, revieweeId, rating, comment, reviewerId, callback);
                });
    }

    private void doSubmitReview(String orderId, String productId, String revieweeId,
                                float rating, String comment, String reviewerId, OrderCallback callback) {

        // Fetch reviewer info and order info in parallel
        firestore.collection(Constants.COLLECTION_USERS).document(reviewerId).get()
                .addOnSuccessListener(userDoc -> {
                    final String reviewerName = userDoc.getString("name") != null ? userDoc.getString("name") : "Anonymous";
                    final String reviewerProfilePic = userDoc.getString("profilePicture") != null ? userDoc.getString("profilePicture") : "";

                    // Fetch order to get product title and transaction type
                    firestore.collection(Constants.COLLECTION_ORDERS).document(orderId).get()
                            .addOnSuccessListener(orderDoc -> {
                                String productTitle = orderDoc.getString("productTitle");
                                String transactionType = orderDoc.getString("type");

                                String reviewId = firestore.collection(Constants.COLLECTION_REVIEWS).document().getId();
                                Map<String, Object> data = new HashMap<>();
                                data.put("reviewId", reviewId);
                                data.put("orderId", orderId);
                                data.put("productId", productId);
                                data.put("productTitle", productTitle != null ? productTitle : "");
                                data.put("reviewerId", reviewerId);
                                data.put("reviewerName", reviewerName);
                                data.put("reviewerProfilePic", reviewerProfilePic);
                                data.put("revieweeId", revieweeId);
                                data.put("rating", rating);
                                data.put("comment", comment != null ? comment : "");
                                data.put("timestamp", System.currentTimeMillis());
                                data.put("transactionType", transactionType != null ? transactionType : "");

                                firestore.collection(Constants.COLLECTION_REVIEWS).document(reviewId)
                                        .set(data)
                                        .addOnSuccessListener(v -> {
                                            updateUserRating(revieweeId);
                                            callback.onSuccess("Review submitted");
                                        })
                                        .addOnFailureListener(e -> callback.onFailure("Failed to submit review: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> {
                                // Fallback: create review without order details
                                String reviewId = firestore.collection(Constants.COLLECTION_REVIEWS).document().getId();
                                Map<String, Object> data = new HashMap<>();
                                data.put("reviewId", reviewId);
                                data.put("orderId", orderId);
                                data.put("productId", productId);
                                data.put("productTitle", "");
                                data.put("reviewerId", reviewerId);
                                data.put("reviewerName", reviewerName);
                                data.put("reviewerProfilePic", reviewerProfilePic);
                                data.put("revieweeId", revieweeId);
                                data.put("rating", rating);
                                data.put("comment", comment != null ? comment : "");
                                data.put("timestamp", System.currentTimeMillis());
                                data.put("transactionType", "");

                                firestore.collection(Constants.COLLECTION_REVIEWS).document(reviewId)
                                        .set(data)
                                        .addOnSuccessListener(v -> {
                                            updateUserRating(revieweeId);
                                            callback.onSuccess("Review submitted");
                                        })
                                        .addOnFailureListener(e2 -> callback.onFailure("Failed to submit review: " + e2.getMessage()));
                            });
                })
                .addOnFailureListener(e -> callback.onFailure("Could not fetch user: " + e.getMessage()));
    }

    private void updateUserRating(String userId) {
        firestore.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("revieweeId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;
                    double total = 0;
                    int count = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        Double r = doc.getDouble("rating");
                        if (r != null) { total += r; count++; }
                    }
                    if (count == 0) return;
                    double avg = total / count;
                    Map<String, Object> update = new HashMap<>();
                    update.put("rating", avg);
                    update.put("reviewCount", count);
                    firestore.collection(Constants.COLLECTION_USERS).document(userId).update(update);
                });
    }

    // ─── Check if reviewer already reviewed this order ───────────────────────

    public interface HasReviewedCallback {
        void onResult(boolean hasReviewed);
    }

    public void hasReviewedOrder(String orderId, String reviewerId, HasReviewedCallback callback) {
        firestore.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("reviewerId", reviewerId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> callback.onResult(!snap.isEmpty()))
                .addOnFailureListener(e -> callback.onResult(false)); // fail open — guard in submit too
    }

    // ─── Get Reviews for User ─────────────────────────────────────────────────

    public interface ReviewListCallback {
        void onSuccess(List<Review> reviews);
        void onFailure(String error);
    }

    /** Fetches all reviews for a specific product (by productId field). */
    public void getReviewsForProduct(String productId, ReviewListCallback callback) {
        if (productId == null || productId.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        firestore.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("productId", productId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Review> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        list.add(mapReviewDoc(doc));
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "getReviewsForProduct ordered failed, trying fallback", e);
                    firestore.collection(Constants.COLLECTION_REVIEWS)
                            .whereEqualTo("productId", productId)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                List<Review> list = new ArrayList<>();
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap2) {
                                    list.add(mapReviewDoc(doc));
                                }
                                list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                                callback.onSuccess(list);
                            })
                            .addOnFailureListener(e2 -> callback.onFailure(e2.getMessage()));
                });
    }

    public void getReviewsForUser(String userId, ReviewListCallback callback) {
        firestore.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("revieweeId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Review> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        Review r = mapReviewDoc(doc);
                        list.add(r);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getReviewsForUser with orderBy failed, trying fallback", e);
                    // Fallback without orderBy (in case index doesn't exist)
                    getReviewsForUserFallback(userId, callback);
                });
    }

    private void getReviewsForUserFallback(String userId, ReviewListCallback callback) {
        firestore.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("revieweeId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Review> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        Review r = mapReviewDoc(doc);
                        list.add(r);
                    }
                    // Sort manually by timestamp descending
                    list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getReviewsForUserFallback also failed", e);
                    callback.onFailure("Could not load reviews");
                });
    }

    private Review mapReviewDoc(com.google.firebase.firestore.QueryDocumentSnapshot doc) {
        Review r = new Review();
        r.setReviewId(safeStr(doc, "reviewId", doc.getId()));
        r.setOrderId(safeStr(doc, "orderId", ""));
        r.setProductId(safeStr(doc, "productId", ""));
        r.setProductTitle(safeStr(doc, "productTitle", ""));
        r.setReviewerId(safeStr(doc, "reviewerId", ""));
        r.setReviewerName(safeStr(doc, "reviewerName", "Anonymous"));
        r.setReviewerProfilePic(safeStr(doc, "reviewerProfilePic", ""));
        r.setRevieweeId(safeStr(doc, "revieweeId", ""));
        r.setTransactionType(safeStr(doc, "transactionType", ""));
        Double rating = doc.getDouble("rating");
        r.setRating(rating != null ? rating.floatValue() : 0f);
        r.setComment(safeStr(doc, "comment", ""));
        Long ts = doc.getLong("timestamp");
        r.setTimestamp(ts != null ? ts : 0L);
        return r;
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Order mapDoc(com.google.firebase.firestore.QueryDocumentSnapshot doc) {
        Order o = new Order();
        o.setOrderId(safeStr(doc, "orderId", doc.getId()));
        o.setProductId(safeStr(doc, "productId", ""));
        o.setProductTitle(safeStr(doc, "productTitle", "Product"));
        o.setProductImageUrl(safeStr(doc, "productImageUrl", ""));
        o.setBuyerId(safeStr(doc, "buyerId", ""));
        o.setBuyerName(safeStr(doc, "buyerName", ""));
        o.setSellerId(safeStr(doc, "sellerId", ""));
        o.setSellerName(safeStr(doc, "sellerName", ""));
        o.setType(safeStr(doc, "type", Constants.PRODUCT_TYPE_BUY));
        o.setStatus(safeStr(doc, "status", Constants.ORDER_STATUS_REQUESTED));
        Double price = doc.getDouble("price");
        o.setPrice(price != null ? price : 0.0);
        Long rentDays = doc.getLong("rentDays");
        o.setRentDays(rentDays != null ? rentDays.intValue() : 0);
        Double totalPrice = doc.getDouble("totalPrice");
        o.setTotalPrice(totalPrice != null ? totalPrice : (price != null ? price : 0.0));
        Long req = doc.getLong("requestedAt");
        o.setRequestedAt(req != null ? req : 0L);
        Long upd = doc.getLong("updatedAt");
        o.setUpdatedAt(upd != null ? upd : 0L);
        return o;
    }

    private String safeStr(com.google.firebase.firestore.DocumentSnapshot doc, String field, String fallback) {
        String v = doc.getString(field);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }
}
