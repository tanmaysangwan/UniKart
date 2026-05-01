package com.example.unikart.utils;

import com.example.unikart.models.Category;
import com.example.unikart.models.Product;

import java.util.ArrayList;
import java.util.List;

public class DummyDataGenerator {

    public static List<Category> getCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("1", "Books", "📚"));
        categories.add(new Category("2", "Electronics", "💻"));
        categories.add(new Category("3", "Furniture", "🪑"));
        categories.add(new Category("4", "Sports", "⚽"));
        categories.add(new Category("5", "Fashion", "👕"));
        categories.add(new Category("6", "Stationery", "✏️"));
        categories.add(new Category("7", "Bikes", "🚲"));
        return categories;
    }

    public static List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        
        // Books
        Product p1 = new Product(
            "1",
            "Data Structures Textbook",
            "Comprehensive guide to data structures and algorithms. Used for one semester, excellent condition.",
            450.0,
            Constants.PRODUCT_TYPE_BUY,
            "Rahul Sharma",
            "user1"
        );
        p1.setCategory(Constants.CATEGORY_BOOKS);
        p1.setImageUrl("https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800&q=80");
        products.add(p1);

        // Electronics - Laptop
        Product p2 = new Product(
            "2",
            "MacBook Pro 2020",
            "13-inch MacBook Pro, 8GB RAM, 256GB SSD. Perfect working condition with charger.",
            45000.0,
            Constants.PRODUCT_TYPE_BUY,
            "Priya Patel",
            "user2"
        );
        p2.setCategory(Constants.CATEGORY_ELECTRONICS);
        p2.setImageUrl("https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80");
        products.add(p2);

        // Furniture
        Product p3 = new Product(
            "3",
            "Study Table with Chair",
            "Wooden study table with comfortable chair. Great for dorm rooms.",
            2500.0,
            Constants.PRODUCT_TYPE_RENT,
            "Amit Kumar",
            "user3"
        );
        p3.setCategory(Constants.CATEGORY_FURNITURE);
        p3.setImageUrl("https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=800&q=80");
        products.add(p3);

        // Sports
        Product p4 = new Product(
            "4",
            "Cricket Bat & Ball Set",
            "Professional cricket bat with 3 balls. Barely used, like new.",
            1200.0,
            Constants.PRODUCT_TYPE_BUY,
            "Vikram Singh",
            "user4"
        );
        p4.setCategory(Constants.CATEGORY_SPORTS);
        p4.setImageUrl("https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=800&q=80");
        products.add(p4);

        // Clothing
        Product p5 = new Product(
            "5",
            "Winter Jacket",
            "North Face winter jacket, size M. Warm and comfortable.",
            1800.0,
            Constants.PRODUCT_TYPE_BUY,
            "Sneha Reddy",
            "user5"
        );
        p5.setCategory(Constants.CATEGORY_CLOTHING);
        p5.setImageUrl("https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&q=80");
        products.add(p5);

        // Electronics - Calculator
        Product p6 = new Product(
            "6",
            "Scientific Calculator",
            "Casio FX-991EX calculator. Essential for engineering students.",
            800.0,
            Constants.PRODUCT_TYPE_BUY,
            "Arjun Mehta",
            "user6"
        );
        p6.setCategory(Constants.CATEGORY_ELECTRONICS);
        p6.setImageUrl("https://images.unsplash.com/photo-1611224923853-80b023f02d71?w=800&q=80");
        products.add(p6);

        // Sports - Bike
        Product p7 = new Product(
            "7",
            "Mountain Bike",
            "21-speed mountain bike, excellent condition. Perfect for campus commute.",
            8500.0,
            Constants.PRODUCT_TYPE_BUY,
            "Neha Gupta",
            "user7"
        );
        p7.setCategory(Constants.CATEGORY_SPORTS);
        p7.setImageUrl("https://images.unsplash.com/photo-1485965120184-e220f721d03e?w=800&q=80");
        products.add(p7);

        // Electronics - Microwave
        Product p8 = new Product(
            "8",
            "Microwave Oven",
            "800W microwave oven for rent. Monthly rental available.",
            500.0,
            Constants.PRODUCT_TYPE_RENT,
            "Karan Joshi",
            "user8"
        );
        p8.setCategory(Constants.CATEGORY_ELECTRONICS);
        p8.setImageUrl("https://images.unsplash.com/photo-1585659722983-3a675dabf23d?w=800&q=80");
        products.add(p8);

        // Books - Bundle
        Product p9 = new Product(
            "9",
            "Programming Books Bundle",
            "5 programming books: Java, Python, C++, Web Dev, and Algorithms.",
            2000.0,
            Constants.PRODUCT_TYPE_BUY,
            "Divya Iyer",
            "user9"
        );
        p9.setCategory(Constants.CATEGORY_BOOKS);
        p9.setImageUrl("https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=800&q=80");
        products.add(p9);

        // Electronics - Headphones
        Product p10 = new Product(
            "10",
            "Wireless Headphones",
            "Sony WH-1000XM4 noise cancelling headphones. 6 months old.",
            15000.0,
            Constants.PRODUCT_TYPE_BUY,
            "Rohan Desai",
            "user10"
        );
        p10.setCategory(Constants.CATEGORY_ELECTRONICS);
        p10.setImageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80");
        products.add(p10);

        // Additional products with images
        Product p11 = new Product(
            "11",
            "Nike Running Shoes",
            "Brand new Nike running shoes, size 9. Never worn, with original box.",
            3500.0,
            Constants.PRODUCT_TYPE_BUY,
            "Campus Store",
            "campus_store_seed"
        );
        p11.setCategory(Constants.CATEGORY_CLOTHING);
        p11.setImageUrl("https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80");
        products.add(p11);

        Product p12 = new Product(
            "12",
            "Gaming Monitor 24\"",
            "144Hz gaming monitor, 1080p. Perfect for gaming and productivity.",
            12000.0,
            Constants.PRODUCT_TYPE_BUY,
            "Campus Store",
            "campus_store_seed"
        );
        p12.setCategory(Constants.CATEGORY_ELECTRONICS);
        p12.setImageUrl("https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&q=80");
        products.add(p12);

        Product p13 = new Product(
            "13",
            "Ergonomic Office Chair",
            "Comfortable office chair with lumbar support. Adjustable height.",
            4500.0,
            Constants.PRODUCT_TYPE_RENT,
            "Campus Store",
            "campus_store_seed"
        );
        p13.setCategory(Constants.CATEGORY_FURNITURE);
        p13.setImageUrl("https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=800&q=80");
        products.add(p13);

        Product p14 = new Product(
            "14",
            "Chemistry Lab Coat",
            "White lab coat, size L. Required for chemistry practicals.",
            600.0,
            Constants.PRODUCT_TYPE_BUY,
            "Campus Store",
            "campus_store_seed"
        );
        p14.setCategory(Constants.CATEGORY_CLOTHING);
        p14.setImageUrl("https://images.unsplash.com/photo-1603126857599-f6e157fa2fe6?w=800&q=80");
        products.add(p14);

        Product p15 = new Product(
            "15",
            "Steel Water Bottle",
            "Insulated steel water bottle, 1L capacity. Keeps drinks cold for 24 hours.",
            450.0,
            Constants.PRODUCT_TYPE_BUY,
            "Campus Store",
            "campus_store_seed"
        );
        p15.setCategory(Constants.CATEGORY_OTHER);
        p15.setImageUrl("https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&q=80");
        products.add(p15);

        return products;
    }
}
