package com.example.eventlotteryapp.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlotteryapp.CsvExportController;
import com.example.eventlotteryapp.NotificationController;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.example.eventlotteryapp.models.User;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Displays and manages the waiting list for an event.
 * Shows entrants with their status (Pending, Selected, Declined) and allows filtering.
 */
public class EntrantListActivity extends AppCompatActivity {

    private static final String TAG = "EntrantListActivity";
    
    private String eventId;
    private FirebaseFirestore firestore;
    private NotificationController notificationController;
    private CsvExportController csvExportController;
    
    // UI Components
    private TextView eventNameText;
    private TextView registrationEndsText;
    private TextView totalEntrantsText;
    private TextView slotsAvailableText;
    private EditText searchEditText;
    private RecyclerView entrantsRecyclerView;
    private Button runLotteryButton;
    private Button exportCsvButton;
    private MapView entrantsMapView;
    private IMapController mapController;
    private boolean requiresGeolocation = false;
    
    // Filter tabs
    private MaterialButton filterAllTab;
    private MaterialButton filterPendingTab;
    private MaterialButton filterSelectedTab;
    private MaterialButton filterDeclinedTab;
    private MaterialButton filterCancelledTab;
    
    private String currentFilter = "all"; // all, pending, selected, declined, cancelled
    private List<EntrantWithStatus> allEntrants = new ArrayList<>();
    private List<EntrantWithStatus> filteredEntrants = new ArrayList<>();
    private WaitingListAdapter adapter;
    private java.util.Set<String> loadedEntrantIds = new java.util.HashSet<>(); // Track loaded IDs to prevent duplicates
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private SimpleDateFormat joinDateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_waiting_list);
        
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        firestore = FirebaseFirestore.getInstance();
        notificationController = new NotificationController();
        csvExportController = new CsvExportController();
        
        initializeViews();
        setupClickListeners();
        loadEventData();
        loadEntrants();
    }
    
    private void initializeViews() {
        eventNameText = findViewById(R.id.event_name_text);
        registrationEndsText = findViewById(R.id.registration_ends_text);
        totalEntrantsText = findViewById(R.id.total_entrants_text);
        slotsAvailableText = findViewById(R.id.slots_available_text);
        searchEditText = findViewById(R.id.search_edit_text);
        entrantsRecyclerView = findViewById(R.id.entrants_recycler_view);
        runLotteryButton = findViewById(R.id.run_lottery_button);
        exportCsvButton = findViewById(R.id.export_csv_button);
        entrantsMapView = findViewById(R.id.entrants_map_view);
        
        // Initialize OpenStreetMap
        if (entrantsMapView != null) {
            // Set user agent (required by OpenStreetMap)
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
            Configuration.getInstance().setUserAgentValue(getPackageName());
            
            // Set tile source
            entrantsMapView.setTileSource(TileSourceFactory.MAPNIK);
            
            // Enable multi-touch controls
            entrantsMapView.setMultiTouchControls(true);
            
            // Get map controller
            mapController = entrantsMapView.getController();
            mapController.setZoom(10.0);
        }
        
        filterAllTab = findViewById(R.id.filter_all_tab);
        filterPendingTab = findViewById(R.id.filter_pending_tab);
        filterSelectedTab = findViewById(R.id.filter_selected_tab);
        filterDeclinedTab = findViewById(R.id.filter_declined_tab);
        filterCancelledTab = findViewById(R.id.filter_cancelled_tab);
        
        // Back button
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
        
        // Setup RecyclerView
        adapter = new WaitingListAdapter(filteredEntrants);
        entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        entrantsRecyclerView.setAdapter(adapter);

        // Setup search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntrants();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Initialize filter tabs
        updateFilterTabs();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (entrantsMapView != null) {
            entrantsMapView.onResume();
        }
        // Refresh data when returning to this activity
        if (eventId != null) {
            loadEventData();
            loadEntrants();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (entrantsMapView != null) {
            entrantsMapView.onPause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (entrantsMapView != null) {
            entrantsMapView.onDetach();
        }
    }
    
    private void setupClickListeners() {
        // Filter tabs
        filterAllTab.setOnClickListener(v -> setFilter("all"));
        filterPendingTab.setOnClickListener(v -> setFilter("pending"));
        filterSelectedTab.setOnClickListener(v -> setFilter("selected"));
        filterDeclinedTab.setOnClickListener(v -> setFilter("declined"));
        filterCancelledTab.setOnClickListener(v -> setFilter("cancelled"));
        
        // Run Lottery button - uses same logic as OrganizerEventDetailsActivity
        runLotteryButton.setOnClickListener(v -> runLottery());
        
        // Export CSV button
        exportCsvButton.setOnClickListener(v -> exportCsv());
        
        // Bottom navigation tabs
        TabLayout bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    // Dashboard
                    Intent intent = new Intent(EntrantListActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else if (position == 1) {
                    // Create
                    Intent intent = new Intent(EntrantListActivity.this, CreateEventActivity.class);
                    startActivity(intent);
                    finish();
                } else if (position == 2) {
                    // Notifications - handled by OrganizerHomePage
                    Intent intent = new Intent(EntrantListActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.putExtra("tab", "notifications");
                    startActivity(intent);
                    finish();
                } else if (position == 3) {
                    // Profile - handled by OrganizerHomePage
                    Intent intent = new Intent(EntrantListActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.putExtra("tab", "profile");
                    startActivity(intent);
                    finish();
                }
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterTabs();
        filterEntrants();
    }
    
    private void updateFilterTabs() {
        List<MaterialButton> buttons = java.util.Arrays.asList(
            filterAllTab, filterPendingTab, filterSelectedTab, filterDeclinedTab, filterCancelledTab
        );
        
        MaterialButton selectedButton = null;
        switch (currentFilter) {
            case "all":
                selectedButton = filterAllTab;
                break;
            case "pending":
                selectedButton = filterPendingTab;
                break;
            case "selected":
                selectedButton = filterSelectedTab;
                break;
            case "declined":
                selectedButton = filterDeclinedTab;
                break;
            case "cancelled":
                selectedButton = filterCancelledTab;
                break;
        }
        
        updateFilterButtonStyles(buttons, selectedButton);
    }
    
    private void updateFilterButtonStyles(List<MaterialButton> buttons, MaterialButton selected) {
        int selectedBgColor = getResources().getColor(R.color.filter_selected_bg, null);
        int selectedTextColor = getResources().getColor(R.color.filter_selected_text, null);
        int unselectedBgColor = getResources().getColor(R.color.filter_unselected_bg, null);
        int unselectedTextColor = getResources().getColor(R.color.filter_unselected_text, null);
        
        for (MaterialButton btn : buttons) {
            if (btn == selected) {
                btn.setBackgroundTintList(ColorStateList.valueOf(selectedBgColor));
                btn.setTextColor(selectedTextColor);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(unselectedBgColor));
                btn.setTextColor(unselectedTextColor);
            }
        }
    }
    
    private void loadEventData() {
        firestore.collection("Events").document(eventId)
                .get()
            .addOnSuccessListener(document -> {
                if (!document.exists()) {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                // Event name
                String title = document.getString("title");
                if (title == null) title = document.getString("Name");
                if (title != null) {
                    eventNameText.setText(title);
                }
                
                // Registration ends date
                Date registrationCloseDate = document.getDate("registrationCloseDate");
                if (registrationCloseDate != null) {
                    String dateStr = dateFormat.format(registrationCloseDate);
                    registrationEndsText.setText("Registration Ends: " + dateStr);
                } else {
                    registrationEndsText.setText("Registration Ends: TBD");
                }
                
                // Check if geolocation is required (for reference, but map is always visible)
                Boolean requireGeolocation = document.getBoolean("requireGeolocation");
                requiresGeolocation = (requireGeolocation != null && requireGeolocation);
                
                // Map is always visible
                if (entrantsMapView != null) {
                    entrantsMapView.setVisibility(View.VISIBLE);
                }
                
                // Update statistics
                updateStatistics(document);
                
                // Always load map markers (if any location data exists)
                if (entrantsMapView != null) {
                    loadEntrantLocations();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event", e);
                Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void updateStatistics(DocumentSnapshot document) {
        // Total Entrants (all entrants: waiting + selected + accepted + declined + cancelled)
        List<String> waitingList = (List<String>) document.get("waitingListEntrantIds");
        List<String> selectedList = (List<String>) document.get("selectedEntrantIds");
        List<String> acceptedList = (List<String>) document.get("acceptedEntrantIds");
        List<String> cancelledList = (List<String>) document.get("cancelledEntrantIds");
        List<String> declinedList = (List<String>) document.get("declinedEntrantIds");
        
        int waitingCount = (waitingList != null) ? waitingList.size() : 0;
        int selectedCount = (selectedList != null) ? selectedList.size() : 0;
        int acceptedCount = (acceptedList != null) ? acceptedList.size() : 0;
        int cancelledCount = (cancelledList != null) ? cancelledList.size() : 0;
        int declinedCount = (declinedList != null) ? declinedList.size() : 0;
        int totalEntrants = waitingCount + selectedCount + acceptedCount + cancelledCount + declinedCount;
        
        totalEntrantsText.setText("Total Entrants: " + totalEntrants);
        
        // Slots Available - use acceptedEntrantIds as finalized count
        Long maxParticipantsLong = document.getLong("maxParticipants");
        int maxParticipants = (maxParticipantsLong != null) ? maxParticipantsLong.intValue() : 0;
        
        int slotsAvailable = Math.max(0, maxParticipants - acceptedCount);
        slotsAvailableText.setText("Slots Available: " + slotsAvailable);
    }
    
    private void loadEntrants() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                List<String> waitingListIds = (List<String>) document.get("waitingListEntrantIds");
                List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
                List<String> acceptedIds = (List<String>) document.get("acceptedEntrantIds");
                List<String> cancelledIds = (List<String>) document.get("cancelledEntrantIds");
                List<String> declinedIds = (List<String>) document.get("declinedEntrantIds");
                
                allEntrants.clear();
                filteredEntrants.clear();
                loadedEntrantIds.clear(); // Reset loaded IDs tracking
                
                // Load all entrant details - use Set to avoid duplicates
                java.util.Set<String> allIdsSet = new java.util.HashSet<>();
                if (waitingListIds != null) {
                    Log.d(TAG, "Waiting list size: " + waitingListIds.size());
                    allIdsSet.addAll(waitingListIds);
                }
                if (selectedIds != null) {
                    Log.d(TAG, "Selected list size: " + selectedIds.size());
                    allIdsSet.addAll(selectedIds);
                }
                if (acceptedIds != null) {
                    Log.d(TAG, "Accepted list size: " + acceptedIds.size());
                    allIdsSet.addAll(acceptedIds);
                }
                if (cancelledIds != null) {
                    Log.d(TAG, "Cancelled list size: " + cancelledIds.size());
                    allIdsSet.addAll(cancelledIds);
                }
                if (declinedIds != null) {
                    Log.d(TAG, "Declined list size: " + declinedIds.size());
                    allIdsSet.addAll(declinedIds);
                }
                
                Log.d(TAG, "Total unique IDs after deduplication: " + allIdsSet.size());
                List<String> allIds = new ArrayList<>(allIdsSet);
                
                final int[] loaded = {0};
                final int total = allIds.size();
                
                if (total == 0) {
                    filteredEntrants.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }
                
                for (String entrantId : allIds) {
                    // Skip if we've already loaded this entrant (prevent duplicates)
                    if (loadedEntrantIds.contains(entrantId)) {
                        loaded[0]++;
                        if (loaded[0] == total) {
                            filterEntrants();
                        }
                        continue;
                    }
                    
                    // Determine status - check cancelled first, then accepted, then selected, then others
                    String status = "pending";
                    if (cancelledIds != null && cancelledIds.contains(entrantId)) {
                        status = "cancelled"; // Cancelled users have their own filter
                    } else if (acceptedIds != null && acceptedIds.contains(entrantId)) {
                        status = "selected"; // Accepted users show as "selected" in the waiting list view
                    } else if (selectedIds != null && selectedIds.contains(entrantId)) {
                        status = "selected";
                    } else if (declinedIds != null && declinedIds.contains(entrantId)) {
                        status = "declined";
                    }
                    
                    final String finalStatus = status;
                    
                    firestore.collection("users").document(entrantId)
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            // Double-check to prevent race condition duplicates
                            if (loadedEntrantIds.contains(entrantId)) {
                                loaded[0]++;
                                if (loaded[0] == total) {
                                    filterEntrants();
                                }
                                return;
                            }
                            
                            if (userDoc.exists()) {
                                // Try both "Name" and "name" to handle field name inconsistencies
                                String name = userDoc.getString("Name");
                                if (name == null) name = userDoc.getString("name");
                                String email = userDoc.getString("email");
                                // Try to get join date from user's joined events timestamp, or use current date as fallback
                                Date joinedDate = new Date(); // Default to current date
                                // Note: In a real implementation, you might want to track when they joined the waiting list
                                
                                EntrantWithStatus entrant = new EntrantWithStatus(
                                    entrantId,
                                    name != null ? name : "Unknown",
                                    email != null ? email : "",
                                    finalStatus,
                                    joinedDate
                                );
                                
                                loadedEntrantIds.add(entrantId); // Mark as loaded
                                allEntrants.add(entrant);
                                
                                loaded[0]++;
                                if (loaded[0] == total) {
                                    filterEntrants();
                                }
                            } else {
                                // User document doesn't exist, but still add to list with placeholder info
                                Log.w(TAG, "User document not found for entrant ID: " + entrantId);
                                Date joinedDate = new Date(); // Default to current date
                                
                                EntrantWithStatus entrant = new EntrantWithStatus(
                                    entrantId,
                                    "Unknown User (ID: " + entrantId.substring(0, Math.min(8, entrantId.length())) + "...)",
                                    "",
                                    finalStatus,
                                    joinedDate
                                );
                                
                                loadedEntrantIds.add(entrantId); // Mark as loaded
                                allEntrants.add(entrant);
                                
                                loaded[0]++;
                                if (loaded[0] == total) {
                                    filterEntrants();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading entrant: " + entrantId, e);
                            loadedEntrantIds.add(entrantId); // Mark as processed even on error
                            loaded[0]++;
                            if (loaded[0] == total) {
                                filterEntrants();
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading entrants", e);
                Toast.makeText(this, "Error loading entrants", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void filterEntrants() {
        filteredEntrants.clear();
        
        // Remove any duplicates from allEntrants before filtering
        java.util.Map<String, EntrantWithStatus> uniqueEntrants = new java.util.HashMap<>();
        for (EntrantWithStatus entrant : allEntrants) {
            // Keep the first occurrence, or prefer selected/declined over pending
            if (!uniqueEntrants.containsKey(entrant.id)) {
                uniqueEntrants.put(entrant.id, entrant);
            } else {
                // If duplicate exists, prefer selected, declined, or cancelled status over pending
                EntrantWithStatus existing = uniqueEntrants.get(entrant.id);
                if (entrant.status.equals("selected") || entrant.status.equals("declined") || entrant.status.equals("cancelled")) {
                    if (existing.status.equals("pending")) {
                        uniqueEntrants.put(entrant.id, entrant);
                    }
                }
            }
        }
        
        // Update allEntrants to only contain unique entrants
        allEntrants.clear();
        allEntrants.addAll(uniqueEntrants.values());
        
        String searchQuery = searchEditText.getText().toString().toLowerCase();
        
        for (EntrantWithStatus entrant : allEntrants) {
            // Filter by status
            boolean matchesFilter = currentFilter.equals("all") || 
                                   entrant.status.equals(currentFilter);
            
            // Filter by search query
            boolean matchesSearch = searchQuery.isEmpty() ||
                                   entrant.name.toLowerCase().contains(searchQuery) ||
                                   entrant.email.toLowerCase().contains(searchQuery);
            
            if (matchesFilter && matchesSearch) {
                filteredEntrants.add(entrant);
            }
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private void runLottery() {
        Intent intent = new Intent(this, RunLotteryActivity.class);
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }
    
    private void exportCsv() {
        // Export all entrants (filtered or all based on current filter)
        List<Entrant> entrantsToExport = new ArrayList<>();
        for (EntrantWithStatus e : filteredEntrants) {
            entrantsToExport.add(new Entrant(e.id, e.name, e.email));
        }
        
        if (entrantsToExport.isEmpty()) {
            Toast.makeText(this, "No entrants to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                String eventTitle = document.getString("title");
                if (eventTitle == null) eventTitle = document.getString("Name");
                if (eventTitle == null) eventTitle = "Event";
                
                csvExportController.exportFinalListToCSV(this, eventTitle + "_waiting_list", entrantsToExport);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event for export", e);
                csvExportController.exportFinalListToCSV(this, "waiting_list", entrantsToExport);
            });
    }
    
    // Helper class to store entrant with status
    static class EntrantWithStatus {
        String id;
        String name;
        String email;
        String status; // pending, selected, declined
        Date joinedDate;
        
        EntrantWithStatus(String id, String name, String email, String status, Date joinedDate) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.status = status;
            this.joinedDate = joinedDate;
        }
    }
    
    // Adapter for waiting list
    private class WaitingListAdapter extends RecyclerView.Adapter<WaitingListAdapter.ViewHolder> {
        private List<EntrantWithStatus> entrants;
        
        WaitingListAdapter(List<EntrantWithStatus> entrants) {
            this.entrants = entrants;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waiting_list_entrant, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EntrantWithStatus entrant = entrants.get(position);
            
            holder.nameText.setText(entrant.name);
            holder.statusText.setText(entrant.status.substring(0, 1).toUpperCase() + entrant.status.substring(1));
            
            // Set status indicator color
            int statusColorRes;
            switch (entrant.status) {
                case "selected":
                    statusColorRes = R.drawable.status_indicator_green;
                    break;
                case "declined":
                    statusColorRes = R.drawable.status_indicator_red;
                    break;
                case "cancelled":
                    statusColorRes = R.drawable.status_indicator_grey;
                    break;
                default: // pending
                    statusColorRes = R.drawable.status_indicator_yellow;
                    break;
            }
            holder.statusIndicator.setBackgroundResource(statusColorRes);
            
            // Set joined date
            if (entrant.joinedDate != null) {
                holder.joinedDateText.setText("Joined " + joinDateFormat.format(entrant.joinedDate));
            } else {
                holder.joinedDateText.setText("");
            }
        }
        
        @Override
        public int getItemCount() {
            return entrants.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView statusText;
            View statusIndicator;
            TextView joinedDateText;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.entrant_name_text);
                statusText = itemView.findViewById(R.id.status_text);
                statusIndicator = itemView.findViewById(R.id.status_indicator);
                joinedDateText = itemView.findViewById(R.id.joined_date_text);
            }
        }
    }
    
    private void loadEntrantLocations() {
        if (entrantsMapView == null || eventId == null) {
            return;
        }
        
        // Clear existing markers
        entrantsMapView.getOverlays().clear();
        
        // Get all entrant IDs
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                List<String> waitingListIds = (List<String>) document.get("waitingListEntrantIds");
                List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
                List<String> cancelledIds = (List<String>) document.get("cancelledEntrantIds");
                
                List<String> allEntrantIds = new ArrayList<>();
                if (waitingListIds != null) allEntrantIds.addAll(waitingListIds);
                if (selectedIds != null) allEntrantIds.addAll(selectedIds);
                if (cancelledIds != null) allEntrantIds.addAll(cancelledIds);
                
                if (allEntrantIds.isEmpty()) {
                    return;
                }
                
                final List<GeoPoint> locations = new ArrayList<>();
                final int[] loaded = {0};
                final int total = allEntrantIds.size();
                
                // Load location for each entrant
                for (String entrantId : allEntrantIds) {
                    // Check for location in event-specific join location subcollection
                    firestore.collection("Events").document(eventId)
                        .collection("joinLocations").document(entrantId)
                        .get()
                        .addOnSuccessListener(locationDoc -> {
                            if (locationDoc.exists()) {
                                Double latitude = locationDoc.getDouble("latitude");
                                Double longitude = locationDoc.getDouble("longitude");
                                
                                if (latitude != null && longitude != null) {
                                    GeoPoint location = new GeoPoint(latitude, longitude);
                                    locations.add(location);
                                    
                                    // Get entrant name for marker
                                    firestore.collection("users").document(entrantId)
                                        .get()
                                        .addOnSuccessListener(userDoc -> {
                                            String name = userDoc.getString("Name");
                                            if (name == null) name = "Unknown";
                                            
                                            // Create marker
                                            Marker marker = new Marker(entrantsMapView);
                                            marker.setPosition(location);
                                            marker.setTitle(name);
                                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                            Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_location_pin);
                                            marker.setIcon(icon);

                                            // Set click listener to show user name
                                            marker.setOnMarkerClickListener((marker1, mapView) -> {
                                                String userName = marker1.getTitle();
                                                if (userName != null && !userName.isEmpty()) {
                                                    Toast.makeText(EntrantListActivity.this, userName, Toast.LENGTH_SHORT).show();
                                                }
                                                return true; // Return true to indicate the click was handled
                                            });
                                            
                                            entrantsMapView.getOverlays().add(marker);
                                            
                                            loaded[0]++;
                                            if (loaded[0] == total) {
                                                // All loaded, adjust camera to show all markers
                                                if (!locations.isEmpty()) {
                                                    adjustCameraToMarkers(locations);
                                                }
                                            }
                                            
                                            entrantsMapView.invalidate();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error loading entrant name: " + entrantId, e);
                                            
                                            // Create marker without name
                                            Marker marker = new Marker(entrantsMapView);
                                            marker.setPosition(location);
                                            marker.setTitle("Entrant");
                                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                            
                                            // Set click listener to show user name
                                            marker.setOnMarkerClickListener((marker1, mapView) -> {
                                                String userName = marker1.getTitle();
                                                if (userName != null && !userName.isEmpty()) {
                                                    Toast.makeText(EntrantListActivity.this, userName, Toast.LENGTH_SHORT).show();
                                                }
                                                return true; // Return true to indicate the click was handled
                                            });
                                            
                                            entrantsMapView.getOverlays().add(marker);
                                            
                                            loaded[0]++;
                                            if (loaded[0] == total) {
                                                adjustCameraToMarkers(locations);
                                            }
                                            
                                            entrantsMapView.invalidate();
                                        });
                                } else {
                                    loaded[0]++;
                                    if (loaded[0] == total) {
                                        adjustCameraToMarkers(locations);
                                    }
                                }
                            } else {
                                // Try to get location from user document (if stored there)
                                firestore.collection("users").document(entrantId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        // Check for location in user's joined events data
                                        // This would need to be implemented when users join
                                        loaded[0]++;
                                        if (loaded[0] == total) {
                                            adjustCameraToMarkers(locations);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        loaded[0]++;
                                        if (loaded[0] == total) {
                                            adjustCameraToMarkers(locations);
                                        }
                                    });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading location for entrant: " + entrantId, e);
                            loaded[0]++;
                            if (loaded[0] == total) {
                                adjustCameraToMarkers(locations);
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event for locations", e);
            });
    }
    
    private void adjustCameraToMarkers(List<GeoPoint> locations) {
        if (locations.isEmpty() || mapController == null) {
            return;
        }
        
        if (locations.size() == 1) {
            // Single marker - center on it
            mapController.setCenter(locations.get(0));
            mapController.setZoom(12.0);
        } else {
            // Multiple markers - calculate bounds
            double minLat = locations.get(0).getLatitude();
            double maxLat = locations.get(0).getLatitude();
            double minLng = locations.get(0).getLongitude();
            double maxLng = locations.get(0).getLongitude();
            
            for (GeoPoint location : locations) {
                minLat = Math.min(minLat, location.getLatitude());
                maxLat = Math.max(maxLat, location.getLatitude());
                minLng = Math.min(minLng, location.getLongitude());
                maxLng = Math.max(maxLng, location.getLongitude());
            }
            
            GeoPoint center = new GeoPoint((minLat + maxLat) / 2, (minLng + maxLng) / 2);
            double latSpan = maxLat - minLat;
            double lngSpan = maxLng - minLng;
            double maxSpan = Math.max(latSpan, lngSpan);
            
            // Calculate zoom level
            double zoom = maxSpan > 0 ? Math.log(360 / maxSpan) / Math.log(2) : 12.0;
            zoom = Math.max(zoom - 1.0, 8.0); // Minimum zoom level
            
            mapController.setCenter(center);
            mapController.setZoom(zoom);
        }
        
        entrantsMapView.invalidate();
    }
}
