package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.Controllers.LotteryController;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.EntrantView.JoinConfirmationFragment;
import com.example.eventlotteryapp.EntrantView.LeaveConfirmationFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.eventlotteryapp.NotificationController;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.example.eventlotteryapp.Controllers.LotteryController;
import android.widget.Toast;

public class EventDetailsActivity extends AppCompatActivity {
    private String eventId;
    private FirebaseFirestore db;

    private TextView tvLotteryInfo;
    private TextView tvWaitlistCount;
    private TextView tvStatusMessage;

    private NotificationController notificationController;

    private Button acceptInvitationButton;
    private Button declineInvitationButton;
    private LotteryController lotteryController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);

        ImageView back_button = findViewById(R.id.back_button);
        back_button.setOnClickListener(v -> {
            // Handle back button click
            finish();
        });
        
        notificationController = new NotificationController();


        // Join button click
        Button join_button = findViewById(R.id.join_waitlist_button);
        join_button.setOnClickListener(v -> {
            // Check if event is closed before showing join dialog
            db.collection("Events").document(eventId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Date registrationCloseDate = documentSnapshot.getDate("registrationCloseDate");
                            Date now = new Date();
                            boolean isEventClosed = (registrationCloseDate != null && now.after(registrationCloseDate));
                            
                            if (isEventClosed) {
                                Toast.makeText(this, "Registration for this event is closed.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            JoinConfirmationFragment confirmation = new JoinConfirmationFragment().newInstance(eventId);
                            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
                            // Send notification to organizer about the new entrant
                            notificationController.sendToSelectedEntrants(eventId,
                                    "New Entrant",
                                    "A user has joined the waiting list for your event.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventDetails", "Error checking event status", e);
                        // Still show dialog if check fails (fail open)
                        JoinConfirmationFragment confirmation = new JoinConfirmationFragment().newInstance(eventId);
                        confirmation.show(getSupportFragmentManager(), confirmation.getTag());
                    });
        });

        Button leave_button = findViewById(R.id.leave_waitlist_button);
        leave_button.setOnClickListener(v -> {
            // Check if lottery has run and user was selected before allowing leave
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                return;
            }
            String userId = auth.getCurrentUser().getUid();
            
            db.collection("Events").document(eventId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Date registrationCloseDate = documentSnapshot.getDate("registrationCloseDate");
                            Date now = new Date();
                            boolean isEventClosed = (registrationCloseDate != null && now.after(registrationCloseDate));
                            
                            List<String> selectedEntrantIds = (List<String>) documentSnapshot.get("selectedEntrantIds");
                            boolean isSelected = (selectedEntrantIds != null && selectedEntrantIds.contains(userId));
                            
                            // Block leave if lottery has run and user was selected
                            if (isEventClosed && isSelected) {
                                Toast.makeText(this, "Cannot leave waitlist. The lottery has run and you were selected.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Handle leave button click
                            LeaveConfirmationFragment confirmation = new LeaveConfirmationFragment().newInstance(eventId);
                            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
                            userInWaitlist();
                            // Notify organizer about the user leaving
                            notificationController.sendToCancelledEntrants(eventId,
                                    "Entrant Left",
                                    "A user has left the waiting list for your event.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventDetails", "Error checking event status", e);
                        // Still show dialog if check fails (fail open)
                        LeaveConfirmationFragment confirmation = new LeaveConfirmationFragment().newInstance(eventId);
                        confirmation.show(getSupportFragmentManager(), confirmation.getTag());
                    });
        });

        acceptInvitationButton = findViewById(R.id.accept_invitation_button);
        declineInvitationButton = findViewById(R.id.decline_invitation_button);
        lotteryController = new LotteryController();

        acceptInvitationButton.setOnClickListener(v -> {
            acceptInvitation();
        });

        declineInvitationButton.setOnClickListener(v -> {
            declineInvitation();
        });

        eventId = getIntent().getStringExtra("eventId");
        Log.d("EventDetails", "Event ID: " + eventId);
        db = FirebaseFirestore.getInstance();

        tvLotteryInfo = findViewById(R.id.tv_lottery_info);
        tvWaitlistCount = findViewById(R.id.waitlist_count);
        tvStatusMessage = findViewById(R.id.status_message);
        userInWaitlist();
        populateUI();
    }

    /** Checks if current user is organizer and updates UI accordingly */
    private void showOrganizerControlsIfOwner(DocumentReference organizerRef) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        DocumentReference currentUserRef = db.collection("users").document(auth.getCurrentUser().getUid());
        organizerRef.get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getReference().equals(currentUserRef)) {
                // TODO: Add organizer control buttons if needed
                // notifyWaitlistButton.setVisibility(View.VISIBLE);
                // notifySelectedButton.setVisibility(View.VISIBLE);
                // notifyCancelledButton.setVisibility(View.VISIBLE);
            } else {
                // notifyWaitlistButton.setVisibility(View.GONE);
                // notifySelectedButton.setVisibility(View.GONE);
                // notifyCancelledButton.setVisibility(View.GONE);
            }
        });
    }

    /** Sends notifications to all users in a specific group field (Waitlist / Selected / Cancelled) */
    private void sendNotificationsToGroup(String fieldName, String title, String message) {
        db.collection("Events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) return;
                    // TODO: Implement notification sending logic
                })
                .addOnFailureListener(e -> Log.e("EventDetails", "Error sending notifications", e));
    }

    protected void userInWaitlist() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        Log.d("Firestore", "Checking waitlist for eventId=" + eventId + ", userId=" + userId);

        db.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check all status arrays
                        List<String> waitingListEntrantIds = (List<String>) documentSnapshot.get("waitingListEntrantIds");
                        List<String> selectedEntrantIds = (List<String>) documentSnapshot.get("selectedEntrantIds");
                        List<String> acceptedEntrantIds = (List<String>) documentSnapshot.get("acceptedEntrantIds");
                        List<String> cancelledEntrantIds = (List<String>) documentSnapshot.get("cancelledEntrantIds");
                        List<String> declinedEntrantIds = (List<String>) documentSnapshot.get("declinedEntrantIds");

                        // Check if event is closed (registrationCloseDate has passed)
                        Date registrationCloseDate = documentSnapshot.getDate("registrationCloseDate");
                        Date now = new Date();
                        boolean isEventClosed = (registrationCloseDate != null && now.after(registrationCloseDate));

                        Button join_button = findViewById(R.id.join_waitlist_button);
                        Button leave_button = findViewById(R.id.leave_waitlist_button);

                        // Check user status
                        boolean isInWaitingList = (waitingListEntrantIds != null && waitingListEntrantIds.contains(userId));
                        boolean isSelected = (selectedEntrantIds != null && selectedEntrantIds.contains(userId));
                        boolean isAccepted = (acceptedEntrantIds != null && acceptedEntrantIds.contains(userId));
                        boolean isCancelled = (cancelledEntrantIds != null && cancelledEntrantIds.contains(userId));
                        boolean isDeclined = (declinedEntrantIds != null && declinedEntrantIds.contains(userId));

                        // Check if lottery has run and user was selected (cannot leave waitlist)
                        boolean lotteryRanAndUserSelected = isEventClosed && isSelected;
                        
                        // Check if lottery has run and user was NOT selected (rejected)
                        boolean lotteryRanAndUserNotSelected = isEventClosed && isInWaitingList && !isSelected && !isAccepted && !isCancelled && !isDeclined;

                        Log.d("Firestore", "Status check - waiting: " + isInWaitingList +
                                ", selected: " + isSelected +
                                ", accepted: " + isAccepted +
                                ", cancelled: " + isCancelled +
                                ", declined: " + isDeclined +
                                ", eventClosed: " + isEventClosed +
                                ", lotteryRanAndUserSelected: " + lotteryRanAndUserSelected +
                                ", lotteryRanAndUserNotSelected: " + lotteryRanAndUserNotSelected);
                        
                        // Update status message to show rejection if applicable
                        updateStatusMessage(isEventClosed, isSelected, isAccepted, isInWaitingList, isCancelled, isDeclined);

                        // Show appropriate buttons based on status
                        if (isSelected) {
                            // User is SELECTED - show Accept/Decline buttons
                            join_button.setVisibility(Button.GONE);
                            leave_button.setVisibility(Button.GONE);
                            acceptInvitationButton.setVisibility(Button.VISIBLE);
                            declineInvitationButton.setVisibility(Button.VISIBLE);
                        } else if (isAccepted) {
                            // User has ACCEPTED - show confirmation message
                            join_button.setVisibility(Button.GONE);
                            leave_button.setVisibility(Button.GONE);
                            acceptInvitationButton.setVisibility(Button.GONE);
                            declineInvitationButton.setVisibility(Button.GONE);
                            // Optionally show a "You're registered!" message
                        } else if (isDeclined) {
                            // User has DECLINED - show Leave button to rejoin waiting list (only if event not closed)
                            join_button.setVisibility(Button.GONE);
                            if (isEventClosed) {
                                leave_button.setVisibility(Button.GONE);
                            } else {
                                leave_button.setVisibility(Button.VISIBLE);
                            }
                            acceptInvitationButton.setVisibility(Button.GONE);
                            declineInvitationButton.setVisibility(Button.GONE);
                        } else if (isInWaitingList || isCancelled) {
                            // User is in waiting list - show Leave button (only if lottery hasn't run or user wasn't selected)
                            join_button.setVisibility(Button.GONE);
                            if (lotteryRanAndUserSelected) {
                                // Cannot leave if lottery ran and user was selected
                                leave_button.setVisibility(Button.GONE);
                            } else {
                                leave_button.setVisibility(Button.VISIBLE);
                            }
                            acceptInvitationButton.setVisibility(Button.GONE);
                            declineInvitationButton.setVisibility(Button.GONE);
                        } else {
                            // User is NOT enrolled - show Join button (only if event is not closed)
                            if (isEventClosed) {
                                join_button.setVisibility(Button.GONE);
                            } else {
                                join_button.setVisibility(Button.VISIBLE);
                            }
                            leave_button.setVisibility(Button.GONE);
                            acceptInvitationButton.setVisibility(Button.GONE);
                            declineInvitationButton.setVisibility(Button.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error reading waitlist", e));
    }
    protected void populateUI() {
        if (eventId != null) {
            db.collection("Events").document(eventId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("Name");
                            String cost = documentSnapshot.getString("Cost");
                            DocumentReference organizer = documentSnapshot.getDocumentReference("Organizer");
                            String image = documentSnapshot.getString("Image");
                            String lotteryInfo = documentSnapshot.getString("LotteryInfo");
                            String description = documentSnapshot.getString("description");
                            String location = documentSnapshot.getString("location");
                            
                            Date eventStartDate = documentSnapshot.getDate("eventStartDate");
                            Date eventEndDate = documentSnapshot.getDate("eventEndDate");
                            Date registrationOpenDate = documentSnapshot.getDate("registrationOpenDate");
                            Date registrationCloseDate = documentSnapshot.getDate("registrationCloseDate");
                            
                            Integer maxParticipants = documentSnapshot.get("maxParticipants") != null ? 
                                documentSnapshot.getLong("maxParticipants").intValue() : 0;
                            
                            // Get waitlist count (new system - waitingListEntrantIds)
                            List<String> waitingListEntrantIds = (List<String>) documentSnapshot.get("waitingListEntrantIds");
                            int waitlistSize = waitingListEntrantIds != null ? waitingListEntrantIds.size() : 0;
                            
                            // Get selected entrants count (new system)
                            List<String> selectedEntrants = (List<String>) documentSnapshot.get("selectedEntrantIds");
                            int selectedCount = selectedEntrants != null ? selectedEntrants.size() : 0;

                            // Populate basic fields
                            TextView nameView = findViewById(R.id.event_name);
                            nameView.setText(name);
                            
                            // Cost - remove "Cost: " prefix, just show price
                            TextView costView = findViewById(R.id.event_cost);
                            if (cost != null && cost.startsWith("$")) {
                                costView.setText(cost);
                            } else {
                                costView.setText("Free");
                            }
                            
                            // Organizer
                            TextView organizerView = findViewById(R.id.event_organizer);
                            if (organizer != null) {
                                populateOrganizer(organizer, organizerView);
                            } else {
                                organizerView.setText("Organized by Unknown");
                            }
                            
                            // Image
                            ImageView imageView = findViewById(R.id.event_poster);
                            populateImage(image, imageView);
                            
                            // Description
                            TextView descriptionView = findViewById(R.id.event_description);
                            if (description != null && !description.isEmpty()) {
                                descriptionView.setText(description);
                            } else {
                                descriptionView.setText("No description available.");
                            }
                            
                            // Location
                            TextView locationView = findViewById(R.id.event_location);
                            if (location != null && !location.isEmpty()) {
                                locationView.setText(location);
                            } else {
                                locationView.setText("Location TBD");
                            }
                            // Location is now inside a LinearLayout, so the TextView reference still works
                            
                            // Date range
                            TextView dateView = findViewById(R.id.event_date);
                            if (eventStartDate != null && eventEndDate != null) {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                                dateView.setText(dateFormat.format(eventStartDate) + " - " + dateFormat.format(eventEndDate));
                            } else if (eventStartDate != null) {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                                dateView.setText(dateFormat.format(eventStartDate));
                            } else {
                                dateView.setText("Date TBD");
                            }
                            
                            // Time
                            TextView timeView = findViewById(R.id.event_time);
                            if (eventStartDate != null) {
                                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                                timeView.setText(timeFormat.format(eventStartDate));
                            } else {
                                timeView.setText("Time TBD");
                            }
                            
                            // Schedule (for now, just show "One-time event" or calculate if recurring)
                            TextView scheduleView = findViewById(R.id.event_schedule);
                            scheduleView.setText("One-time event");
                            
                            // Status tag - show selected count vs max participants
                            TextView statusTag = findViewById(R.id.event_status_tag);
                            updateStatusTag(statusTag, documentSnapshot, selectedCount, maxParticipants);
                            
                            // Registration info
                            TextView registrationInfo = findViewById(R.id.registration_info);
                            // Check if user is in waitlist (check both old and new systems)
                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            boolean userInWaitlist = false;
                            if (auth.getCurrentUser() != null) {
                                String userId = auth.getCurrentUser().getUid();
                                
                                // Check new system (waitingListEntrantIds)
                                if (waitingListEntrantIds != null && waitingListEntrantIds.contains(userId)) {
                                    userInWaitlist = true;
                                } else {
                                    // Check old system (Waitlist) for backward compatibility
                                    List<DocumentReference> waitlist = (List<DocumentReference>) documentSnapshot.get("Waitlist");
                                    if (waitlist != null) {
                                        DocumentReference user_ref = db.collection("users").document(userId);
                                        userInWaitlist = waitlist.contains(user_ref);
                                    }
                                }
                            }
                            updateRegistrationInfo(registrationInfo, registrationOpenDate, registrationCloseDate, userInWaitlist);
                            
                            // Set waitlist count
                            tvWaitlistCount.setText("Waiting List Entrants: " + waitlistSize);
                            tvWaitlistCount.setVisibility(View.VISIBLE);
                            
                            // Lottery info
                            displayLotteryCriteria(lotteryInfo);
                        }
                    });
        }
    }
    
    private void updateStatusTag(TextView statusTag, DocumentSnapshot documentSnapshot, int selectedCount, int maxParticipants) {
        Date now = new Date();
        Date registrationOpenDate = documentSnapshot.getDate("registrationOpenDate");
        Date registrationCloseDate = documentSnapshot.getDate("registrationCloseDate");
        
        boolean isOpen = true;
        if (registrationOpenDate != null && now.before(registrationOpenDate)) {
            isOpen = false; // Registration not open yet
        } else if (registrationCloseDate != null && now.after(registrationCloseDate)) {
            isOpen = false; // Registration closed
        }
        
        // Ensure maxParticipants is valid (at least 1)
        if (maxParticipants <= 0) {
            maxParticipants = 1; // Default to 1 to avoid division by zero or weird display
        }
        
        String statusText;
        if (isOpen) {
            statusText = "🟢 Open - " + selectedCount + "/" + maxParticipants;
        } else {
            statusText = "🔴 Closed - " + selectedCount + "/" + maxParticipants;
        }
        
        statusTag.setText(statusText);
    }
    
    private void updateRegistrationInfo(TextView registrationInfo, Date registrationOpenDate, Date registrationCloseDate, boolean userInWaitlist) {
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        
        if (userInWaitlist) {
            // User is in waiting list - show days remaining
            long daysRemaining = 0;
            String message = "";
            
            if (registrationOpenDate != null && now.before(registrationOpenDate)) {
                // Registration hasn't opened yet
                long diffInMillis = registrationOpenDate.getTime() - now.getTime();
                daysRemaining = diffInMillis / (1000 * 60 * 60 * 24);
                message = daysRemaining + " day" + (daysRemaining != 1 ? "s" : "") + " until registration opens";
            } else if (registrationCloseDate != null) {
                // Registration is open, show days until lottery/selection
                long diffInMillis = registrationCloseDate.getTime() - now.getTime();
                daysRemaining = diffInMillis / (1000 * 60 * 60 * 24);
                if (daysRemaining > 0) {
                    message = daysRemaining + " day" + (daysRemaining != 1 ? "s" : "") + " until lottery selection";
                } else if (daysRemaining == 0) {
                    message = "Lottery selection today";
                } else {
                    message = "Lottery selection completed";
                }
            }
            
            if (!message.isEmpty()) {
                registrationInfo.setText(message);
            } else {
                registrationInfo.setVisibility(View.GONE);
            }
        } else {
            // User not in waiting list - show normal registration info
            if (registrationOpenDate != null && now.before(registrationOpenDate)) {
                registrationInfo.setText("Registration opens " + dateFormat.format(registrationOpenDate));
            } else if (registrationCloseDate != null) {
                registrationInfo.setText("Registration closes " + dateFormat.format(registrationCloseDate));
            } else {
                registrationInfo.setVisibility(View.GONE);
            }
        }
    }

    protected void populateOrganizer(DocumentReference organizerRef, TextView organizerView) {
        organizerRef.get().addOnSuccessListener(userSnapshot -> {
            if (userSnapshot.exists()) {
                String organizerName = userSnapshot.getString("name");
                if (organizerName != null && !organizerName.isEmpty()) {
                    organizerView.setText("Organized by " + organizerName);
                } else {
                    organizerView.setText("Organized by Unknown");
                }
            } else {
                organizerView.setText("Organized by Unknown");
            }
        }).addOnFailureListener(e -> {
            organizerView.setText("Organized by Unknown");
        });
    }

    protected void populateImage(String base64Image, ImageView holder) {
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                // Remove the "data:image/jpeg;base64," or similar prefix
                if (base64Image.startsWith("data:image")) {
                    base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                }

                byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                holder.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e("EventAdapter", "Failed to decode image: " + e.getMessage());
                // Set placeholder if image fails to load
                holder.setImageResource(R.drawable.event_placeholder);
            }
        } else {
            // Set placeholder if no image
            holder.setImageResource(R.drawable.event_placeholder);
        }
    }

    /**
     * US 01.05.05: Display lottery selection criteria to entrants
     */
    private void displayLotteryCriteria(String lotteryInfo) {
        if (lotteryInfo != null && !lotteryInfo.isEmpty()) {
            tvLotteryInfo.setText("Lottery Info: " + lotteryInfo);
            tvLotteryInfo.setVisibility(View.VISIBLE);
        } else {
            tvLotteryInfo.setText("Lottery Info: Random selection. All entrants have equal chance.");
            tvLotteryInfo.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Updates the status message to inform users about their lottery status.
     * Shows rejection message if lottery has run and user was not selected.
     */
    private void updateStatusMessage(boolean isEventClosed, boolean isSelected, boolean isAccepted, 
                                     boolean isInWaitingList, boolean isCancelled, boolean isDeclined) {
        if (tvStatusMessage == null) {
            return;
        }
        
        // If lottery has run (event closed) and user is in waiting list but not selected/accepted, they were rejected
        if (isEventClosed && isInWaitingList && !isSelected && !isAccepted && !isCancelled && !isDeclined) {
            tvStatusMessage.setText("❌ You were not selected in the lottery draw. You remain on the waiting list.");
            tvStatusMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvStatusMessage.setVisibility(View.VISIBLE);
        } else if (isSelected && !isAccepted) {
            tvStatusMessage.setText("🎉 Congratulations! You've been selected in the lottery. Please accept or decline your invitation.");
            tvStatusMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            tvStatusMessage.setVisibility(View.VISIBLE);
        } else if (isAccepted) {
            tvStatusMessage.setText("✅ You're registered for this event!");
            tvStatusMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            tvStatusMessage.setVisibility(View.VISIBLE);
        } else if (isCancelled) {
            tvStatusMessage.setText("ℹ️ You cancelled your registration for this event.");
            tvStatusMessage.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvStatusMessage.setVisibility(View.VISIBLE);
        } else if (isDeclined) {
            tvStatusMessage.setText("ℹ️ You declined the invitation for this event.");
            tvStatusMessage.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvStatusMessage.setVisibility(View.VISIBLE);
        } else {
            tvStatusMessage.setVisibility(View.GONE);
        }
    }
    /**
     * US 01.05.02: Accept invitation to register for event
     */
    private void acceptInvitation() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        lotteryController.acceptInvitation(eventId, userId, new LotteryController.AcceptCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EventDetailsActivity.this, "Invitation accepted! You're registered for the event.", Toast.LENGTH_SHORT).show();
                // Refresh the UI to show updated status
                userInWaitlist();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(EventDetailsActivity.this, "Error accepting invitation: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * US 01.05.03: Decline invitation to participate in event
     */
    private void declineInvitation() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Show confirmation dialog
        new android.app.AlertDialog.Builder(this)
                .setTitle("Decline Invitation")
                .setMessage("Are you sure you want to decline this invitation?")
                .setPositiveButton("Decline", (dialog, which) -> {
                    lotteryController.declineInvitation(eventId, userId, new LotteryController.DeclineCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(EventDetailsActivity.this, "Invitation declined.", Toast.LENGTH_SHORT).show();
                            // Refresh the UI to show updated status
                            userInWaitlist();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(EventDetailsActivity.this, "Error declining invitation: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}