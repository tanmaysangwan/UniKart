package com.example.unikart.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.unikart.R;
import com.example.unikart.firebase.FirebaseManager;
import com.example.unikart.firebase.OrderRepository;
import com.example.unikart.models.Order;
import com.example.unikart.utils.Constants;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrdersActivity extends AppCompatActivity {

    private static final String TAG = "OrdersActivity";

    private TabLayout tabLayout;
    private RecyclerView rvOrders;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private BottomNavigationView bottomNavigation;
    private OrderRepository orderRepository;
    private OrderAdapter orderAdapter;
    private List<Order> orderList = new ArrayList<>();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        orderRepository = new OrderRepository();
        currentUserId   = FirebaseManager.getInstance().getCurrentUserId();

        tabLayout   = findViewById(R.id.tabLayout);
        rvOrders    = findViewById(R.id.rvOrders);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(orderList);
        rvOrders.setAdapter(orderAdapter);

        tabLayout.addTab(tabLayout.newTab().setText("Buying / Renting"));
        tabLayout.addTab(tabLayout.newTab().setText("Selling / Lending"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { loadOrders(tab.getPosition()); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        setupBottomNavigation();
        
        // Check if specific tab was requested
        int requestedTab = getIntent().getIntExtra("tab", 0);
        if (requestedTab == 1) {
            tabLayout.selectTab(tabLayout.getTabAt(1));
        } else {
            loadOrders(0);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) return;
        bottomNavigation.setSelectedItemId(R.id.nav_orders);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }
            if (id == R.id.nav_add) {
                startActivity(new Intent(this, AddProductActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            if (id == R.id.nav_chat) {
                startActivity(new Intent(this, ChatsListActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            if (id == R.id.nav_orders) return true;
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void loadOrders(int tab) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        OrderRepository.OrderListCallback cb = new OrderRepository.OrderListCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                progressBar.setVisibility(View.GONE);
                orderList.clear();
                orderList.addAll(orders);
                orderAdapter.notifyDataSetChanged();
                tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "loadOrders failed: " + error);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Could not load orders.");
            }
        };

        if (tab == 0) orderRepository.getOrdersAsBuyer(cb);
        else          orderRepository.getOrdersAsSeller(cb);
    }

    // ── Inner Adapter ─────────────────────────────────────────────────────────

    class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderVH> {
        private final List<Order> list;
        OrderAdapter(List<Order> list) { this.list = list; }

        @NonNull
        @Override
        public OrderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order, parent, false);
            return new OrderVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderVH h, int pos) {
            h.bind(list.get(pos));
        }

        @Override public int getItemCount() { return list.size(); }

        class OrderVH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvStatus, tvPrice, tvOtherParty, tvDate, tvType;
            MaterialButton btnAction;

            OrderVH(@NonNull View v) {
                super(v);
                tvTitle      = v.findViewById(R.id.tvOrderTitle);
                tvStatus     = v.findViewById(R.id.tvOrderStatus);
                tvPrice      = v.findViewById(R.id.tvOrderPrice);
                tvOtherParty = v.findViewById(R.id.tvOtherParty);
                tvDate       = v.findViewById(R.id.tvOrderDate);
                tvType       = v.findViewById(R.id.tvOrderType);
                btnAction    = v.findViewById(R.id.btnOrderAction);
            }

            void bind(Order order) {
                tvTitle.setText(order.getProductTitle());
                tvStatus.setText(Constants.statusLabel(order.getStatus()));
                boolean isRentOrder = Constants.PRODUCT_TYPE_RENT.equals(order.getType());
                if (isRentOrder && order.getRentDays() > 0) {
                    tvPrice.setText(String.format(Locale.getDefault(), "₹ %.0f × %d days = ₹ %.0f",
                            order.getPrice(), order.getRentDays(), order.getTotalPrice()));
                } else {
                    tvPrice.setText(String.format(Locale.getDefault(), "₹ %.0f", order.getPrice()));
                }
                tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(new Date(order.getRequestedAt())));
                tvType.setText(Constants.PRODUCT_TYPE_RENT.equals(order.getType()) ? "RENT" : "BUY");

                boolean isBuyer = order.getBuyerId().equals(currentUserId);
                tvOtherParty.setText(isBuyer
                        ? "Seller: " + order.getSellerName()
                        : "Buyer: " + order.getBuyerName());

                // Status color
                int color;
                switch (order.getStatus()) {
                    case Constants.ORDER_STATUS_ACCEPTED:
                    case Constants.ORDER_STATUS_COMPLETED:
                    case Constants.ORDER_STATUS_RETURNED:
                        color = 0xFF2E7D32; break;
                    case Constants.ORDER_STATUS_REJECTED:
                        color = 0xFFD32F2F; break;
                    case Constants.ORDER_STATUS_HANDED_OVER:
                    case Constants.ORDER_STATUS_RETURN_PENDING:
                        color = 0xFF1565C0; break;
                    default:
                        color = 0xFF7C8099;
                }
                tvStatus.setTextColor(color);

                // Action button
                String action = getActionLabel(order, isBuyer);
                if (action != null) {
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText(action);
                    btnAction.setOnClickListener(v -> handleAction(order, isBuyer));
                } else {
                    btnAction.setVisibility(View.GONE);
                }
            }

            private String getActionLabel(Order order, boolean isBuyer) {
                switch (order.getStatus()) {
                    case Constants.ORDER_STATUS_REQUESTED:
                        return isBuyer ? null : "Accept / Reject";
                    case Constants.ORDER_STATUS_ACCEPTED:
                        return isBuyer ? null : "Mark Handed Over";
                    case Constants.ORDER_STATUS_HANDED_OVER:
                        if (Constants.PRODUCT_TYPE_RENT.equals(order.getType())) {
                            return isBuyer ? "Request Return" : null;
                        } else {
                            return isBuyer ? "Confirm Received" : null;
                        }
                    case Constants.ORDER_STATUS_RETURN_PENDING:
                        return isBuyer ? null : "Confirm Return";
                    case Constants.ORDER_STATUS_COMPLETED:
                    case Constants.ORDER_STATUS_RETURNED:
                        return "Leave Review";
                    default:
                        return null;
                }
            }

            private void handleAction(Order order, boolean isBuyer) {
                switch (order.getStatus()) {
                    case Constants.ORDER_STATUS_REQUESTED:
                        if (!isBuyer) showAcceptRejectDialog(order);
                        break;
                    case Constants.ORDER_STATUS_ACCEPTED:
                        if (!isBuyer) updateStatus(order, Constants.ORDER_STATUS_HANDED_OVER);
                        break;
                    case Constants.ORDER_STATUS_HANDED_OVER:
                        if (Constants.PRODUCT_TYPE_RENT.equals(order.getType())) {
                            if (isBuyer) updateStatus(order, Constants.ORDER_STATUS_RETURN_PENDING);
                        } else {
                            if (isBuyer) updateStatus(order, Constants.ORDER_STATUS_COMPLETED);
                        }
                        break;
                    case Constants.ORDER_STATUS_RETURN_PENDING:
                        if (!isBuyer) updateStatus(order, Constants.ORDER_STATUS_RETURNED);
                        break;
                    case Constants.ORDER_STATUS_COMPLETED:
                    case Constants.ORDER_STATUS_RETURNED:
                        showReviewDialog(order, isBuyer);
                        break;
                }
            }

            private void showAcceptRejectDialog(Order order) {
                new AlertDialog.Builder(OrdersActivity.this)
                        .setTitle("Respond to Request")
                        .setMessage("\"" + order.getProductTitle() + "\"\nBuyer: " + order.getBuyerName())
                        .setPositiveButton("Accept", (d, w) ->
                                updateStatus(order, Constants.ORDER_STATUS_ACCEPTED))
                        .setNegativeButton("Reject", (d, w) ->
                                updateStatus(order, Constants.ORDER_STATUS_REJECTED))
                        .setNeutralButton("Cancel", null)
                        .show();
            }

            private void updateStatus(Order order, String newStatus) {
                orderRepository.updateOrderStatus(order.getOrderId(), newStatus,
                        new OrderRepository.OrderCallback() {
                            @Override
                            public void onSuccess(String msg) {
                                order.setStatus(newStatus);
                                int pos = getAdapterPosition();
                                if (pos != RecyclerView.NO_ID) notifyItemChanged(pos);
                                Snackbar.make(rvOrders,
                                        "Status: " + Constants.statusLabel(newStatus),
                                        Snackbar.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onFailure(String error) {
                                Snackbar.make(rvOrders, "Failed: " + error, Snackbar.LENGTH_SHORT).show();
                            }
                        });
            }

            private void showReviewDialog(Order order, boolean isBuyer) {
                View v = LayoutInflater.from(OrdersActivity.this)
                        .inflate(R.layout.dialog_review, null);
                RatingBar ratingBar = v.findViewById(R.id.ratingBar);
                TextView etComment  = v.findViewById(R.id.etComment);

                String revieweeId = isBuyer ? order.getSellerId() : order.getBuyerId();

                new AlertDialog.Builder(OrdersActivity.this)
                        .setTitle("Leave a Review")
                        .setView(v)
                        .setPositiveButton("Submit", (d, w) -> {
                            float rating = ratingBar.getRating();
                            String comment = etComment.getText() != null
                                    ? etComment.getText().toString().trim() : "";
                            orderRepository.submitReview(
                                    order.getOrderId(), order.getProductId(),
                                    revieweeId, rating, comment,
                                    new OrderRepository.OrderCallback() {
                                        @Override public void onSuccess(String msg) {
                                            Snackbar.make(rvOrders, "Review submitted!", Snackbar.LENGTH_SHORT).show();
                                        }
                                        @Override public void onFailure(String err) {
                                            Snackbar.make(rvOrders, "Failed: " + err, Snackbar.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }
}
