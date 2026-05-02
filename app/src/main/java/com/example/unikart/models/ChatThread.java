package com.example.unikart.models;

public class ChatThread {
    private String chatId;
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private String productId;
    private String productTitle;
    private String lastMessage;
    private long lastMessageAt;
    private long createdAt;

    public ChatThread() {}

    public ChatThread(String chatId, String buyerId, String sellerId, String sellerName,
                      String productId, String productTitle, String lastMessage,
                      long lastMessageAt, long createdAt) {
        this.chatId = chatId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.productId = productId;
        this.productTitle = productTitle;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.createdAt = createdAt;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getBuyerName() { return buyerName != null ? buyerName : ""; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductTitle() { return productTitle; }
    public void setProductTitle(String productTitle) { this.productTitle = productTitle; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(long lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
