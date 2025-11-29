package com.example.eventlotteryapp.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
    private LinearLayout filterAllTab;
    private LinearLayout filterPendingTab;
    private LinearLayout filterSelectedTab;
    private LinearLayout filterDeclinedTab;
    
    private String currentFilter = "all"; // all, pending, selected, declined
    private List<EntrantWithStatus> allEntrants = new ArrayList<>();
    private List<EntrantWithStatus> filteredEntrants = new ArrayList<>();
    private WaitingListAdapter adapter;
    
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
        // Reset all tabs
        resetFilterTab(filterAllTab);
        resetFilterTab(filterPendingTab);
        resetFilterTab(filterSelectedTab);
        resetFilterTab(filterDeclinedTab);
        
        // Highlight selected tab
        switch (currentFilter) {
            case "all":
                highlightFilterTab(filterAllTab);
                break;
            case "pending":
                highlightFilterTab(filterPendingTab);
                break;
            case "selected":
                highlightFilterTab(filterSelectedTab);
                break;
            case "declined":
                highlightFilterTab(filterDeclinedTab);
                break;
        }
    }
    
    private void resetFilterTab(LinearLayout tab) {
        for (int i = 0; i < tab.getChildCount(); i++) {
            View child = tab.getChildAt(i);
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(getResources().getColor(R.color.medium_grey, null));
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                textView.setTextColor(getResources().getColor(R.color.black, null));
                textView.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }
    
    private void highlightFilterTab(LinearLayout tab) {
        for (int i = 0; i < tab.getChildCount(); i++) {
            View child = tab.getChildAt(i);
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(getResources().getColor(R.color.selected_tab_color, null));
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                textView.setTextColor(getResources().getColor(R.color.selected_tab_color, null));
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
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
        // Total Entrants (all entrants: waiting + selected + declined)
        List<String> waitingList = (List<String>) document.get("waitingListEntrantIds");
        List<String> selectedList = (List<String>) document.get("selectedEntrantIds");
        List<String> cancelledList = (List<String>) document.get("cancelledEntrantIds");
        
        int waitingCount = (waitingList != null) ? waitingList.size() : 0;
        int selectedCount = (selectedList != null) ? selectedList.size() : 0;
        int cancelledCount = (cancelledList != null) ? cancelledList.size() : 0;
        int totalEntrants = waitingCount + selectedCount + cancelledCount;
        
        totalEntrantsText.setText("Total Entrants: " + totalEntrants);
        
        // Slots Available
        Long maxParticipantsLong = document.getLong("maxParticipants");
        int maxParticipants = (maxParticipantsLong != null) ? maxParticipantsLong.intValue() : 0;
        
        int slotsAvailable = Math.max(0, maxParticipants - selectedCount);
        slotsAvailableText.setText("Slots Available: " + slotsAvailable);
    }
    
    private void loadEntrants() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                List<String> waitingListIds = (List<String>) document.get("waitingListEntrantIds");
                List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
                List<String> cancelledIds = (List<String>) document.get("cancelledEntrantIds");
                
                allEntrants.clear();
                filteredEntrants.clear();
                
                // Load all entrant details
                List<String> allIds = new ArrayList<>();
                if (waitingListIds != null) allIds.addAll(waitingListIds);
                if (selectedIds != null) allIds.addAll(selectedIds);
                if (cancelledIds != null) allIds.addAll(cancelledIds);
                
                final int[] loaded = {0};
                final int total = allIds.size();
                
                if (total == 0) {
                    filteredEntrants.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }
                
                for (String entrantId : allIds) {
                    // Determine status
                    String status = "pending";
                    if (selectedIds != null && selectedIds.contains(entrantId)) {
                        status = "selected";
                    } else if (cancelledIds != null && cancelledIds.contains(entrantId)) {
                        status = "declined";
                    }
                    
                    final String finalStatus = status;
                    
                    firestore.collection("users").document(entrantId)
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                String name = userDoc.getString("name");
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
                                
                                allEntrants.add(entrant);
                                
                                loaded[0]++;
                                if (loaded[0] == total) {
                                    filterEntrants();
                                }
                            } else {
                                loaded[0]++;
                                if (loaded[0] == total) {
                                    filterEntrants();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading entrant: " + entrantId, e);
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
