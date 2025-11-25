package com.example.eventlotteryapp.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.eventlotteryapp.NotificationController;
import com.example.eventlotteryapp.R;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Activity for running a lottery draw with configurable number of participants and auto-notify option.
 */
public class RunLotteryActivity extends AppCompatActivity {

    private static final String TAG = "RunLotteryActivity";
    
    private String eventId;
    private String replacementForEntrantId; // ID of participant being replaced
    private FirebaseFirestore firestore;
    private NotificationController notificationController;
    
    private EditText participantsCountEditText;
    private SwitchMaterial autoNotifySwitch;
    private Button runDrawButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_run_lottery);
        
        eventId = getIntent().getStringExtra("eventId");
        replacementForEntrantId = getIntent().getStringExtra("replacementForEntrantId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        firestore = FirebaseFirestore.getInstance();
        notificationController = new NotificationController();
        
        initializeViews();
        setupClickListeners();
    }
    
    private void initializeViews() {
        participantsCountEditText = findViewById(R.id.participants_count_edit_text);
        autoNotifySwitch = findViewById(R.id.auto_notify_switch);
        runDrawButton = findViewById(R.id.run_draw_button);
        
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
                    Intent intent = new Intent(RunLotteryActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else if (position == 1) {
                    // Create
                    Intent intent = new Intent(RunLotteryActivity.this, CreateEventActivity.class);
                    startActivity(intent);
                    finish();
                } else if (position == 2) {
                    // Notifications
                    Intent intent = new Intent(RunLotteryActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.putExtra("tab", "notifications");
                    startActivity(intent);
                    finish();
                } else if (position == 3) {
                    // Profile
                    Intent intent = new Intent(RunLotteryActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
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
        runDrawButton.setOnClickListener(v -> runDraw());
    }
    
    private void runDraw() {
        String countText = participantsCountEditText.getText().toString().trim();
        if (countText.isEmpty()) {
            Toast.makeText(this, "Please enter the number of participants to draw", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int participantsToDraw;
        try {
            participantsToDraw = Integer.parseInt(countText);
            if (participantsToDraw <= 0) {
                Toast.makeText(this, "Number must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean autoNotify = autoNotifySwitch.isChecked();
        
        // Confirm before running
        new AlertDialog.Builder(this)
            .setTitle("Run Lottery Draw")
            .setMessage("This will randomly select " + participantsToDraw + " participant(s) from the waiting list. Continue?")
            .setPositiveButton("Run Draw", (dialog, which) -> {
                executeLotteryDraw(participantsToDraw, autoNotify);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void executeLotteryDraw(int participantsToDraw, boolean autoNotify) {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                List<String> waitingList = (List<String>) document.get("waitingListEntrantIds");
                Long maxParticipantsLong = document.getLong("maxParticipants");
                int maxParticipants = (maxParticipantsLong != null) ? maxParticipantsLong.intValue() : 0;
                
                if (waitingList == null || waitingList.isEmpty()) {
                    Toast.makeText(this, "No entrants in waiting list", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Get current selected entrants
                List<String> selectedEntrants = (List<String>) document.get("selectedEntrantIds");
                if (selectedEntrants == null) {
                    selectedEntrants = new ArrayList<>();
                }
                
                // Check available slots
                int slotsRemaining = maxParticipants - selectedEntrants.size();
                if (slotsRemaining <= 0) {
                    Toast.makeText(this, "All slots are already filled", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Limit participantsToDraw to available slots and waiting list size
                int actualDrawCount = Math.min(participantsToDraw, Math.min(slotsRemaining, waitingList.size()));
                
                if (actualDrawCount < participantsToDraw) {
                    Toast.makeText(this, "Only " + actualDrawCount + " participant(s) can be selected (limited by available slots or waiting list size)", Toast.LENGTH_LONG).show();
                }
                
                // Randomly select from waiting list
                List<String> shuffled = new ArrayList<>(waitingList);
                Collections.shuffle(shuffled, new Random());
                
                List<String> newlySelected = shuffled.subList(0, actualDrawCount);
                
                // Update Firestore - add newly selected first
                selectedEntrants.addAll(newlySelected);
                waitingList.removeAll(newlySelected);
                
                // If this is a replacement draw, remove the original participant AFTER drawing
                if (replacementForEntrantId != null && !replacementForEntrantId.isEmpty()) {
                    // Remove from selected, cancelled, or declined lists
                    selectedEntrants.remove(replacementForEntrantId);
                    List<String> cancelledIds = (List<String>) document.get("cancelledEntrantIds");
                    List<String> declinedIds = (List<String>) document.get("declinedEntrantIds");
                    
                    if (cancelledIds == null) cancelledIds = new ArrayList<>();
                    if (declinedIds == null) declinedIds = new ArrayList<>();
                    
                    cancelledIds.remove(replacementForEntrantId);
                    declinedIds.remove(replacementForEntrantId);
                    
                    // Add back to waiting list if not already there
                    if (!waitingList.contains(replacementForEntrantId)) {
                        waitingList.add(replacementForEntrantId);
                    }
                    
                    // Update with cancelled and declined lists too
                    firestore.collection("Events").document(eventId)
                        .update("selectedEntrantIds", selectedEntrants, 
                                "waitingListEntrantIds", waitingList,
                                "cancelledEntrantIds", cancelledIds,
                                "declinedEntrantIds", declinedIds)
                        .addOnSuccessListener(aVoid -> {
                            String message = "Replacement drawn. " + actualDrawCount + " participant(s) selected.";
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            
                            // Auto-notify if enabled
                            if (autoNotify) {
                                String eventTitle = document.getString("title");
                                if (eventTitle == null) eventTitle = document.getString("Name");
                                
                                // Notify selected entrants
                                notificationController.sendToSelectedEntrants(eventId, 
                                    "Lottery Selection", 
                                    "Congratulations! You've been selected for " + eventTitle);
                                
                                // Notify non-selected entrants (those still in waiting list)
                                notificationController.sendToWaitingList(eventId,
                                    "Lottery Results",
                                    "Thank you for your interest in " + eventTitle + ". The lottery draw has been completed. You were not selected this time, but you remain on the waiting list.");
                            }
                            
                            // Navigate to Lottery Results screen
                            Intent resultsIntent = new Intent(RunLotteryActivity.this, LotteryResultsActivity.class);
                            resultsIntent.putExtra("eventId", eventId);
                            resultsIntent.putStringArrayListExtra("selectedEntrantIds", new ArrayList<>(newlySelected));
                            resultsIntent.putStringArrayListExtra("remainingEntrantIds", new ArrayList<>(waitingList));
                            startActivity(resultsIntent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating event with replacement", e);
                            Toast.makeText(this, "Error updating event", Toast.LENGTH_SHORT).show();
                        });
                } else {
                    // Normal lottery draw (not a replacement)
                    firestore.collection("Events").document(eventId)
                        .update("selectedEntrantIds", selectedEntrants, "waitingListEntrantIds", waitingList)
                        .addOnSuccessListener(aVoid -> {
                            String message = "Lottery draw completed. " + actualDrawCount + " participant(s) selected.";
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            
                            // Auto-notify if enabled
                            if (autoNotify) {
                                String eventTitle = document.getString("title");
                                if (eventTitle == null) eventTitle = document.getString("Name");
                                
                                // Notify selected entrants
                                notificationController.sendToSelectedEntrants(eventId, 
                                    "Lottery Selection", 
                                    "Congratulations! You've been selected for " + eventTitle);
                                
                                // Notify non-selected entrants (those still in waiting list)
                                notificationController.sendToWaitingList(eventId,
                                    "Lottery Results",
                                    "Thank you for your interest in " + eventTitle + ". The lottery draw has been completed. You were not selected this time, but you remain on the waiting list.");
                            }
                            
                            // Navigate to Lottery Results screen
                            Intent resultsIntent = new Intent(RunLotteryActivity.this, LotteryResultsActivity.class);
                            resultsIntent.putExtra("eventId", eventId);
                            resultsIntent.putStringArrayListExtra("selectedEntrantIds", new ArrayList<>(newlySelected));
                            resultsIntent.putStringArrayListExtra("remainingEntrantIds", new ArrayList<>(waitingList));
                            startActivity(resultsIntent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error running lottery draw", e);
                            Toast.makeText(this, "Error running lottery draw: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event for lottery", e);
                Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
            });
    }
}

