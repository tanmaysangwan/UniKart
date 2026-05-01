package com.example.unikart.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.unikart.R;
import com.google.android.material.appbar.MaterialToolbar;

public class ProjectInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_info);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Project Info / Viva Mode");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvContent = findViewById(R.id.tvContent);
        tvContent.setText(getProjectInfo());
    }

    private String getProjectInfo() {
        return "═══════════════════════════════════════════════════\n" +
                "UNIKART - PROJECT INFORMATION\n" +
                "═══════════════════════════════════════════════════\n\n" +

                "📱 PROJECT OVERVIEW\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Name: UniKart\n" +
                "Type: Campus Marketplace Android App\n" +
                "Purpose: Peer-to-peer buying, selling & renting\n" +
                "Target: University students\n" +
                "Version: 1.0\n\n" +

                "🛠️ TECHNOLOGY STACK\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Language: Java\n" +
                "Platform: Android (API 24-36)\n" +
                "UI: Material Design 3\n" +
                "Architecture: MVC Pattern\n\n" +

                "Backend:\n" +
                "• Firebase Authentication\n" +
                "• Cloud Firestore (NoSQL)\n" +
                "• Firebase Storage\n" +
                "• Cloudinary (Image upload)\n\n" +

                "Libraries:\n" +
                "• Glide 4.16.0 (Image loading)\n" +
                "• OkHttp 4.12.0 (HTTP client)\n" +
                "• Material Components\n" +
                "• RecyclerView\n\n" +

                "✨ KEY FEATURES (12 TOTAL)\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "1. Authentication\n" +
                "   • University email verification\n" +
                "   • Firebase Auth integration\n" +
                "   • Session management\n\n" +

                "2. Marketplace Feed\n" +
                "   • 40+ seeded products\n" +
                "   • Grid layout with images\n" +
                "   • Real-time updates\n\n" +

                "3. Advanced Filtering\n" +
                "   • Category filter\n" +
                "   • Price range\n" +
                "   • Seller rating filter\n" +
                "   • Condition filter\n" +
                "   • Multiple sort options\n\n" +

                "4. Product Details\n" +
                "   • Full information display\n" +
                "   • Seller profile\n" +
                "   • Buy/Rent actions\n\n" +

                "5. Add Product\n" +
                "   • Image upload (Gallery/Camera)\n" +
                "   • Cloudinary integration\n" +
                "   • Form validation\n\n" +

                "6. Real-time Chat\n" +
                "   • One-on-one messaging\n" +
                "   • Firestore listeners\n" +
                "   • Message timestamps\n\n" +

                "7. Order Lifecycle\n" +
                "   • 7-stage status tracking\n" +
                "   • Buyer/Seller views\n" +
                "   • Action buttons\n\n" +

                "8. Reviews & Ratings\n" +
                "   • 5-star rating system\n" +
                "   • Written comments\n" +
                "   • Automatic rating updates\n\n" +

                "9. User Profile\n" +
                "   • Statistics display\n" +
                "   • Verification badge\n" +
                "   • Profile picture\n\n" +

                "10. Edit Profile\n" +
                "    • Update information\n" +
                "    • Image upload\n" +
                "    • Real-time sync\n\n" +

                "11. Bottom Navigation\n" +
                "    • 5 main tabs\n" +
                "    • Persistent UI\n" +
                "    • Smooth transitions\n\n" +

                "12. Developer Tools\n" +
                "    • Seed marketplace\n" +
                "    • Repair images\n" +
                "    • Firebase diagnostics\n\n" +

                "🗄️ DATABASE STRUCTURE\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Firestore Collections (6):\n\n" +

                "1. users\n" +
                "   • uid, name, email, studentId\n" +
                "   • rating, reviewCount\n" +
                "   • profilePicture, bio\n\n" +

                "2. products\n" +
                "   • productId, title, description\n" +
                "   • price, type (BUY/RENT)\n" +
                "   • category, condition\n" +
                "   • ownerId, imageUrl\n\n" +

                "3. orders\n" +
                "   • orderId, productId\n" +
                "   • buyerId, sellerId\n" +
                "   • status (7 stages)\n" +
                "   • price, timestamps\n\n" +

                "4. reviews\n" +
                "   • reviewId, orderId\n" +
                "   • reviewerId, revieweeId\n" +
                "   • rating (1-5), comment\n" +
                "   • productTitle, timestamp\n\n" +

                "5. chats\n" +
                "   • chatId, participants\n" +
                "   • lastMessage, lastMessageTime\n" +
                "   • productId, productTitle\n\n" +

                "6. messages (subcollection)\n" +
                "   • messageId, senderId\n" +
                "   • text, timestamp\n" +
                "   • isRead\n\n" +

                "📂 PROJECT STRUCTURE\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Activities: 14 screens\n" +
                "• SplashActivity\n" +
                "• WelcomeActivity\n" +
                "• LoginActivity\n" +
                "• RegisterActivity\n" +
                "• HomeActivity\n" +
                "• ProductDetailActivity\n" +
                "• AddProductActivity\n" +
                "• ChatsListActivity\n" +
                "• ChatActivity\n" +
                "• OrdersActivity\n" +
                "• ProfileActivity\n" +
                "• EditProfileActivity\n" +
                "• ReviewsActivity\n" +
                "• DiagnosticsActivity\n\n" +

                "Adapters: 5 RecyclerViews\n" +
                "• ProductAdapter\n" +
                "• CategoryAdapter\n" +
                "• ChatListAdapter\n" +
                "• ChatMessageAdapter\n" +
                "• ReviewAdapter\n\n" +

                "Repositories: 6 classes\n" +
                "• FirebaseManager\n" +
                "• AuthRepository\n" +
                "• ProductRepository\n" +
                "• OrderRepository\n" +
                "• ChatRepository\n" +
                "• AdminRepository\n\n" +

                "Models: 8 data classes\n" +
                "• User, Product, Order\n" +
                "• Review, ChatThread\n" +
                "• ChatMessage, Category\n" +
                "• FilterState\n\n" +

                "Utils: 4 helpers\n" +
                "• Constants\n" +
                "• SessionManager\n" +
                "• CloudinaryUploader\n" +
                "• DummyDataGenerator\n\n" +

                "🎯 ORDER LIFECYCLE (7 STAGES)\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "1. REQUESTED - Buyer initiates\n" +
                "2. ACCEPTED - Seller approves\n" +
                "3. REJECTED - Seller declines\n" +
                "4. HANDED_OVER - Item delivered\n" +
                "5. COMPLETED - Transaction done\n" +
                "6. RETURN_PENDING - Rental return\n" +
                "7. RETURNED - Rental completed\n\n" +

                "📊 PRODUCT CATEGORIES (6)\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "• Books\n" +
                "• Electronics\n" +
                "• Furniture\n" +
                "• Clothing\n" +
                "• Sports\n" +
                "• Other\n\n" +

                "🎨 UI HIGHLIGHTS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "• Material Design 3\n" +
                "• Gradient backgrounds\n" +
                "• Rounded corners (16dp)\n" +
                "• Card elevations\n" +
                "• Smooth animations\n" +
                "• Responsive layouts\n" +
                "• Bottom navigation\n" +
                "• Bottom sheets\n" +
                "• Empty states\n" +
                "• Loading indicators\n\n" +

                "🔐 SECURITY FEATURES\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "• Firebase Authentication\n" +
                "• University email validation\n" +
                "• Session management\n" +
                "• Firestore security rules\n" +
                "• Secure image upload\n\n" +

                "📈 PROJECT STATISTICS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "Total Activities: 14\n" +
                "Total Adapters: 5\n" +
                "Total Repositories: 6\n" +
                "Total Models: 8\n" +
                "Total Layouts: 21\n" +
                "Firestore Collections: 6\n" +
                "Seeded Products: 42\n" +
                "Lines of Code: ~8,000+\n\n" +

                "🚀 DEMO FLOW\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "1. Splash → Welcome\n" +
                "2. Login/Register\n" +
                "3. Home Feed\n" +
                "4. Filter & Search\n" +
                "5. Product Detail\n" +
                "6. Chat with Seller\n" +
                "7. Add Product\n" +
                "8. Orders Management\n" +
                "9. Reviews & Ratings\n" +
                "10. Profile & Edit\n" +
                "11. Logout\n\n" +

                "💡 FUTURE ENHANCEMENTS\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "• Push notifications\n" +
                "• Payment integration\n" +
                "• Location-based discovery\n" +
                "• ML recommendations\n" +
                "• Dispute resolution\n" +
                "• Admin dashboard\n" +
                "• Multi-university support\n" +
                "• Wishlist feature\n" +
                "• Analytics dashboard\n" +
                "• Social features\n\n" +

                "═══════════════════════════════════════════════════\n" +
                "For detailed viva preparation, see VIVA_GUIDE.txt\n" +
                "in project root directory.\n" +
                "═══════════════════════════════════════════════════";
    }
}
