package com.example.eventlotteryapp.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eventlotteryapp.CsvExportController;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for displaying and managing the finalized list of selected entrants.
 */
public class FinalizedListActivity extends AppCompatActivity {

    private static final String TAG = "FinalizedListActivity";
    
    private String eventId;
    private FirebaseFirestore firestore;
    private CsvExportController csvExportController;
    
    private TextView eventTitle;
    private TextView totalAttendeesText;
    private TextView cancelledCountText;
    private TextView declinedCountText;
    private Button filterAcceptedButton;
    private Button filterDeclinedButton;
    private Button filterCancelledButton;
    private RecyclerView participantsRecyclerView;
    private Button drawReplacementButton;
    private Button exportCsvButton;
    
    private List<FinalizedParticipant> allParticipants = new ArrayList<>();
    private List<FinalizedParticipant> filteredParticipants = new ArrayList<>();
    private FinalizedParticipantAdapter adapter;
    private String currentFilter = "accepted";
    private FinalizedParticipant selectedParticipant = null;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_finalized_list);
        
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        firestore = FirebaseFirestore.getInstance();
        csvExportController = new CsvExportController();
        
        initializeViews();
        setupClickListeners();
        updateFilterButtons(); // Initialize filter button states
        
        // Initialize draw replacement button as disabled (grey)
        drawReplacementButton.setBackground(getResources().getDrawable(R.drawable.disabled_button_bg));
        drawReplacementButton.setBackgroundTintList(null);
        drawReplacementButton.setTextColor(getResources().getColor(R.color.white, null));
        
        loadEventData();
    }
    
    private void initializeViews() {
        eventTitle = findViewById(R.id.event_title);
        totalAttendeesText = findViewById(R.id.total_attendees_text);
        cancelledCountText = findViewById(R.id.cancelled_count_text);
        declinedCountText = findViewById(R.id.declined_count_text);
        filterAcceptedButton = findViewById(R.id.filter_accepted_button);
        filterDeclinedButton = findViewById(R.id.filter_declined_button);
        filterCancelledButton = findViewById(R.id.filter_cancelled_button);
        participantsRecyclerView = findViewById(R.id.participants_recycler_view);
        drawReplacementButton = findViewById(R.id.draw_replacement_button);
        exportCsvButton = findViewById(R.id.export_csv_button);
        
        // Back button
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
        
        // Setup RecyclerView
        adapter = new FinalizedParticipantAdapter(filteredParticipants);
        participantsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantsRecyclerView.setAdapter(adapter);
        
        // Bottom navigation
        TabLayout bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    Intent intent = new Intent(FinalizedListActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else if (position == 1) {
                    Intent intent = new Intent(FinalizedListActivity.this, CreateEventActivity.class);
                    startActivity(intent);
                    finish();
                } else if (position == 2) {
                    Intent intent = new Intent(FinalizedListActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.putExtra("tab", "notifications");
                    startActivity(intent);
                    finish();
                } else if (position == 3) {
                    Intent intent = new Intent(FinalizedListActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
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
        filterAcceptedButton.setOnClickListener(v -> setFilter("accepted"));
        filterDeclinedButton.setOnClickListener(v -> setFilter("declined"));
        filterCancelledButton.setOnClickListener(v -> setFilter("cancelled"));
        
        drawReplacementButton.setOnClickListener(v -> {
            if (!drawReplacementButton.isEnabled()) {
                Toast.makeText(this, "Please select a participant first", Toast.LENGTH_SHORT).show();
                return;
            }
            drawReplacement();
        });
        exportCsvButton.setOnClickListener(v -> exportCsv());
    }
    
    private void setFilter(String filter) {
        currentFilter = filter;
        // Clear selection when filter changes
        selectedParticipant = null;
        drawReplacementButton.setEnabled(false);
        drawReplacementButton.setBackground(getResources().getDrawable(R.drawable.disabled_button_bg));
        drawReplacementButton.setBackgroundTintList(null);
        drawReplacementButton.setTextColor(getResources().getColor(R.color.white, null));
        updateFilterButtons();
        filterParticipants();
    }
    
    private void updateFilterButtons() {
        // Reset all buttons to default state (grey)
        filterAcceptedButton.setBackground(getResources().getDrawable(R.drawable.export_csv_button_bg));
        filterAcceptedButton.setBackgroundTintList(null);
        filterAcceptedButton.setTextColor(getResources().getColor(R.color.black, null));
        filterAcceptedButton.setCompoundDrawableTintList(getResources().getColorStateList(R.color.black_color_state_list));
        
        filterDeclinedButton.setBackground(getResources().getDrawable(R.drawable.export_csv_button_bg));
        filterDeclinedButton.setBackgroundTintList(null);
        filterDeclinedButton.setTextColor(getResources().getColor(R.color.black, null));
        filterDeclinedButton.setCompoundDrawableTintList(getResources().getColorStateList(R.color.black_color_state_list));
        
        filterCancelledButton.setBackground(getResources().getDrawable(R.drawable.export_csv_button_bg));
        filterCancelledButton.setBackgroundTintList(null);
        filterCancelledButton.setTextColor(getResources().getColor(R.color.black, null));
        filterCancelledButton.setCompoundDrawableTintList(getResources().getColorStateList(R.color.black_color_state_list));
        
        // Highlight selected filter (purple)
        switch (currentFilter) {
            case "accepted":
                filterAcceptedButton.setBackground(getResources().getDrawable(R.drawable.notify_button_bg));
                filterAcceptedButton.setBackgroundTintList(null);
                filterAcceptedButton.setTextColor(getResources().getColor(R.color.white, null));
                filterAcceptedButton.setCompoundDrawableTintList(getResources().getColorStateList(R.color.white_color_state_list));
                break;
            case "declined":
                filterDeclinedButton.setBackground(getResources().getDrawable(R.drawable.notify_button_bg));
                filterDeclinedButton.setBackgroundTintList(null);
                filterDeclinedButton.setTextColor(getResources().getColor(R.color.white, null));
                filterDeclinedButton.setCompoundDrawableTintList(getResources().getColorStateList(R.color.white_color_state_list));
                break;
            case "cancelled":
                filterCancelledButton.setBackground(getResources().getDrawable(R.drawable.notify_button_bg));
                filterCancelledButton.setBackgroundTintList(null);
                filterCancelledButton.setTextColor(getResources().getColor(R.color.white, null));
                filterCancelledButton.setCompoundDrawableTintList(getResources().getColorStateList(R.color.white_color_state_list));
                break;
        }
    }
    
    private void loadEventData() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                // Load event title
                String title = document.getString("title");
                if (title == null) title = document.getString("Name");
                if (title != null) {
                    eventTitle.setText(title);
                }
                
                // Load participants
                loadParticipants(document);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event data", e);
                Toast.makeText(this, "Error loading event data", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void loadParticipants(DocumentSnapshot document) {
        List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
        List<String> cancelledIds = (List<String>) document.get("cancelledEntrantIds");
        List<String> declinedIds = (List<String>) document.get("declinedEntrantIds");
        
        if (selectedIds == null) selectedIds = new ArrayList<>();
        if (cancelledIds == null) cancelledIds = new ArrayList<>();
        if (declinedIds == null) declinedIds = new ArrayList<>();
        
        allParticipants.clear();
        
        // Count totals
        int totalSelected = selectedIds.size();
        int totalCancelled = cancelledIds.size();
        int totalDeclined = declinedIds.size();
        Long maxParticipants = document.getLong("maxParticipants");
        int max = (maxParticipants != null) ? maxParticipants.intValue() : 0;
        
        totalAttendeesText.setText("Total Attendees: " + totalSelected + "/" + max);
        cancelledCountText.setText("Cancelled: " + totalCancelled);
        declinedCountText.setText("Declined: " + totalDeclined);
        
        // Load all participants
        final int[] loaded = {0};
        final int total = selectedIds.size() + cancelledIds.size() + declinedIds.size();
        
        if (total == 0) {
            adapter.notifyDataSetChanged();
            return;
        }
        
        // Load selected (accepted) participants
        for (String entrantId : selectedIds) {
            loadParticipant(entrantId, "accepted", loaded, total);
        }
        
        // Load cancelled participants
        for (String entrantId : cancelledIds) {
            loadParticipant(entrantId, "cancelled", loaded, total);
        }
        
        // Load declined participants
        for (String entrantId : declinedIds) {
            loadParticipant(entrantId, "declined", loaded, total);
        }
    }
    
    private void loadParticipant(String entrantId, String status, int[] loaded, int total) {
        firestore.collection("Users").document(entrantId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String name = document.getString("Name");
                    Date createdAt = document.getDate("createdAt");
                    Date confirmedDate = new Date(); // Use current date as confirmed date for now
                    
                    FinalizedParticipant participant = new FinalizedParticipant(
                        entrantId,
                        name != null ? name : "Unknown",
                        status,
                        createdAt != null ? createdAt : new Date(),
                        confirmedDate
                    );
                    
                    allParticipants.add(participant);
                }
                
                loaded[0]++;
                if (loaded[0] == total) {
                    filterParticipants();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading participant: " + entrantId, e);
                loaded[0]++;
                if (loaded[0] == total) {
                    filterParticipants();
                }
            });
    }
    
    private void filterParticipants() {
        filteredParticipants.clear();
        
        for (FinalizedParticipant participant : allParticipants) {
            if (participant.status.equals(currentFilter)) {
                filteredParticipants.add(participant);
            }
        }
        
        adapter.notifyDataSetChanged();
    }
    
    private void drawReplacement() {
        if (selectedParticipant == null) {
            Toast.makeText(this, "Please select a participant", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Navigate to Run Lottery page with the selected participant ID
        // The removal will happen after a replacement is drawn
        Intent intent = new Intent(this, RunLotteryActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("replacementForEntrantId", selectedParticipant.entrantId);
        startActivity(intent);
        finish();
    }
    
    private void exportCsv() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
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
                    firestore.collection("Users").document(entrantId)
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String name = userDoc.getString("Name");
                            String email = userDoc.getString("email");
                            if (name != null && email != null) {
                                entrants.add(new Entrant(entrantId, name, email));
                            }

                            loaded[0]++;
                            if (loaded[0] == selectedIds.size()) {
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
    
    private void onParticipantSelected(FinalizedParticipant participant, boolean isSelected) {
        if (isSelected) {
            // Deselect previous selection
            if (selectedParticipant != null && selectedParticipant != participant) {
                int oldIndex = filteredParticipants.indexOf(selectedParticipant);
                if (oldIndex >= 0) {
                    adapter.notifyItemChanged(oldIndex);
                }
            }
            selectedParticipant = participant;
            drawReplacementButton.setEnabled(true);
            // Update button appearance to purple
            drawReplacementButton.setBackground(getResources().getDrawable(R.drawable.notify_button_bg));
            drawReplacementButton.setBackgroundTintList(null);
            drawReplacementButton.setTextColor(getResources().getColor(R.color.white, null));
        } else {
            if (selectedParticipant == participant) {
                selectedParticipant = null;
                drawReplacementButton.setEnabled(false);
                // Update button appearance to grey
                drawReplacementButton.setBackground(getResources().getDrawable(R.drawable.disabled_button_bg));
                drawReplacementButton.setBackgroundTintList(null);
                drawReplacementButton.setTextColor(getResources().getColor(R.color.white, null));
            }
        }
    }
    
    // Helper class for participants
    private static class FinalizedParticipant {
        String entrantId;
        String name;
        String status;
        Date joinedDate;
        Date confirmedDate;
        
        FinalizedParticipant(String entrantId, String name, String status, Date joinedDate, Date confirmedDate) {
            this.entrantId = entrantId;
            this.name = name;
            this.status = status;
            this.joinedDate = joinedDate;
            this.confirmedDate = confirmedDate;
        }
    }
    
    // Adapter for participants
    private class FinalizedParticipantAdapter extends RecyclerView.Adapter<FinalizedParticipantAdapter.ViewHolder> {
        private List<FinalizedParticipant> participants;
        
        FinalizedParticipantAdapter(List<FinalizedParticipant> participants) {
            this.participants = participants;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_finalized_participant, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FinalizedParticipant participant = participants.get(position);
            holder.nameText.setText(participant.name);
            
            String datesText = "Confirmed " + dateFormat.format(participant.confirmedDate) +
                             " · Joined " + dateFormat.format(participant.joinedDate);
            holder.datesText.setText(datesText);
            
            holder.checkbox.setChecked(selectedParticipant == participant);
            
            holder.itemView.setOnClickListener(v -> {
                boolean newState = !holder.checkbox.isChecked();
                holder.checkbox.setChecked(newState);
                onParticipantSelected(participant, newState);
            });
            
            holder.checkbox.setOnClickListener(v -> {
                onParticipantSelected(participant, holder.checkbox.isChecked());
            });
        }
        
        @Override
        public int getItemCount() {
            return participants.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkbox;
            TextView nameText;
            TextView datesText;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkbox = itemView.findViewById(R.id.participant_checkbox);
                nameText = itemView.findViewById(R.id.participant_name);
                datesText = itemView.findViewById(R.id.participant_dates);
            }
        }
    }
}

