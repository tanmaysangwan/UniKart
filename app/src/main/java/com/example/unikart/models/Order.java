package com.example.unikart.models;

public class Order {
    private String orderId;
    private String productId;
    private String productTitle;
    private String productImageUrl;
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String sellerName;
    private String type;       // BUY or RENT
    private String status;
    private double price;
    private int rentDays;      // number of days requested (RENT only)
    private double totalPrice; // price * rentDays for RENT, same as price for BUY
    private long requestedAt;
    private long updatedAt;

    public Order() {}

    public String getOrderId()              { return orderId != null ? orderId : ""; }
    public void setOrderId(String v)        { this.orderId = v; }

    public String getProductId()            { return productId != null ? productId : ""; }
    public void setProductId(String v)      { this.productId = v; }

    public String getProductTitle()         { return productTitle != null ? productTitle : ""; }
    public void setProductTitle(String v)   { this.productTitle = v; }

    public String getProductImageUrl()      { return productImageUrl != null ? productImageUrl : ""; }
    public void setProductImageUrl(String v){ this.productImageUrl = v; }

    public String getBuyerId()              { return buyerId != null ? buyerId : ""; }
    public void setBuyerId(String v)        { this.buyerId = v; }

    public String getBuyerName()            { return buyerName != null ? buyerName : ""; }
    public void setBuyerName(String v)      { this.buyerName = v; }

    public String getSellerId()             { return sellerId != null ? sellerId : ""; }
    public void setSellerId(String v)       { this.sellerId = v; }

    public String getSellerName()           { return sellerName != null ? sellerName : ""; }
    public void setSellerName(String v)     { this.sellerName = v; }

    public String getType()                 { return type != null ? type : "BUY"; }
    public void setType(String v)           { this.type = v; }

    public String getStatus()               { return status != null ? status : ""; }
    public void setStatus(String v)         { this.status = v; }

    public double getPrice()                { return price; }
    public void setPrice(double v)          { this.price = v; }

    public int getRentDays()                { return rentDays; }
    public void setRentDays(int v)          { this.rentDays = v; }

    public double getTotalPrice()           { return totalPrice > 0 ? totalPrice : price; }
    public void setTotalPrice(double v)     { this.totalPrice = v; }

    public long getRequestedAt()            { return requestedAt; }
    public void setRequestedAt(long v)      { this.requestedAt = v; }

    public long getUpdatedAt()              { return updatedAt; }
    public void setUpdatedAt(long v)        { this.updatedAt = v; }
}
