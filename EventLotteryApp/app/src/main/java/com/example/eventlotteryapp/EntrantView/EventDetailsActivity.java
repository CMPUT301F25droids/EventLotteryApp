package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.example.eventlotteryapp.Controllers.LotteryController;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.EntrantView.JoinConfirmationFragment;
import com.example.eventlotteryapp.EntrantView.LeaveConfirmationFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.example.eventlotteryapp.NotificationController;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import com.example.eventlotteryapp.Controllers.LotteryController;
import android.widget.Toast;

/**
 * Activity displaying detailed information about an event for entrants.
 * Shows event details, allows joining/leaving the waiting list, and handles
 * accepting/declining lottery invitations. Manages complex state logic for
 * different entrant statuses (waiting list, selected, accepted, declined, cancelled).
 * 
 * @author EventLotteryApp Team
 */
public class EventDetailsActivity extends AppCompatActivity {
    /** The unique identifier of the event being displayed. */
    private String eventId;
    
    /** Firestore database instance for querying event data. */
    private FirebaseFirestore db;

    /** TextView displaying lottery selection criteria information. */
    private TextView tvLotteryInfo;
    
    /** TextView displaying the current waiting list count. */
    private TextView tvWaitlistCount;
    
    /** TextView displaying status messages to the user. */
    private TextView tvStatusMessage;
    
    /** TextView displaying "You've Been Selected!" message when applicable. */
    private TextView tvSelectedStatus;

    /** Controller for sending notifications. */
    private NotificationController notificationController;

    /** Button for accepting a lottery invitation. */
    private Button acceptInvitationButton;
    
    /** Button for declining a lottery invitation. */
    private Button declineInvitationButton;
    
    /** Controller for managing lottery invitation acceptance/decline operations. */
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
            // Check if event is closed and waiting list limit before showing join dialog
            db.collection("Events").document(eventId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Date registrationCloseDate = documentSnapshot.getDate("registrationCloseDate");
                            Date registrationOpenDate = documentSnapshot.getDate("registrationOpenDate");
                            Date now = new Date();
                            boolean isEventClosed = (registrationCloseDate != null && now.after(registrationCloseDate));
                            boolean isRegistrationOpen = (registrationOpenDate == null || now.after(registrationOpenDate) || now.equals(registrationOpenDate));
                            
                            if (isEventClosed) {
                                Toast.makeText(this, "Registration for this event is closed.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            if (!isRegistrationOpen) {
                                Toast.makeText(this, "Registration for this event has not opened yet.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            // Check waiting list limit
                            Boolean limitWaitingList = documentSnapshot.getBoolean("limitWaitingList");
                            boolean isLimitEnabled = (limitWaitingList != null && limitWaitingList);
                            
                            if (isLimitEnabled) {
                                Long waitingListSizeLong = documentSnapshot.getLong("waitingListSize");
                                int waitingListLimit = (waitingListSizeLong != null) ? waitingListSizeLong.intValue() : 0;
                                
                                // If limit is 0 or not set, treat as infinite (no limit)
                                if (waitingListLimit > 0) {
                                    List<String> waitingListEntrantIds = (List<String>) documentSnapshot.get("waitingListEntrantIds");
                                    int currentWaitingListSize = (waitingListEntrantIds != null) ? waitingListEntrantIds.size() : 0;
                                    
                                    if (currentWaitingListSize >= waitingListLimit) {
                                        Toast.makeText(this, "The waiting list is full. Maximum " + waitingListLimit + " entrants allowed.", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                }
                            }
                            
                            JoinConfirmationFragment confirmation = new JoinConfirmationFragment().newInstance(eventId);
                            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
                            // Send notification to organizer about the new entrant
                            notificationController.sendToOrganizer(eventId,
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
                            // Note: userInWaitlist() will be called after the fragment successfully removes the user
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
        tvSelectedStatus = findViewById(R.id.selected_status_text);
        
        // Menu button (3 dots)
        ImageView menuButton = findViewById(R.id.menu_button);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> showMenu(v));
        }
        
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

    /**
     * Public method to refresh the waiting list status - can be called from fragments
     * Forces a server read to avoid cache issues after updates
     */
    public void refreshWaitingListStatus() {
        userInWaitlist(true);
    }
    
    protected void userInWaitlist() {
        userInWaitlist(false);
    }
    
    protected void userInWaitlist(boolean forceServerRead) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        Log.d("Firestore", "Checking waitlist for eventId=" + eventId + ", userId=" + userId);

        // Use server source if forcing a fresh read, otherwise use default (cache first)
        if (forceServerRead) {
            db.collection("Events").document(eventId)
                    .get(Source.SERVER)
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
                        Date registrationOpenDate = documentSnapshot.getDate("registrationOpenDate");
                        Date now = new Date();
                        boolean isEventClosed = (registrationCloseDate != null && now.after(registrationCloseDate));
                        boolean isRegistrationOpen = (registrationOpenDate == null || now.after(registrationOpenDate) || now.equals(registrationOpenDate));

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
                            
                            // Show/hide menu button based on registration status
                            // Only show if user is registered (in waiting list, selected, or accepted)
                            ImageView menuButton = findViewById(R.id.menu_button);
                            if (menuButton != null) {
                                boolean isRegistered = isInWaitingList || isSelected || isAccepted;
                                menuButton.setVisibility(isRegistered ? View.VISIBLE : View.GONE);
                            }
                            
                            // Show appropriate buttons based on status
                            if (isSelected) {
                            // User is SELECTED - show Accept/Decline buttons
                            join_button.setVisibility(Button.GONE);
                            leave_button.setVisibility(Button.GONE);
                            acceptInvitationButton.setVisibility(Button.VISIBLE);
                            declineInvitationButton.setVisibility(Button.VISIBLE);
                            // Show "You've Been Selected!" status text
                            if (tvSelectedStatus != null) {
                                tvSelectedStatus.setVisibility(View.VISIBLE);
                            }
                            // Adjust event name margin when selected status is visible
                            TextView eventName = findViewById(R.id.event_name);
                            if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                params.topMargin = (int) (4 * getResources().getDisplayMetrics().density); // 4dp
                                eventName.setLayoutParams(params);
                            }
                        } else if (isAccepted && !isCancelled) {
                            // User has ACCEPTED (and NOT cancelled) - show confirmation message
                            join_button.setVisibility(Button.GONE);
                            leave_button.setVisibility(Button.GONE);
                            acceptInvitationButton.setVisibility(Button.GONE);
                            declineInvitationButton.setVisibility(Button.GONE);
                            // Hide selected status text
                            if (tvSelectedStatus != null) {
                                tvSelectedStatus.setVisibility(View.GONE);
                            }
                            // Reset event name margin when selected status is hidden
                            TextView eventName = findViewById(R.id.event_name);
                            if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                params.topMargin = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                                eventName.setLayoutParams(params);
                            }
                            // Optionally show a "You're registered!" message
                        } else if (isDeclined || isCancelled) {
                            // User has DECLINED or CANCELLED - show Join Waitlist button to rejoin (only if event not closed)
                            leave_button.setVisibility(Button.GONE);
                            if (isEventClosed) {
                                join_button.setVisibility(Button.GONE);
                            } else {
                                // Check waiting list limit
                                Boolean limitWaitingList = documentSnapshot.getBoolean("limitWaitingList");
                                boolean isLimitEnabled = (limitWaitingList != null && limitWaitingList);
                                boolean canJoin = true;
                                
                                if (isLimitEnabled) {
                                    Long waitingListSizeLong = documentSnapshot.getLong("waitingListSize");
                                    int waitingListLimit = (waitingListSizeLong != null) ? waitingListSizeLong.intValue() : 0;
                                    
                                    // If limit is 0 or not set, treat as infinite (no limit)
                                    if (waitingListLimit > 0) {
                                        int currentWaitingListSize = (waitingListEntrantIds != null) ? waitingListEntrantIds.size() : 0;
                                        if (currentWaitingListSize >= waitingListLimit) {
                                            canJoin = false;
                                        }
                                    }
                                }
                                
                                if (!canJoin) {
                                    join_button.setVisibility(Button.GONE);
                                } else {
                                    join_button.setVisibility(Button.VISIBLE);
                                    // Disable button if registration hasn't opened yet
                                    join_button.setEnabled(isRegistrationOpen);
                                    // Update button text and background color based on registration status
                                    if (!isRegistrationOpen) {
                                        join_button.setText("Registration is not open");
                                        join_button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                ContextCompat.getColor(this, R.color.medium_grey)));
                                    } else {
                                        join_button.setText("Join Waiting List");
                                        join_button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                ContextCompat.getColor(this, R.color.selected_tab_color)));
                                    }
                                }
                            }
                            acceptInvitationButton.setVisibility(Button.GONE);
                            declineInvitationButton.setVisibility(Button.GONE);
                            // Hide selected status text
                            if (tvSelectedStatus != null) {
                                tvSelectedStatus.setVisibility(View.GONE);
                            }
                            // Reset event name margin when selected status is hidden
                            TextView eventName = findViewById(R.id.event_name);
                            if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                params.topMargin = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                                eventName.setLayoutParams(params);
                            }
                        } else if (isInWaitingList) {
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
                            // Hide selected status text
                            if (tvSelectedStatus != null) {
                                tvSelectedStatus.setVisibility(View.GONE);
                            }
                            // Reset event name margin when selected status is hidden
                            TextView eventName = findViewById(R.id.event_name);
                            if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                params.topMargin = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                                eventName.setLayoutParams(params);
                            }
                        } else {
                            // User is NOT enrolled - show Join button (only if event is not closed and waiting list not full)
                            if (isEventClosed) {
                                join_button.setVisibility(Button.GONE);
                            } else {
                                // Check waiting list limit
                                Boolean limitWaitingList = documentSnapshot.getBoolean("limitWaitingList");
                                boolean isLimitEnabled = (limitWaitingList != null && limitWaitingList);
                                boolean canJoin = true;
                                
                                if (isLimitEnabled) {
                                    Long waitingListSizeLong = documentSnapshot.getLong("waitingListSize");
                                    int waitingListLimit = (waitingListSizeLong != null) ? waitingListSizeLong.intValue() : 0;
                                    
                                    // If limit is 0 or not set, treat as infinite (no limit)
                                    if (waitingListLimit > 0) {
                                        int currentWaitingListSize = (waitingListEntrantIds != null) ? waitingListEntrantIds.size() : 0;
                                        if (currentWaitingListSize >= waitingListLimit) {
                                            canJoin = false;
                                        }
                                    }
                                }
                                
                                // Show button but disable it if registration hasn't opened yet
                                // Hide button if waitlist is full
                                if (!canJoin) {
                                    join_button.setVisibility(Button.GONE);
                                } else {
                                    join_button.setVisibility(Button.VISIBLE);
                                    // Disable button if registration hasn't opened yet
                                    join_button.setEnabled(isRegistrationOpen);
                                    // Update button text and background color based on registration status
                                    if (!isRegistrationOpen) {
                                        join_button.setText("Registration is not open");
                                        // Set button background to grey when registration is not open
                                        join_button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                ContextCompat.getColor(this, R.color.medium_grey)));
                                    } else {
                                        join_button.setText("Join Waiting List");
                                        // Set button background to purple when registration is open
                                        join_button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                ContextCompat.getColor(this, R.color.selected_tab_color)));
                                    }
                                }
                            }
                            leave_button.setVisibility(Button.GONE);
                            acceptInvitationButton.setVisibility(Button.GONE);
                            declineInvitationButton.setVisibility(Button.GONE);
                            // Hide selected status text
                            if (tvSelectedStatus != null) {
                                tvSelectedStatus.setVisibility(View.GONE);
                            }
                            // Reset event name margin when selected status is hidden
                            TextView eventName = findViewById(R.id.event_name);
                            if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                params.topMargin = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                                eventName.setLayoutParams(params);
                            }
                        }
                    }
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error reading waitlist", e));
        } else {
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
                            Date registrationOpenDate = documentSnapshot.getDate("registrationOpenDate");
                            Date now = new Date();
                            boolean isEventClosed = (registrationCloseDate != null && now.after(registrationCloseDate));
                            boolean isRegistrationOpen = (registrationOpenDate == null || now.after(registrationOpenDate) || now.equals(registrationOpenDate));

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

                            // Show/hide menu button based on registration status
                            // Only show if user is registered (in waiting list, selected, or accepted)
                            ImageView menuButton = findViewById(R.id.menu_button);
                            if (menuButton != null) {
                                boolean isRegistered = isInWaitingList || isSelected || isAccepted;
                                menuButton.setVisibility(isRegistered ? View.VISIBLE : View.GONE);
                            }

                            // Show appropriate buttons based on status
                            if (isSelected) {
                                // User is SELECTED - show Accept/Decline buttons
                                join_button.setVisibility(Button.GONE);
                                leave_button.setVisibility(Button.GONE);
                                acceptInvitationButton.setVisibility(Button.VISIBLE);
                                declineInvitationButton.setVisibility(Button.VISIBLE);
                                // Show "You've Been Selected!" status text
                                if (tvSelectedStatus != null) {
                                    tvSelectedStatus.setVisibility(View.VISIBLE);
                                }
                                // Adjust event name margin when selected status is visible
                                TextView eventName = findViewById(R.id.event_name);
                                if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                    params.topMargin = (int) (4 * getResources().getDisplayMetrics().density); // 4dp
                                    eventName.setLayoutParams(params);
                                }
                            } else if (isAccepted && !isCancelled) {
                                // User has ACCEPTED (and NOT cancelled) - show confirmation message
                                join_button.setVisibility(Button.GONE);
                                leave_button.setVisibility(Button.GONE);
                                acceptInvitationButton.setVisibility(Button.GONE);
                                declineInvitationButton.setVisibility(Button.GONE);
                                // Hide selected status text
                                if (tvSelectedStatus != null) {
                                    tvSelectedStatus.setVisibility(View.GONE);
                                }
                                // Reset event name margin when selected status is hidden
                                TextView eventName = findViewById(R.id.event_name);
                                if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                    params.topMargin = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                                    eventName.setLayoutParams(params);
                                }
                                // Optionally show a "You're registered!" message
                            } else if (isDeclined || isCancelled) {
                                // User has DECLINED or CANCELLED - show Join Waitlist button to rejoin (only if event not closed)
                                leave_button.setVisibility(Button.GONE);
                                if (isEventClosed) {
                                    join_button.setVisibility(Button.GONE);
                                } else {
                                    // Check waiting list limit
                                    Boolean limitWaitingList = documentSnapshot.getBoolean("limitWaitingList");
                                    boolean isLimitEnabled = (limitWaitingList != null && limitWaitingList);
                                    boolean canJoin = true;
                                    
                                    if (isLimitEnabled) {
                                        Long waitingListSizeLong = documentSnapshot.getLong("waitingListSize");
                                        int waitingListLimit = (waitingListSizeLong != null) ? waitingListSizeLong.intValue() : 0;
                                        
                                        // If limit is 0 or not set, treat as infinite (no limit)
                                        if (waitingListLimit > 0) {
                                            int currentWaitingListSize = (waitingListEntrantIds != null) ? waitingListEntrantIds.size() : 0;
                                            if (currentWaitingListSize >= waitingListLimit) {
                                                canJoin = false;
                                            }
                                        }
                                    }
                                    
                                    if (!canJoin) {
                                        join_button.setVisibility(Button.GONE);
                                    } else {
                                        join_button.setVisibility(Button.VISIBLE);
                                        // Disable button if registration hasn't opened yet
                                        join_button.setEnabled(isRegistrationOpen);
                                        // Update button text and background color based on registration status
                                        if (!isRegistrationOpen) {
                                            join_button.setText("Registration is not open");
                                            join_button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                    ContextCompat.getColor(this, R.color.medium_grey)));
                                        } else {
                                            join_button.setText("Join Waiting List");
                                            join_button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                    ContextCompat.getColor(this, R.color.selected_tab_color)));
                                        }
                                    }
                                }
                                acceptInvitationButton.setVisibility(Button.GONE);
                                declineInvitationButton.setVisibility(Button.GONE);
                                // Hide selected status text
                                if (tvSelectedStatus != null) {
                                    tvSelectedStatus.setVisibility(View.GONE);
                                }
                                // Reset event name margin when selected status is hidden
                                TextView eventName = findViewById(R.id.event_name);
                                if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                    params.topMargin = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                                    eventName.setLayoutParams(params);
                                }
                            } else if (isInWaitingList) {
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
                                // Hide selected status text
                                if (tvSelectedStatus != null) {
                                    tvSelectedStatus.setVisibility(View.GONE);
                                }
                                // Reset event name margin when selected status is hidden
                                TextView eventName = findViewById(R.id.event_name);
                                if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                    params.topMargin = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                                    eventName.setLayoutParams(params);
                                }
                            } else {
                                // User is NOT enrolled - show Join button (only if event is not closed and waiting list not full)
                                if (isEventClosed) {
                                    join_button.setVisibility(Button.GONE);
                                } else {
                                    // Check waiting list limit
                                    Boolean limitWaitingList = documentSnapshot.getBoolean("limitWaitingList");
                                    boolean isLimitEnabled = (limitWaitingList != null && limitWaitingList);
                                    boolean canJoin = true;
                                    
                                    if (isLimitEnabled) {
                                        Long waitingListSizeLong = documentSnapshot.getLong("waitingListSize");
                                        int waitingListLimit = (waitingListSizeLong != null) ? waitingListSizeLong.intValue() : 0;
                                        
                                        // If limit is 0 or not set, treat as infinite (no limit)
                                        if (waitingListLimit > 0) {
                                            int currentWaitingListSize = (waitingListEntrantIds != null) ? waitingListEntrantIds.size() : 0;
                                            if (currentWaitingListSize >= waitingListLimit) {
                                                canJoin = false;
                                            }
                                        }
                                    }
                                    
                                    // Show button but disable it if registration hasn't opened yet
                                    // Hide button if waitlist is full
                                    if (!canJoin) {
                                        join_button.setVisibility(Button.GONE);
                                    } else {
                                        join_button.setVisibility(Button.VISIBLE);
                                        // Disable button if registration hasn't opened yet
                                        join_button.setEnabled(isRegistrationOpen);
                                        // Update button text and background color based on registration status
                                        if (!isRegistrationOpen) {
                                            join_button.setText("Registration is not open");
                                            // Set button background to grey when registration is not open
                                            join_button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                    ContextCompat.getColor(this, R.color.medium_grey)));
                                        } else {
                                            join_button.setText("Join Waiting List");
                                            // Set button background to purple when registration is open
                                            join_button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                                    ContextCompat.getColor(this, R.color.selected_tab_color)));
                                        }
                                    }
                                }
                                leave_button.setVisibility(Button.GONE);
                                acceptInvitationButton.setVisibility(Button.GONE);
                                declineInvitationButton.setVisibility(Button.GONE);
                                // Hide selected status text
                                if (tvSelectedStatus != null) {
                                    tvSelectedStatus.setVisibility(View.GONE);
                                }
                                // Reset event name margin when selected status is hidden
                                TextView eventName = findViewById(R.id.event_name);
                                if (eventName != null && eventName.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) eventName.getLayoutParams();
                                    params.topMargin = (int) (16 * getResources().getDisplayMetrics().density); // 16dp
                                    eventName.setLayoutParams(params);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error reading waitlist", e));
        }
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
                            
                            // Status tag - show waitlist count vs waiting list size (like organizer view)
                            TextView statusTag = findViewById(R.id.event_status_tag);
                            updateStatusTag(statusTag, documentSnapshot, waitlistSize);
                            
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
                            
                            // Hide waitlist count
                            tvWaitlistCount.setVisibility(View.GONE);
                            
                            // Lottery info
                            displayLotteryCriteria(lotteryInfo);
                        }
                    });
        }
    }
    
    private void updateStatusTag(TextView statusTag, DocumentSnapshot documentSnapshot, int waitlistCount) {
        Date now = new Date();
        Date registrationOpenDate = documentSnapshot.getDate("registrationOpenDate");
        Date registrationCloseDate = documentSnapshot.getDate("registrationCloseDate");
        
        boolean isOpen = true;
        if (registrationOpenDate != null && now.before(registrationOpenDate)) {
            isOpen = false; // Registration not open yet
        } else if (registrationCloseDate != null && now.after(registrationCloseDate)) {
            isOpen = false; // Registration closed
        }
        
        // Check if waiting list is limited (like organizer view does)
        Boolean limitWaitingList = documentSnapshot.getBoolean("limitWaitingList");
        boolean isLimitEnabled = (limitWaitingList != null && limitWaitingList);
        Long waitingListSizeLong = documentSnapshot.getLong("waitingListSize");
        int waitingListSize = (waitingListSizeLong != null) ? waitingListSizeLong.intValue() : 0;
        
        // Build status text - use waiting list size as denominator if limited, otherwise show just numerator
        // Numerator is the current waiting list count, denominator is waitingListSize
        String statusText;
        String countText;
        if (isLimitEnabled && waitingListSize > 0) {
            // Waiting list is limited - show numerator/denominator
            countText = waitlistCount + "/" + waitingListSize;
        } else {
            // Waiting list is infinite - show just numerator without slash
            countText = String.valueOf(waitlistCount);
        }
        
        if (isOpen) {
            statusText = "🟢 Open - " + countText;
        } else {
            statusText = "🔴 Closed - " + countText;
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
                // Show days until registration opens
                long diffInMillis = registrationOpenDate.getTime() - now.getTime();
                long daysRemaining = diffInMillis / (1000 * 60 * 60 * 24);
                registrationInfo.setText(daysRemaining + " day" + (daysRemaining != 1 ? "s" : "") + " until registration opens");
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
        
        // Check cancelled FIRST - cancelled users should not see "registered" message
        if (isCancelled) {
            // Create inactive pill button style message
            String cancelledText = "❌ The organizer cancelled your registration.";
            SpannableString spannable = new SpannableString(cancelledText);
            // Make the checkmark grey (inactive look)
            spannable.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.medium_grey)),
                0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            // Rest of text is dark grey for inactive look
            spannable.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.medium_grey)),
                1, cancelledText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            tvStatusMessage.setText(spannable);
            tvStatusMessage.setBackgroundResource(R.drawable.success_button_background);
            tvStatusMessage.setTextSize(17);
            tvStatusMessage.setTypeface(tvStatusMessage.getTypeface(), android.graphics.Typeface.BOLD);
            tvStatusMessage.setPadding(
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
            );
            tvStatusMessage.setVisibility(View.VISIBLE);
        } else if (isDeclined) {
            // Hide status message for declined - we'll show Join Waitlist button instead
            tvStatusMessage.setVisibility(View.GONE);
        } else if (isEventClosed && isInWaitingList && !isSelected && !isAccepted) {
            // If lottery has run (event closed) and user is in waiting list but not selected/accepted, they were rejected
            tvStatusMessage.setText("❌ You were not selected in the lottery draw. You remain on the waiting list.");
            tvStatusMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvStatusMessage.setVisibility(View.VISIBLE);
        } else if (isSelected) {
            // Hide the status message when selected - we show "You've Been Selected!" text instead
            // Even if they're also in acceptedIds (stale from previous acceptance), they need to accept again
            tvStatusMessage.setVisibility(View.GONE);
        } else if (isAccepted && !isCancelled) {
            // Create a nice success message with green checkmark in inactive pill button style
            String successText = "✓ You're registered for this event!";
            SpannableString spannable = new SpannableString(successText);
            // Make the checkmark green
            spannable.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.success_green)),
                0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            // Rest of text is dark grey for inactive look
            spannable.setSpan(
                new ForegroundColorSpan(getResources().getColor(R.color.medium_grey)),
                1, successText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            tvStatusMessage.setText(spannable);
            tvStatusMessage.setBackgroundResource(R.drawable.success_button_background);
            tvStatusMessage.setTextSize(17);
            tvStatusMessage.setTypeface(tvStatusMessage.getTypeface(), android.graphics.Typeface.BOLD);
            tvStatusMessage.setPadding(
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
            );
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

        // Disable buttons immediately to prevent spam clicking
        acceptInvitationButton.setEnabled(false);
        declineInvitationButton.setEnabled(false);
        
        // Update UI immediately to show accepted state
        acceptInvitationButton.setVisibility(Button.GONE);
        declineInvitationButton.setVisibility(Button.GONE);
        if (tvSelectedStatus != null) {
            tvSelectedStatus.setVisibility(View.GONE);
        }
        
        // Show registered message immediately
        updateStatusMessage(false, false, true, false, false, false);

        lotteryController.acceptInvitation(eventId, userId, new LotteryController.AcceptCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(EventDetailsActivity.this, "Invitation accepted! You're registered for the event.", Toast.LENGTH_SHORT).show();
                // Refresh the UI to show updated status (this will confirm the state from Firestore)
                userInWaitlist();
            }

            @Override
            public void onFailure(String error) {
                // Re-enable buttons on error
                acceptInvitationButton.setEnabled(true);
                declineInvitationButton.setEnabled(true);
                acceptInvitationButton.setVisibility(Button.VISIBLE);
                declineInvitationButton.setVisibility(Button.VISIBLE);
                if (tvSelectedStatus != null) {
                    tvSelectedStatus.setVisibility(View.VISIBLE);
                }
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
                    // Disable buttons immediately to prevent spam clicking
                    acceptInvitationButton.setEnabled(false);
                    declineInvitationButton.setEnabled(false);
                    
                    // Update UI immediately to show declined state
                    acceptInvitationButton.setVisibility(Button.GONE);
                    declineInvitationButton.setVisibility(Button.GONE);
                    if (tvSelectedStatus != null) {
                        tvSelectedStatus.setVisibility(View.GONE);
                    }
                    
                    lotteryController.declineInvitation(eventId, userId, new LotteryController.DeclineCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(EventDetailsActivity.this, "Invitation declined.", Toast.LENGTH_SHORT).show();
                            // Refresh the UI to show updated status (this will confirm the state from Firestore)
                            userInWaitlist();
                        }

                        @Override
                        public void onFailure(String error) {
                            // Re-enable buttons on error
                            acceptInvitationButton.setEnabled(true);
                            declineInvitationButton.setEnabled(true);
                            acceptInvitationButton.setVisibility(Button.VISIBLE);
                            declineInvitationButton.setVisibility(Button.VISIBLE);
                            if (tvSelectedStatus != null) {
                                tvSelectedStatus.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(EventDetailsActivity.this, "Error declining invitation: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Displays a popup menu with option to cancel registration.
     *
     * @param anchor The view to anchor the popup menu to.
     */
    private void showMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.entrant_event_details_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_cancel_registration) {
                showCancelRegistrationDialog();
                return true;
            }
            return false;
        });
        
        try {
            popupMenu.show();
        } catch (Exception e) {
            Log.e("EventDetails", "Error showing popup menu", e);
            Toast.makeText(this, "Error showing menu", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a confirmation dialog for cancelling registration.
     */
    private void showCancelRegistrationDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancel Registration")
                .setMessage("Are you sure you want to cancel your registration for this event?")
                .setPositiveButton("Cancel Registration", (dialog, which) -> {
                    cancelRegistration();
                })
                .setNegativeButton("Keep Registration", null)
                .show();
    }

    /**
     * Cancels the user's registration from the event.
     * Removes from waitlist/accepted/selected and adds to cancelled.
     * Sends notification to organizer only if user was accepted.
     */
    private void cancelRegistration() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DocumentReference eventRef = db.collection("Events").document(eventId);

        // Get current event data to check if user is registered and was accepted
        eventRef.get().addOnSuccessListener(eventDoc -> {
            List<String> waitingListIds = (List<String>) eventDoc.get("waitingListEntrantIds");
            List<String> acceptedIds = (List<String>) eventDoc.get("acceptedEntrantIds");
            List<String> selectedIds = (List<String>) eventDoc.get("selectedEntrantIds");
            List<String> cancelledIds = (List<String>) eventDoc.get("cancelledEntrantIds");

            if (waitingListIds == null) waitingListIds = new ArrayList<>();
            if (acceptedIds == null) acceptedIds = new ArrayList<>();
            if (selectedIds == null) selectedIds = new ArrayList<>();
            if (cancelledIds == null) cancelledIds = new ArrayList<>();

            // Validate that user is actually registered (in waiting list, selected, or accepted)
            boolean isInWaitingList = waitingListIds.contains(userId);
            boolean isSelected = selectedIds.contains(userId);
            boolean isAccepted = acceptedIds.contains(userId);
            boolean isRegistered = isInWaitingList || isSelected || isAccepted;

            if (!isRegistered) {
                Toast.makeText(this, "You are not registered for this event.", Toast.LENGTH_SHORT).show();
                Log.w("EventDetails", "User attempted to cancel registration but is not registered. userId: " + userId);
                return;
            }

            // Check if user was accepted (to send notification)
            boolean wasAccepted = isAccepted;

            // Remove from all lists
            waitingListIds.remove(userId);
            acceptedIds.remove(userId);
            selectedIds.remove(userId);

            // Add to cancelled if not already there
            if (!cancelledIds.contains(userId)) {
                cancelledIds.add(userId);
            }

            // Update Firestore
            eventRef.update(
                    "waitingListEntrantIds", waitingListIds,
                    "acceptedEntrantIds", acceptedIds,
                    "selectedEntrantIds", selectedIds,
                    "cancelledEntrantIds", cancelledIds
            ).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "You've cancelled your registration for this event.", Toast.LENGTH_SHORT).show();
                
                // Send notification to organizer only if user was accepted
                if (wasAccepted) {
                    // Get organizer ID
                    String organizerId = null;
                    DocumentReference organizerRef = eventDoc.getDocumentReference("Organizer");
                    if (organizerRef != null) {
                        String path = organizerRef.getPath();
                        if (path != null && path.contains("/users/")) {
                            organizerId = path.substring(path.lastIndexOf("/") + 1);
                        } else if (path != null) {
                            String[] parts = path.split("/");
                            for (int i = 0; i < parts.length - 1; i++) {
                                if (parts[i].equals("users") && i + 1 < parts.length) {
                                    organizerId = parts[i + 1];
                                    break;
                                }
                            }
                        }
                    }
                    if (organizerId == null || organizerId.isEmpty()) {
                        organizerId = eventDoc.getString("organizerId");
                    }

                    // Make final copy for use in lambda
                    final String finalOrganizerId = organizerId;

                    if (finalOrganizerId != null && !finalOrganizerId.isEmpty()) {
                        // Get event name and user name for notification
                        String eventNameRaw = eventDoc.getString("Name");
                        if (eventNameRaw == null || eventNameRaw.isEmpty()) {
                            eventNameRaw = eventDoc.getString("title");
                        }
                        final String eventName = (eventNameRaw != null && !eventNameRaw.isEmpty()) ? eventNameRaw : "your event";

                        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                            String userNameRaw = userDoc.getString("Name");
                            if (userNameRaw == null || userNameRaw.isEmpty()) {
                                userNameRaw = userDoc.getString("name");
                            }
                            final String userName = (userNameRaw != null && !userNameRaw.isEmpty()) ? userNameRaw : "An entrant";

                            String notificationMessage = userName + " has cancelled their registration for " + eventName + ".";

                            // Create notification for organizer
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("UserId", finalOrganizerId);
                            notification.put("EventId", eventRef);
                            notification.put("Type", "entrant_cancelled");
                            notification.put("Message", notificationMessage);
                            notification.put("TimeStamp", new java.util.Date().toString());
                            notification.put("Read", false);
                            notification.put("UserType", "organizer");

                            db.collection("Notifications").add(notification)
                                    .addOnSuccessListener(ref -> Log.d("EventDetails", "Organizer notified about cancellation: " + ref.getId()))
                                    .addOnFailureListener(e -> Log.e("EventDetails", "Error notifying organizer", e));
                        }).addOnFailureListener(e -> {
                            Log.e("EventDetails", "Error getting user document for notification", e);
                            // Still notify with fallback message
                            String fallbackMessage = "An entrant has cancelled their registration for " + eventName + ".";
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("UserId", finalOrganizerId);
                            notification.put("EventId", eventRef);
                            notification.put("Type", "entrant_cancelled");
                            notification.put("Message", fallbackMessage);
                            notification.put("TimeStamp", new java.util.Date().toString());
                            notification.put("Read", false);
                            notification.put("UserType", "organizer");
                            db.collection("Notifications").add(notification);
                        });
                    }
                }

                // Update UI immediately
                updateStatusMessage(false, false, false, false, true, false);
                userInWaitlist();
            }).addOnFailureListener(e -> {
                Log.e("EventDetails", "Error cancelling registration", e);
                Toast.makeText(this, "Error cancelling registration: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Log.e("EventDetails", "Error loading event data", e);
            Toast.makeText(this, "Error loading event data", Toast.LENGTH_SHORT).show();
        });
    }
}