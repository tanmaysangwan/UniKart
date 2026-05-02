package com.example.unikart.firebase;

import android.util.Log;

import com.example.unikart.models.ChatMessage;
import com.example.unikart.models.ChatThread;
import com.example.unikart.utils.Constants;
import com.example.unikart.utils.NotificationSender;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {

    private static final String TAG = "ChatRepository";

    private final FirebaseFirestore firestore;
    private final FirebaseManager firebaseManager;

    public interface ChatCallback {
        void onSuccess(String chatId);
        void onFailure(String error);
    }

    public interface MessageCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface MessagesListener {
        void onMessages(List<ChatMessage> messages);
        void onError(String error);
    }

    public interface ChatListListener {
        void onChats(List<ChatThread> chats);
        void onError(String error);
    }

    public ChatRepository() {
        firebaseManager = FirebaseManager.getInstance();
        firestore = firebaseManager.getFirestore();
    }

    /**
     * Gets or creates a chat thread between current user and seller for a product.
     * chatId = sorted(buyerId, sellerId) + "_" + productId
     */
    public void getOrCreateChat(String sellerId, String sellerName,
                                String productId, String productTitle,
                                ChatCallback callback) {
        String buyerId = firebaseManager.getCurrentUserId();
        if (buyerId == null) {
            callback.onFailure("Not logged in");
            return;
        }

        String chatId = buildChatId(buyerId, sellerId, productId);

        DocumentReference chatRef = firestore.collection(Constants.COLLECTION_CHATS).document(chatId);
        chatRef.get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Log.d(TAG, "Chat exists: " + chatId);
                        callback.onSuccess(chatId);
                    } else {
                        // Fetch buyer's name before creating the chat doc
                        firestore.collection(Constants.COLLECTION_USERS).document(buyerId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String buyerName = userDoc.getString("name");
                                    if (buyerName == null || buyerName.isEmpty()) buyerName = "Buyer";
                                    createChatDoc(chatRef, chatId, buyerId, buyerName,
                                            sellerId, sellerName, productId, productTitle, callback);
                                })
                                .addOnFailureListener(e -> {
                                    // Still create chat even if name fetch fails
                                    createChatDoc(chatRef, chatId, buyerId, "Buyer",
                                            sellerId, sellerName, productId, productTitle, callback);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Get chat failed", e);
                    callback.onFailure("Could not load chat: " + e.getMessage());
                });
    }

    private void createChatDoc(DocumentReference chatRef, String chatId,
                                String buyerId, String buyerName,
                                String sellerId, String sellerName,
                                String productId, String productTitle,
                                ChatCallback callback) {
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chatId", chatId);
        chatData.put("buyerId", buyerId);
        chatData.put("buyerName", buyerName);
        chatData.put("sellerId", sellerId);
        chatData.put("sellerName", sellerName);
        chatData.put("productId", productId != null ? productId : "");
        chatData.put("productTitle", productTitle != null ? productTitle : "");
        chatData.put("createdAt", System.currentTimeMillis());
        chatData.put("lastMessage", "");
        chatData.put("lastMessageAt", System.currentTimeMillis());

        chatRef.set(chatData)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Chat created: " + chatId);
                    callback.onSuccess(chatId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Create chat failed", e);
                    callback.onFailure("Could not create chat: " + e.getMessage());
                });
    }

    /**
     * Sends a message to a chat thread and notifies the recipient via FCM.
     */
    public void sendMessage(String chatId, String text, MessageCallback callback) {
        String senderId = firebaseManager.getCurrentUserId();
        if (senderId == null) {
            callback.onFailure("Not logged in");
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            callback.onFailure("Empty message");
            return;
        }

        String messageId = firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .document().getId();

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageId);
        messageData.put("chatId", chatId);
        messageData.put("senderId", senderId);
        messageData.put("text", text.trim());
        messageData.put("sentAt", System.currentTimeMillis());

        firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .document(messageId)
                .set(messageData)
                .addOnSuccessListener(v -> {
                    // Update last message on chat doc
                    Map<String, Object> update = new HashMap<>();
                    update.put("lastMessage", text.trim());
                    update.put("lastMessageAt", System.currentTimeMillis());
                    firestore.collection(Constants.COLLECTION_CHATS)
                            .document(chatId)
                            .update(update);

                    // Notify the other participant
                    sendChatPushNotification(chatId, senderId, text.trim());

                    callback.onSuccess("sent");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "sendMessage failed", e);
                    callback.onFailure("Failed to send: " + e.getMessage());
                });
    }

    /**
     * Reads the chat document to find the recipient (the participant who is NOT the sender),
     * then fetches the sender's display name and fires a push notification.
     */
    private void sendChatPushNotification(String chatId, String senderId, String messageText) {
        firestore.collection(Constants.COLLECTION_CHATS).document(chatId).get()
                .addOnSuccessListener(chatDoc -> {
                    if (!chatDoc.exists()) return;

                    String buyerId  = chatDoc.getString("buyerId");
                    String sellerId = chatDoc.getString("sellerId");

                    // Determine who receives the notification
                    String recipientId = senderId.equals(buyerId) ? sellerId : buyerId;
                    if (recipientId == null || recipientId.isEmpty()) return;

                    // Fetch sender's display name, then send the push
                    final String finalRecipientId = recipientId;
                    firestore.collection(Constants.COLLECTION_USERS).document(senderId).get()
                            .addOnSuccessListener(userDoc -> {
                                String senderName = userDoc.getString("name");
                                if (senderName == null || senderName.isEmpty()) senderName = "Someone";

                                String preview = messageText.length() > 80
                                        ? messageText.substring(0, 80) + "…"
                                        : messageText;

                                NotificationSender.sendChatNotification(
                                        finalRecipientId, senderName, preview, chatId);
                            })
                            .addOnFailureListener(e ->
                                    Log.w(TAG, "Could not fetch sender name for notification", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "Could not read chat doc for notification", e));
    }

    /**
     * Attaches a real-time listener to messages in a chat.
     * Returns the ListenerRegistration so caller can remove it.
     */
    public ListenerRegistration listenToMessages(String chatId, MessagesListener listener) {
        return firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenToMessages error", error);
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<ChatMessage> messages = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            ChatMessage msg = new ChatMessage();
                            msg.setMessageId(doc.getString("messageId") != null ? doc.getString("messageId") : doc.getId());
                            msg.setChatId(doc.getString("chatId") != null ? doc.getString("chatId") : chatId);
                            msg.setSenderId(doc.getString("senderId") != null ? doc.getString("senderId") : "");
                            msg.setText(doc.getString("text") != null ? doc.getString("text") : "");
                            Long sentAt = doc.getLong("sentAt");
                            msg.setSentAt(sentAt != null ? sentAt : 0L);
                            messages.add(msg);
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping malformed message: " + doc.getId(), e);
                        }
                    }
                    listener.onMessages(messages);
                });
    }

    /**
     * Listens to all chats where current user is buyer or seller.
     */
    public ListenerRegistration listenToUserChats(String userId, ChatListListener listener) {
        return firestore.collection(Constants.COLLECTION_CHATS)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenToUserChats error", error);
                        listener.onError(error.getMessage());
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<ChatThread> threads = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            String buyerId = doc.getString("buyerId");
                            String sellerId = doc.getString("sellerId");
                            
                            // Only include chats where user is buyer or seller
                            if (!userId.equals(buyerId) && !userId.equals(sellerId)) {
                                continue;
                            }

                            ChatThread thread = new ChatThread();
                            thread.setChatId(doc.getString("chatId") != null ? doc.getString("chatId") : doc.getId());
                            thread.setBuyerId(buyerId != null ? buyerId : "");
                            thread.setSellerId(sellerId != null ? sellerId : "");
                            thread.setSellerName(doc.getString("sellerName") != null ? doc.getString("sellerName") : "Seller");
                            thread.setProductId(doc.getString("productId") != null ? doc.getString("productId") : "");
                            thread.setProductTitle(doc.getString("productTitle") != null ? doc.getString("productTitle") : "");
                            thread.setLastMessage(doc.getString("lastMessage") != null ? doc.getString("lastMessage") : "");

                            Long lastMsgAt = doc.getLong("lastMessageAt");
                            thread.setLastMessageAt(lastMsgAt != null ? lastMsgAt : 0L);

                            Long createdAt = doc.getLong("createdAt");
                            thread.setCreatedAt(createdAt != null ? createdAt : 0L);

                            // buyerName — stored on new chats; for old chats look it up live
                            String storedBuyerName = doc.getString("buyerName");
                            if (storedBuyerName != null && !storedBuyerName.isEmpty()) {
                                thread.setBuyerName(storedBuyerName);
                                threads.add(thread);
                            } else if (buyerId != null && !buyerId.isEmpty()) {
                                // Old chat doc — backfill from users collection
                                final ChatThread finalThread = thread;
                                final List<ChatThread> finalThreads = threads;
                                firestore.collection(Constants.COLLECTION_USERS)
                                        .document(buyerId).get()
                                        .addOnSuccessListener(userDoc -> {
                                            String name = userDoc.getString("name");
                                            finalThread.setBuyerName(name != null && !name.isEmpty() ? name : "Buyer");
                                            // Also backfill the chat doc so we don't look up again
                                            Map<String, Object> patch = new HashMap<>();
                                            patch.put("buyerName", finalThread.getBuyerName());
                                            firestore.collection(Constants.COLLECTION_CHATS)
                                                    .document(finalThread.getChatId())
                                                    .update(patch);
                                        })
                                        .addOnFailureListener(e2 -> finalThread.setBuyerName("Buyer"));
                                threads.add(thread);
                            } else {
                                thread.setBuyerName("Buyer");
                                threads.add(thread);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Skipping malformed chat: " + doc.getId(), e);
                        }
                    }
                    listener.onChats(threads);
                });
    }

    private String buildChatId(String buyerId, String sellerId, String productId) {
        // Sort so A-B and B-A produce same ID
        String pair = buyerId.compareTo(sellerId) < 0
                ? buyerId + "_" + sellerId
                : sellerId + "_" + buyerId;
        String pid = (productId != null && !productId.isEmpty()) ? productId : "general";
        return pair + "_" + pid;
    }
}
