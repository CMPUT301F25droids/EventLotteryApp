package com.example.eventlotteryapp.organizer;

import android.app.AlertDialog;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Activity for displaying and managing the finalized list of selected entrants.
 * Implements US 02.06.04: Cancel entrants who didn't sign up
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
    private Button cancelButton;
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
        updateFilterButtons();
        updateButtonVisibility();
        
        // Ensure cancel button is red
        cancelButton.setBackground(getResources().getDrawable(R.drawable.delete_button_bg));
        cancelButton.setBackgroundTintList(null);

        loadEventData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        if (eventId != null) {
            loadEventData();
        }
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
        cancelButton = findViewById(R.id.cancel_button);
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

        cancelButton.setOnClickListener(v -> {
            if (selectedParticipant != null) {
                cancelParticipant(selectedParticipant);
            }
        });

        drawReplacementButton.setOnClickListener(v -> {
            if (selectedParticipant == null) {
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
        updateButtonVisibility();
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
        // Get acceptedEntrantIds (users who accepted invitation) - this is the finalized list
        List<String> acceptedIds = (List<String>) document.get("acceptedEntrantIds");
        // Also check selectedEntrantIds for users who were selected but haven't responded yet
        List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
        List<String> cancelledIds = (List<String>) document.get("cancelledEntrantIds");
        List<String> declinedIds = (List<String>) document.get("declinedEntrantIds");

        if (acceptedIds == null) acceptedIds = new ArrayList<>();
        if (selectedIds == null) selectedIds = new ArrayList<>();
        if (cancelledIds == null) cancelledIds = new ArrayList<>();
        if (declinedIds == null) declinedIds = new ArrayList<>();

        // Remove duplicates from each list while preserving order
        Set<String> acceptedSet = new LinkedHashSet<>(acceptedIds);
        Set<String> selectedSet = new LinkedHashSet<>(selectedIds);
        Set<String> cancelledSet = new LinkedHashSet<>(cancelledIds);
        Set<String> declinedSet = new LinkedHashSet<>(declinedIds);
        
        acceptedIds = new ArrayList<>(acceptedSet);
        selectedIds = new ArrayList<>(selectedSet);
        cancelledIds = new ArrayList<>(cancelledSet);
        declinedIds = new ArrayList<>(declinedSet);

        allParticipants.clear();

        // Count totals (using deduplicated lists)
        // Accepted users are the finalized attendees
        int totalAccepted = acceptedIds.size();
        int totalCancelled = cancelledIds.size();
        int totalDeclined = declinedIds.size();
        Long maxParticipants = document.getLong("maxParticipants");
        int max = (maxParticipants != null) ? maxParticipants.intValue() : 0;

        totalAttendeesText.setText("Total Attendees: " + totalAccepted + "/" + max);
        cancelledCountText.setText("Cancelled: " + totalCancelled);
        declinedCountText.setText("Declined: " + totalDeclined);

        // Load all participants
        final int[] loaded = {0};
        final int total = acceptedIds.size() + selectedIds.size() + cancelledIds.size() + declinedIds.size();

        if (total == 0) {
            adapter.notifyDataSetChanged();
            return;
        }

        // Track loaded entrant IDs to prevent duplicates across different statuses
        Set<String> loadedEntrantIds = new LinkedHashSet<>();

        // Load accepted participants (users who accepted invitation) - these are the finalized attendees
        for (String entrantId : acceptedIds) {
            if (!loadedEntrantIds.contains(entrantId)) {
                loadedEntrantIds.add(entrantId);
                loadParticipant(entrantId, "accepted", loaded, total);
            }
        }

        // Load selected participants (users who were selected but haven't responded yet)
        // These should also show as "accepted" in the UI since they're pending acceptance
        for (String entrantId : selectedIds) {
            if (!loadedEntrantIds.contains(entrantId)) {
                loadedEntrantIds.add(entrantId);
                loadParticipant(entrantId, "accepted", loaded, total);
            }
        }

        // Load cancelled participants (only if not already loaded)
        for (String entrantId : cancelledIds) {
            if (!loadedEntrantIds.contains(entrantId)) {
                loadedEntrantIds.add(entrantId);
                loadParticipant(entrantId, "cancelled", loaded, total);
            }
        }

        // Load declined participants (only if not already loaded)
        for (String entrantId : declinedIds) {
            if (!loadedEntrantIds.contains(entrantId)) {
                loadedEntrantIds.add(entrantId);
                loadParticipant(entrantId, "declined", loaded, total);
            }
        }
    }

    private void loadParticipant(String entrantId, String status, int[] loaded, int total) {
        firestore.collection("users").document(entrantId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Check if this participant already exists to prevent duplicates
                        boolean alreadyExists = false;
                        for (FinalizedParticipant existing : allParticipants) {
                            if (existing.entrantId.equals(entrantId)) {
                                alreadyExists = true;
                                break;
                            }
                        }
                        
                        if (!alreadyExists) {
                            String name = document.getString("Name");
                            if (name == null) name = document.getString("name");
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

    /**
     * US 02.06.04: Cancel a selected entrant who didn't sign up
     * Moves them from selectedEntrantIds to cancelledEntrantIds
     */
    private void cancelParticipant(FinalizedParticipant participant) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Participant")
                .setMessage("Are you sure you want to cancel " + participant.name + "? They will be moved to the cancelled list.")
                .setPositiveButton("Cancel Participant", (dialog, which) -> {
                    // Move from selected to cancelled in Firestore
                    firestore.collection("Events").document(eventId)
                            .get()
                            .addOnSuccessListener(document -> {
                                List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
                                List<String> cancelledIds = (List<String>) document.get("cancelledEntrantIds");

                                if (selectedIds == null) selectedIds = new ArrayList<>();
                                if (cancelledIds == null) cancelledIds = new ArrayList<>();

                                // Remove from selected
                                selectedIds.remove(participant.entrantId);

                                // Add to cancelled
                                if (!cancelledIds.contains(participant.entrantId)) {
                                    cancelledIds.add(participant.entrantId);
                                }

                                // Update Firestore
                                firestore.collection("Events").document(eventId)
                                        .update("selectedEntrantIds", selectedIds,
                                                "cancelledEntrantIds", cancelledIds)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, participant.name + " has been cancelled", Toast.LENGTH_SHORT).show();
                                            // Clear selection and update button visibility
                                            selectedParticipant = null;
                                            updateButtonVisibility();
                                            // Reload data
                                            loadEventData();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error cancelling participant", e);
                                            Toast.makeText(this, "Error cancelling participant: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error loading event", e);
                                Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Keep Participant", null)
                .show();
    }

    private void exportCsv() {
        firestore.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    // Export accepted entrants (finalized list) and selected entrants (pending acceptance)
                    List<String> acceptedIds = (List<String>) document.get("acceptedEntrantIds");
                    List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
                    
                    if (acceptedIds == null) acceptedIds = new ArrayList<>();
                    if (selectedIds == null) selectedIds = new ArrayList<>();
                    
                    // Combine accepted and selected (remove duplicates)
                    Set<String> allFinalizedIds = new LinkedHashSet<>();
                    allFinalizedIds.addAll(acceptedIds);
                    allFinalizedIds.addAll(selectedIds);
                    List<String> finalizedIds = new ArrayList<>(allFinalizedIds);
                    
                    if (finalizedIds.isEmpty()) {
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

                    for (String entrantId : finalizedIds) {
                        firestore.collection("users").document(entrantId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("Name");
                                    if (name == null) name = userDoc.getString("name");
                                    String email = userDoc.getString("email");
                                    if (name != null && email != null) {
                                        entrants.add(new Entrant(entrantId, name, email));
                                    }

                                    loaded[0]++;
                                    if (loaded[0] == finalizedIds.size()) {
                                        csvExportController.exportFinalListToCSV(this, eventTitle, entrants);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading entrant: " + entrantId, e);
                                    loaded[0]++;
                                    if (loaded[0] == finalizedIds.size()) {
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
            // Notify adapter to update checkbox state
            int newIndex = filteredParticipants.indexOf(participant);
            if (newIndex >= 0) {
                adapter.notifyItemChanged(newIndex);
            }
            updateButtonVisibility();
        } else {
            if (selectedParticipant == participant) {
                selectedParticipant = null;
                // Notify adapter to update checkbox state
                int oldIndex = filteredParticipants.indexOf(participant);
                if (oldIndex >= 0) {
                    adapter.notifyItemChanged(oldIndex);
                }
                updateButtonVisibility();
            }
        }
    }

    private void updateButtonVisibility() {
        if (selectedParticipant != null && currentFilter.equals("accepted")) {
            // Show both buttons when a participant is selected in accepted filter
            cancelButton.setVisibility(View.VISIBLE);
            drawReplacementButton.setVisibility(View.VISIBLE);
        } else {
            // Hide both buttons when no participant is selected or not in accepted filter
            cancelButton.setVisibility(View.GONE);
            drawReplacementButton.setVisibility(View.GONE);
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