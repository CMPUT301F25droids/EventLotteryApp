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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.EntrantView.JoinConfirmationFragment;
import com.example.eventlotteryapp.EntrantView.LeaveConfirmationFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.eventlotteryapp.NotificationController;

import java.util.HashMap;
import java.util.List;

public class EventDetailsActivity extends AppCompatActivity {
    private String eventId;
    private FirebaseFirestore db;

    private TextView tvLotteryInfo;
    private TextView tvWaitlistCount;

    private NotificationController notificationController;


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
            JoinConfirmationFragment confirmation = new JoinConfirmationFragment().newInstance(eventId);
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
            // Send notification to organizer about the new entrant
            notificationController.sendToSelectedEntrants(eventId,
                    "New Entrant",
                    "A user has joined the waiting list for your event.");
        });

        Button leave_button = findViewById(R.id.leave_waitlist_button);
        leave_button.setOnClickListener(v -> {
            // Handle leave button click
            LeaveConfirmationFragment confirmation = new LeaveConfirmationFragment().newInstance(eventId);
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
            userInWaitlist();
            // Notify organizer about the user leaving
            notificationController.sendToCancelledEntrants(eventId,
                    "Entrant Left",
                    "A user has left the waiting list for your event.");
        });

        eventId = getIntent().getStringExtra("eventId");
        Log.d("EventDetails", "Event ID: " + eventId);
        db = FirebaseFirestore.getInstance();

        // TEMPORARY: Test invitation response screen
        Button testButton = findViewById(R.id.btn_test_invitation);
        testButton.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailsActivity.this, InvitationResponseActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        tvLotteryInfo = findViewById(R.id.tv_lottery_info);
        tvWaitlistCount = findViewById(R.id.waitlist_count);
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
        DocumentReference user_ref = db.collection("users").document(userId);
        Log.d("Firestore", "Checking waitlist for eventId=" + eventId + ", userId=" + userId);

        db.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Safely retrieve the array field
                        List<DocumentReference> waitlist = (List<DocumentReference>) documentSnapshot.get("Waitlist");
                        Button join_button = findViewById(R.id.join_waitlist_button);
                        Button leave_button = findViewById(R.id.leave_waitlist_button);
                        Log.d("Firestore", "Waitlist from DB: " + waitlist);

                        if (waitlist != null && waitlist.contains(user_ref)) {
                            join_button.setEnabled(false);
                            leave_button.setEnabled(true);
                            join_button.setVisibility(Button.GONE);
                            leave_button.setVisibility(Button.VISIBLE);
                            join_button.setAlpha(0.5f);
                            leave_button.setAlpha(1f);
                        } else {
                            join_button.setEnabled(true);
                            leave_button.setEnabled(false);
                            join_button.setVisibility(Button.VISIBLE);
                            leave_button.setVisibility(Button.GONE);
                            join_button.setAlpha(1f);
                            leave_button.setAlpha(0.5f);

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
                            String lotteryInfo = documentSnapshot.getString("LotteryInfo"); // for lottery info

                            tvLotteryInfo.setText(lotteryInfo);

                            // populate UI
                            TextView nameView = findViewById(R.id.event_name);
                            nameView.setText(name);
                            TextView costView = findViewById(R.id.event_cost);
                            costView.setText("Cost: " + cost);
                            TextView organizerView = findViewById(R.id.event_organizer);
                            assert organizer != null;
                            populateOrganizer(organizer, organizerView);
                            ImageView imageView = findViewById(R.id.event_poster);
                            populateImage(image, imageView);

                            displayLotteryCriteria(lotteryInfo);

                            if (image != null)
                                populateImage(image, findViewById(R.id.event_poster));

                            // Update waitlist count dynamically
                            List<DocumentReference> waitlist = (List<DocumentReference>) documentSnapshot.get("Waitlist");

                            if (waitlist != null) {
                                tvWaitlistCount.setText("Waiting List Entrants: " + waitlist.size());
                            } else {
                                tvWaitlistCount.setText("Waiting List Entrants: 0");
                            }
                        }
                    });
        }
    }

    protected void populateOrganizer(DocumentReference organizerRef, TextView organizerView) {
        organizerRef.get().addOnSuccessListener(userSnapshot -> {
            if (userSnapshot.exists()) {
                String organizerName = userSnapshot.getString("Name");
                organizerView.setText(organizerName);
            }
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
            }
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
}