# 🎓 UniKart

<div align="center">

**A Campus Marketplace for Students**

*Buy, Sell, and Rent — All Within Your University Community*

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)
[![Java](https://img.shields.io/badge/Language-Java-blue.svg)](https://www.java.com/)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-brightgreen.svg)](https://developer.android.com/about/versions/nougat)

</div>

---

## 📖 Overview

**UniKart** is a secure, university-exclusive marketplace Android application that enables students to buy, sell, and rent items within their campus community. Built with Firebase and modern Android development practices, UniKart creates a trusted ecosystem where students can safely transact with verified peers.

### ✨ Key Highlights

- 🔐 **University Email Verification** — Only verified students can join
- 💰 **Dual Transaction Modes** — Buy or rent items with flexible pricing
- 💬 **Real-time Chat** — Negotiate directly with sellers
- 📦 **Order Management** — Track requests from submission to completion
- ⭐ **Rating System** — Build trust through peer reviews
- 🔔 **Push Notifications** — Stay updated on orders and messages
- 📱 **Modern UI** — Clean, intuitive Material Design interface

---

## 🎯 Features

### 🛍️ Marketplace

- **Browse Products** — Explore listings with category filters, search, and sorting
- **Advanced Filtering** — Filter by type (buy/rent), category, condition, price, and seller ratings
- **Product Details** — View comprehensive information including seller ratings and reviews
- **Smart Recommendations** — Discover products based on seller reputation and ratings

### 🏷️ Listing Management

- **Create Listings** — Post items for sale or rent with images, descriptions, and pricing
- **Image Upload** — Cloudinary integration for fast, reliable image hosting
- **Edit & Delete** — Manage your active listings with full control
- **Availability Toggle** — Mark items as available/unavailable without deleting
- **Rent Configuration** — Set daily rates and maximum rental periods

### 💬 Communication

- **Real-time Chat** — Instant messaging with buyers and sellers
- **Product Context** — Chats linked to specific products for easy reference
- **Message History** — Access all conversations in one place
- **In-app Notifications** — See new messages without leaving the current screen

### 📦 Order System

#### For Buyers:
- **Request to Buy/Rent** — Submit purchase or rental requests with custom durations
- **Track Orders** — Monitor order status from request to completion
- **Confirm Receipt** — Mark items as received after handover
- **Return Items** — Initiate returns for rental transactions
- **Leave Reviews** — Rate sellers after successful transactions

#### For Sellers:
- **Accept/Reject Requests** — Review and respond to buyer requests
- **Mark Handover** — Confirm when items are handed over
- **Confirm Returns** — Verify returned rental items
- **Receive Reviews** — Build reputation through buyer feedback

### ⭐ Rating & Review System

- **Seller Ratings** — Average ratings displayed on all listings
- **Review Count** — See how many transactions a seller has completed
- **Detailed Reviews** — Read comments from previous buyers
- **Trust Indicators** — Filter by highly-rated, trusted, or new sellers
- **Product Reviews** — View reviews specific to each listing

### 👤 User Profiles

- **Profile Management** — Update name, student ID, and profile picture
- **My Listings** — View and manage all your active listings
- **Order History** — Access complete transaction history
- **Rating Display** — Showcase your seller reputation
- **Review Portfolio** — Display all reviews received

### 🔔 Notifications

- **Push Notifications** — FCM-powered real-time alerts
- **Order Updates** — Get notified when orders change status
- **New Messages** — Instant chat notifications
- **In-app Banners** — Non-intrusive notifications while using the app
- **Deep Linking** — Tap notifications to jump directly to relevant screens

---

## 🏗️ Architecture

### Tech Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Java |
| **UI Framework** | Android SDK, Material Design Components |
| **Backend** | Firebase (Auth, Firestore, Storage, Messaging) |
| **Image Hosting** | Cloudinary |
| **Image Loading** | Glide |
| **Networking** | OkHttp |
| **Min SDK** | Android 7.0 (API 24) |
| **Target SDK** | Android 14 (API 36) |

### Project Structure

```
app/src/main/java/com/example/unikart/
├── activities/          # UI screens and user interactions
│   ├── HomeActivity.java              # Main marketplace feed
│   ├── ProductDetailActivity.java     # Product information
│   ├── AddProductActivity.java        # Create new listings
│   ├── EditListingActivity.java       # Modify existing listings
│   ├── ChatActivity.java              # One-on-one messaging
│   ├── ChatsListActivity.java         # All conversations
│   ├── OrdersActivity.java            # Order management
│   ├── ProfileActivity.java           # User profile
│   ├── LoginActivity.java             # Authentication
│   └── RegisterActivity.java          # User registration
│
├── adapters/            # RecyclerView adapters for lists
│   ├── ProductAdapter.java            # Product grid
│   ├── CategoryAdapter.java           # Category chips
│   ├── ChatListAdapter.java           # Conversation list
│   ├── ChatMessageAdapter.java        # Message bubbles
│   └── ReviewAdapter.java             # Review cards
│
├── firebase/            # Backend data layer
│   ├── FirebaseManager.java           # Singleton Firebase instance
│   ├── AuthRepository.java            # User authentication
│   ├── ProductRepository.java         # Product CRUD operations
│   ├── OrderRepository.java           # Order & review management
│   ├── ChatRepository.java            # Real-time messaging
│   └── AdminRepository.java           # Admin utilities
│
├── models/              # Data models
│   ├── Product.java                   # Product entity
│   ├── User.java                      # User profile
│   ├── Order.java                     # Transaction record
│   ├── Review.java                    # Rating & feedback
│   ├── ChatThread.java                # Conversation metadata
│   ├── ChatMessage.java               # Individual message
│   ├── Category.java                  # Product category
│   └── FilterState.java               # Search & filter state
│
├── services/            # Background services
│   └── UniKartFCMService.java         # Push notification handler
│
└── utils/               # Helper classes
    ├── CloudinaryUploader.java        # Image upload
    ├── NotificationHelper.java        # System notifications
    ├── NotificationSender.java        # FCM message sender
    ├── FCMTokenManager.java           # Device token management
    ├── InAppNotificationManager.java  # In-app banners
    ├── AppLifecycleTracker.java       # App state monitoring
    ├── SessionManager.java            # User session
    └── Constants.java                 # App-wide constants
```

### Firebase Collections

```
Firestore Database:
├── users/                    # User profiles
│   └── {userId}
│       ├── uid: string
│       ├── name: string
│       ├── email: string
│       ├── studentId: string
│       ├── rating: number
│       ├── reviewCount: number
│       ├── profilePicture: string
│       ├── fcmToken: string
│       └── createdAt: timestamp
│
├── products/                 # Product listings
│   └── {productId}
│       ├── productId: string
│       ├── title: string
│       ├── description: string
│       ├── price: number
│       ├── type: "BUY" | "RENT"
│       ├── category: string
│       ├── condition: string
│       ├── ownerId: string
│       ├── ownerName: string
│       ├── imageUrl: string
│       ├── available: boolean
│       ├── maxRentDays: number
│       └── createdAt: timestamp
│
├── orders/                   # Transactions
│   └── {orderId}
│       ├── orderId: string
│       ├── productId: string
│       ├── productTitle: string
│       ├── productImageUrl: string
│       ├── buyerId: string
│       ├── buyerName: string
│       ├── sellerId: string
│       ├── sellerName: string
│       ├── type: "BUY" | "RENT"
│       ├── status: string
│       ├── price: number
│       ├── rentDays: number
│       ├── totalPrice: number
│       ├── requestedAt: timestamp
│       └── updatedAt: timestamp
│
├── reviews/                  # Ratings & feedback
│   └── {reviewId}
│       ├── reviewId: string
│       ├── orderId: string
│       ├── productId: string
│       ├── productTitle: string
│       ├── reviewerId: string
│       ├── reviewerName: string
│       ├── reviewerProfilePic: string
│       ├── revieweeId: string
│       ├── rating: number (1-5)
│       ├── comment: string
│       ├── transactionType: string
│       └── timestamp: timestamp
│
└── chats/                    # Conversations
    └── {chatId}
        ├── chatId: string
        ├── buyerId: string
        ├── buyerName: string
        ├── sellerId: string
        ├── sellerName: string
        ├── productId: string
        ├── productTitle: string
        ├── lastMessage: string
        ├── lastMessageAt: timestamp
        ├── createdAt: timestamp
        └── messages/         # Subcollection
            └── {messageId}
                ├── messageId: string
                ├── chatId: string
                ├── senderId: string
                ├── text: string
                └── sentAt: timestamp
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Arctic Fox or later
- **JDK** 11 or higher
- **Android SDK** with API 24+ support
- **Firebase Account** (free tier works)
- **Cloudinary Account** (free tier works)

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/unikart.git
cd unikart
```

#### 2. Firebase Setup

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project (or use existing)
3. Add an Android app with package name: `com.example.unikart`
4. Download `google-services.json`
5. Place it in `app/` directory

**Enable Firebase Services:**

- **Authentication** → Sign-in method → Email/Password → Enable
- **Firestore Database** → Create database → Start in test mode
- **Storage** → Get started → Start in test mode
- **Cloud Messaging** → No additional setup needed

**Firestore Security Rules** (for production):

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can read all profiles, but only update their own
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    
    // Products are public, but only owners can modify
    match /products/{productId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth.uid == resource.data.ownerId;
    }
    
    // Orders visible to buyer and seller only
    match /orders/{orderId} {
      allow read: if request.auth != null && 
        (request.auth.uid == resource.data.buyerId || 
         request.auth.uid == resource.data.sellerId);
      allow create: if request.auth != null;
      allow update: if request.auth != null && 
        (request.auth.uid == resource.data.buyerId || 
         request.auth.uid == resource.data.sellerId);
    }
    
    // Reviews are public
    match /reviews/{reviewId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
    }
    
    // Chats visible to participants only
    match /chats/{chatId} {
      allow read: if request.auth != null && 
        (request.auth.uid == resource.data.buyerId || 
         request.auth.uid == resource.data.sellerId);
      allow create: if request.auth != null;
      allow update: if request.auth != null && 
        (request.auth.uid == resource.data.buyerId || 
         request.auth.uid == resource.data.sellerId);
      
      match /messages/{messageId} {
        allow read: if request.auth != null;
        allow create: if request.auth != null;
      }
    }
  }
}
```

#### 3. Cloudinary Setup

1. Sign up at [Cloudinary](https://cloudinary.com/)
2. Go to Dashboard → Copy your **Cloud Name**
3. Go to Settings → Upload → Add upload preset
   - Name: `unikart_products`
   - Signing Mode: **Unsigned**
   - Save
4. Open `app/src/main/java/com/example/unikart/utils/CloudinaryUploader.java`
5. Replace placeholders:

```java
private static final String CLOUD_NAME = "your_cloud_name";
private static final String UPLOAD_PRESET = "unikart_products";
```

#### 4. Configure Email Domain

By default, UniKart restricts registration to specific university email domains.

Edit `app/src/main/java/com/example/unikart/utils/Constants.java`:

```java
public static final List<String> ALLOWED_EMAIL_DOMAINS = Arrays.asList(
    "@youruniversity.edu",
    "@students.youruniversity.edu"
);
```

Or allow all domains for testing:

```java
public static boolean isValidUniversityEmail(String email) {
    return email != null && email.contains("@");
}
```

#### 5. Build and Run

1. Open project in Android Studio
2. Sync Gradle files
3. Connect Android device or start emulator
4. Click **Run** ▶️

---

## 📱 User Guide

### Getting Started

1. **Register** — Sign up with your university email
2. **Verify Email** — Check your inbox for verification link (optional)
3. **Login** — Access the marketplace
4. **Browse** — Explore available products

### Buying an Item

1. Browse or search for products
2. Tap a product to view details
3. Check seller rating and reviews
4. Tap **Chat** to ask questions
5. Tap **Buy Now** to send a purchase request
6. Wait for seller to accept
7. Coordinate handover via chat
8. Confirm receipt in **My Orders**
9. Leave a review for the seller

### Renting an Item

1. Find a product marked **RENT**
2. View daily rate and max rental period
3. Tap **Rent Now**
4. Enter number of days needed
5. Review total cost and confirm
6. Wait for seller acceptance
7. Coordinate pickup via chat
8. After use, tap **Return Item**
9. Wait for seller to confirm return
10. Leave a review

### Selling an Item

1. Tap **+** in bottom navigation
2. Upload product photo
3. Fill in details (title, description, price, category, condition)
4. Choose **Buy** or **Rent** (set daily rate and max days for rent)
5. Tap **List Product**
6. Manage requests in **Orders** → **Selling** tab
7. Accept/reject buyer requests
8. Mark as **Handed Over** after delivery
9. For rentals, confirm return when item is back

### Managing Orders

**Buyer View:**
- **Requested** — Waiting for seller response
- **Accepted** — Seller approved, coordinate handover
- **Handed Over** — Seller marked as delivered, confirm receipt
- **Completed** — Transaction finished (Buy)
- **Return Pending** — You requested return (Rent)
- **Returned** — Seller confirmed return (Rent)

**Seller View:**
- **Requested** — New request, accept or reject
- **Accepted** — Approved, coordinate handover
- **Handed Over** — You marked as delivered, wait for buyer confirmation
- **Completed** — Buyer confirmed receipt (Buy)
- **Return Pending** — Buyer wants to return (Rent)
- **Returned** — You confirmed return (Rent)

---

## 🎨 Screenshots

<div align="center">

| Home Screen | Product Detail | Chat |
|------------|----------------|------|
| Browse marketplace with filters | View product info & seller ratings | Real-time messaging |

| Orders | Profile | Add Listing |
|--------|---------|-------------|
| Track buy/sell transactions | Manage profile & listings | Create new product |

</div>

---

## 🔧 Configuration

### Categories

Edit categories in `Constants.java`:

```java
public static final String CATEGORY_BOOKS = "Books";
public static final String CATEGORY_ELECTRONICS = "Electronics";
public static final String CATEGORY_FURNITURE = "Furniture";
// Add more categories...

public static final List<String> ALL_CATEGORIES = Arrays.asList(
    CATEGORY_BOOKS,
    CATEGORY_ELECTRONICS,
    CATEGORY_FURNITURE
    // Add to list...
);
```

### Order Status Flow

```
BUY Flow:
REQUESTED → ACCEPTED → HANDED_OVER → COMPLETED
         ↘ REJECTED

RENT Flow:
REQUESTED → ACCEPTED → HANDED_OVER → RETURN_PENDING → RETURNED
         ↘ REJECTED
```

### Notification Channels

- **Chat Messages** — New message alerts
- **Order Updates** — Status change notifications
- **General** — App-wide announcements

---

## 🛡️ Security Features

- ✅ **Email Verification** — University domain validation
- ✅ **Firebase Authentication** — Secure user management
- ✅ **Firestore Security Rules** — Data access control
- ✅ **Input Validation** — Prevent malicious data
- ✅ **Image Compression** — Reduce upload size
- ✅ **FCM Token Management** — Secure push notifications
- ✅ **Session Management** — Automatic logout on token expiry

---

## 🐛 Troubleshooting

### Firebase Issues

**Problem:** "API key not valid" error

**Solution:**
1. Ensure `google-services.json` is in `app/` directory
2. Verify package name matches Firebase project
3. Rebuild project: Build → Clean Project → Rebuild Project

**Problem:** "Email/Password sign-in not enabled"

**Solution:**
1. Firebase Console → Authentication → Sign-in method
2. Enable Email/Password provider
3. Save changes

### Cloudinary Issues

**Problem:** "Cloudinary not configured" error

**Solution:**
1. Verify `CLOUD_NAME` in `CloudinaryUploader.java`
2. Ensure upload preset is **unsigned**
3. Check preset name matches exactly

### Notification Issues

**Problem:** Not receiving push notifications

**Solution:**
1. Grant notification permission in app settings
2. Ensure FCM is enabled in Firebase Console
3. Check device internet connection
4. Verify `google-services.json` includes FCM config

### Build Issues

**Problem:** Gradle sync failed

**Solution:**
```bash
# Clear Gradle cache
./gradlew clean
./gradlew build --refresh-dependencies
```

---

## 🚧 Known Limitations

- **Payment Integration** — Currently no in-app payments (coordinate offline)
- **Location Services** — No GPS-based filtering (campus-wide only)
- **Multi-language** — English only
- **Web Version** — Android app only (no web interface)
- **Admin Panel** — No dedicated admin dashboard

---

## 🗺️ Roadmap

### Planned Features

- [ ] **In-app Payments** — Stripe/Razorpay integration
- [ ] **Location Filtering** — Find items near you
- [ ] **Wishlist** — Save favorite products
- [ ] **Price Alerts** — Get notified of price drops
- [ ] **Barcode Scanner** — Quick book listing via ISBN
- [ ] **Social Sharing** — Share listings on social media
- [ ] **Dark Mode** — Eye-friendly night theme
- [ ] **Multi-language** — Support for regional languages
- [ ] **Admin Dashboard** — Web-based moderation tools
- [ ] **Analytics** — User behavior insights

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable names
- Add comments for complex logic
- Write descriptive commit messages

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **Your Name** — *Initial work* — [GitHub](https://github.com/yourusername)

---

## 🙏 Acknowledgments

- **Firebase** — Backend infrastructure
- **Cloudinary** — Image hosting and optimization
- **Material Design** — UI components and guidelines
- **Glide** — Efficient image loading
- **OkHttp** — Reliable networking

---

## 📞 Support

For questions, issues, or feature requests:

- 📧 Email: support@unikart.app
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/unikart/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/yourusername/unikart/discussions)

---

<div align="center">

**Made with ❤️ for Students, by Students**

⭐ Star this repo if you find it helpful!

</div>
