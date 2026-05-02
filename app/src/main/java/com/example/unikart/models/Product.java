package com.example.unikart.models;

public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private String type;
    private String category;
    private String condition;
    private String sellerName;
    private String sellerId;
    private String imageUrl;
    private long timestamp;
    
    // Rent-specific
    private int maxRentDays;

    // Seller rating info (loaded from seller's user document)
    private double sellerRating;
    private int sellerReviewCount;

    public Product() {
    }

    public Product(String id, String name, String description, double price, String type,
                   String sellerName, String sellerId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.type = type;
        this.sellerName = sellerName;
        this.sellerId = sellerId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId()                   { return id != null ? id : ""; }
    public void setId(String id)            { this.id = id; }

    public String getName()                 { return name != null ? name : ""; }
    public void setName(String name)        { this.name = name; }

    public String getDescription()          { return description != null ? description : ""; }
    public void setDescription(String d)    { this.description = d; }

    public double getPrice()                { return price; }
    public void setPrice(double price)      { this.price = price; }

    public String getType()                 { return type != null ? type : "BUY"; }
    public void setType(String type)        { this.type = type; }

    public String getCategory()             { return category != null ? category : "Other"; }
    public void setCategory(String c)       { this.category = c; }

    public String getCondition()            { return condition != null ? condition : "Good"; }
    public void setCondition(String c)      { this.condition = c; }

    public String getSellerName()           { return sellerName != null ? sellerName : "Unknown"; }
    public void setSellerName(String s)     { this.sellerName = s; }

    public String getSellerId()             { return sellerId != null ? sellerId : ""; }
    public void setSellerId(String s)       { this.sellerId = s; }

    public String getImageUrl()             { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String url)     { this.imageUrl = url; }

    public long getTimestamp()              { return timestamp; }
    public void setTimestamp(long t)        { this.timestamp = t; }
    
    public int getMaxRentDays()             { return maxRentDays; }
    public void setMaxRentDays(int d)       { this.maxRentDays = d; }

    public double getSellerRating()         { return sellerRating; }
    public void setSellerRating(double r)   { this.sellerRating = r; }
    
    public int getSellerReviewCount()       { return sellerReviewCount; }
    public void setSellerReviewCount(int c) { this.sellerReviewCount = c; }
}
