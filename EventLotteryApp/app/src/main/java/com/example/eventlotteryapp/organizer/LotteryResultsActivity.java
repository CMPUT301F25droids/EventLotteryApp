package com.example.eventlotteryapp.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.eventlotteryapp.CsvExportController;
import com.example.eventlotteryapp.NotificationController;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying lottery results after a draw has been completed.
 */
public class LotteryResultsActivity extends AppCompatActivity {

    private static final String TAG = "LotteryResultsActivity";
    
    private String eventId;
    private FirebaseFirestore firestore;
    private NotificationController notificationController;
    private CsvExportController csvExportController;
    
    private TextView selectedEntrantsTitle;
    private TextView selectedEntrantsPreview;
    private TextView remainingEntrantsTitle;
    private TextView remainingEntrantsPreview;
    private CardView selectedEntrantsCard;
    private CardView remainingEntrantsCard;
    private Button notifyEntrantsButton;
    private Button downloadCsvButton;
    private Button rerunLotteryButton;
    
    private List<String> selectedEntrantIds;
    private List<String> remainingEntrantIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lottery_results);
        
        eventId = getIntent().getStringExtra("eventId");
        selectedEntrantIds = getIntent().getStringArrayListExtra("selectedEntrantIds");
        remainingEntrantIds = getIntent().getStringArrayListExtra("remainingEntrantIds");
        
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        if (selectedEntrantIds == null) {
            selectedEntrantIds = new ArrayList<>();
        }
        if (remainingEntrantIds == null) {
            remainingEntrantIds = new ArrayList<>();
        }
        
        firestore = FirebaseFirestore.getInstance();
        notificationController = new NotificationController();
        csvExportController = new CsvExportController();
        
        initializeViews();
        setupClickListeners();
        updateUI();
    }
    
    private void initializeViews() {
        selectedEntrantsTitle = findViewById(R.id.selected_entrants_title);
        selectedEntrantsPreview = findViewById(R.id.selected_entrants_preview);
        remainingEntrantsTitle = findViewById(R.id.remaining_entrants_title);
        remainingEntrantsPreview = findViewById(R.id.remaining_entrants_preview);
        selectedEntrantsCard = findViewById(R.id.selected_entrants_card);
        remainingEntrantsCard = findViewById(R.id.remaining_entrants_card);
        notifyEntrantsButton = findViewById(R.id.notify_entrants_button);
        downloadCsvButton = findViewById(R.id.download_csv_button);
        rerunLotteryButton = findViewById(R.id.rerun_lottery_button);
        
        // Back button
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
        
        // Bottom navigation
        TabLayout bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    // Dashboard
                    Intent intent = new Intent(LotteryResultsActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else if (position == 1) {
                    // Create
                    Intent intent = new Intent(LotteryResultsActivity.this, CreateEventActivity.class);
                    startActivity(intent);
                    finish();
                } else if (position == 2) {
                    // Notifications
                    Intent intent = new Intent(LotteryResultsActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.putExtra("tab", "notifications");
                    startActivity(intent);
                    finish();
                } else if (position == 3) {
                    // Profile
                    Intent intent = new Intent(LotteryResultsActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
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
    
    private void setupClickListeners() {
        selectedEntrantsCard.setOnClickListener(v -> {
            // Navigate to selected entrants list
            Intent intent = new Intent(this, EntrantListActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("filter", "selected");
            startActivity(intent);
        });
        
        remainingEntrantsCard.setOnClickListener(v -> {
            // Navigate to remaining entrants list
            Intent intent = new Intent(this, EntrantListActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("filter", "pending");
            startActivity(intent);
        });
        
        notifyEntrantsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotifyEntrantsActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
        
        downloadCsvButton.setOnClickListener(v -> exportCsv());
        
        rerunLotteryButton.setOnClickListener(v -> {
            // Navigate back to RunLotteryActivity
            Intent intent = new Intent(this, RunLotteryActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
    }
    
    private void updateUI() {
        // Update selected entrants
        int selectedCount = selectedEntrantIds.size();
        selectedEntrantsTitle.setText("Selected Entrants (" + selectedCount + ")");
        
        // Update remaining entrants
        int remainingCount = remainingEntrantIds.size();
        remainingEntrantsTitle.setText("Remaining Entrants (" + remainingCount + ")");
        
        // Load preview names
        loadPreviewNames();
    }
    
    private void loadPreviewNames() {
        // Load first few names for selected entrants
        if (!selectedEntrantIds.isEmpty()) {
            loadNamesPreview(selectedEntrantIds, selectedEntrantsPreview, 3);
        } else {
            selectedEntrantsPreview.setText("No selected entrants");
        }
        
        // Load first few names for remaining entrants
        if (!remainingEntrantIds.isEmpty()) {
            loadNamesPreview(remainingEntrantIds, remainingEntrantsPreview, 3);
        } else {
            remainingEntrantsPreview.setText("No remaining entrants");
        }
    }
    
    private void loadNamesPreview(List<String> entrantIds, TextView previewText, int maxNames) {
        int count = Math.min(maxNames, entrantIds.size());
        if (count == 0) {
            previewText.setText("No entrants");
            return;
        }
        
        List<String> namesToLoad = new ArrayList<>(entrantIds.subList(0, count));
        final int[] loaded = {0};
        final List<String> names = new ArrayList<>();
        
        for (String entrantId : namesToLoad) {
            firestore.collection("users").document(entrantId)
                .get()
                .addOnSuccessListener(document -> {
                    String name = document.getString("Name");
                    if (name != null) {
                        names.add(name);
                    }
                    loaded[0]++;
                    
                    if (loaded[0] == namesToLoad.size()) {
                        // All loaded, build preview string
                        StringBuilder preview = new StringBuilder("Includes ");
                        for (int i = 0; i < names.size(); i++) {
                            preview.append(names.get(i));
                            if (i < names.size() - 1) {
                                preview.append(", ");
                            }
                        }
                        if (entrantIds.size() > maxNames) {
                            preview.append("...");
                        }
                        previewText.setText(preview.toString());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading entrant name: " + entrantId, e);
                    loaded[0]++;
                    if (loaded[0] == namesToLoad.size()) {
                        // Build preview with whatever we have
                        if (!names.isEmpty()) {
                            StringBuilder preview = new StringBuilder("Includes ");
                            for (int i = 0; i < names.size(); i++) {
                                preview.append(names.get(i));
                                if (i < names.size() - 1) {
                                    preview.append(", ");
                                }
                            }
                            if (entrantIds.size() > maxNames) {
                                preview.append("...");
                            }
                            previewText.setText(preview.toString());
                        } else {
                            previewText.setText("Includes " + entrantIds.size() + " entrant(s)");
                        }
                    }
                });
        }
    }
    
    private void exportCsv() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                List<String> selectedIds = selectedEntrantIds;
                if (selectedIds == null || selectedIds.isEmpty()) {
                    Toast.makeText(this, "No finalized entrants to export", Toast.LENGTH_SHORT).show();
                    return;
                }

                String tempEventTitle = document.getString("title");
                if (tempEventTitle == null) tempEventTitle = document.getString("Name");
                if (tempEventTitle == null) tempEventTitle = "Event";
                final String eventTitle = tempEventTitle;

                // Load all entrant details
                List<Entrant> entrants = new ArrayList<>();
                final int[] loaded = {0};

                for (String entrantId : selectedIds) {
                    firestore.collection("users").document(entrantId)
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String name = userDoc.getString("Name");
                            String email = userDoc.getString("email");
                            if (name != null && email != null) {
                                entrants.add(new Entrant(entrantId, name, email));
                            }

                            loaded[0]++;
                            if (loaded[0] == selectedIds.size()) {
                                // All loaded, export CSV
                                csvExportController.exportFinalListToCSV(this, eventTitle, entrants);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading entrant: " + entrantId, e);
                            loaded[0]++;
                            if (loaded[0] == selectedIds.size()) {
                                csvExportController.exportFinalListToCSV(this, eventTitle, entrants);
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event for export", e);
                Toast.makeText(this, "Error loading event for export", Toast.LENGTH_SHORT).show();
            });
    }
}

