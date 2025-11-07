package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.content.Intent;

import com.example.eventlotteryapp.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.eventlotteryapp.UserSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDetailsActivity extends AppCompatActivity {

    private String eventId;
    private FirebaseFirestore db;
    private Button joinButton, leaveButton;
    private Button notifyWaitlistButton, notifySelectedButton, notifyCancelledButton;
    private TextView waitlistCountView;
    private DocumentReference currentUserRef;

    private TextView tvLotteryInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId");
        currentUserRef = UserSession.getCurrentUserRef();

        if (eventId == null || eventId.isEmpty()) {
            Log.e("EventDetailsActivity", "Error: Event ID is null");
            finish();
            return;
        }

        Log.d("EventDetails", "Event ID: " + eventId);

        // UI Elements
        ImageView backButton = findViewById(R.id.back_button);
        joinButton = findViewById(R.id.join_waitlist_button);
        leaveButton = findViewById(R.id.leave_waitlist_button);
        waitlistCountView = findViewById(R.id.waitlist_count);

        // Organizer notification buttons
        notifyWaitlistButton = findViewById(R.id.notify_waitlist_button);
        notifySelectedButton = findViewById(R.id.notify_selected_button);
        notifyCancelledButton = findViewById(R.id.notify_cancelled_button);

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Join button click
        joinButton.setOnClickListener(v -> {
            JoinConfirmationFragment confirmation = new JoinConfirmationFragment().newInstance(eventId);
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());});

        // Leave button click
        leaveButton.setOnClickListener(v -> {
            LeaveConfirmationFragment confirmation = new LeaveConfirmationFragment().newInstance(eventId);
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
        });

        // TEMPORARY: Test invitation response screen
        Button testButton = findViewById(R.id.btn_test_invitation);
        testButton.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailsActivity.this, InvitationResponseActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });


        eventId = getIntent().getStringExtra("eventId");
        Log.d("EventDetails", "Event ID: " + eventId);
        db = FirebaseFirestore.getInstance();

        tvLotteryInfo = findViewById(R.id.tv_lottery_info);


        userInWaitlist();

        populateUI();
        updateWaitlistState();
    }

    /** Checks if current user is organizer and updates UI accordingly */
    private void showOrganizerControlsIfOwner(DocumentReference organizerRef) {
        organizerRef.get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getReference().equals(currentUserRef)) {
                notifyWaitlistButton.setVisibility(View.VISIBLE);
                notifySelectedButton.setVisibility(View.VISIBLE);
                notifyCancelledButton.setVisibility(View.VISIBLE);
            } else {
                notifyWaitlistButton.setVisibility(View.GONE);
                notifySelectedButton.setVisibility(View.GONE);
                notifyCancelledButton.setVisibility(View.GONE);
            }
        });
    }

    protected void userInWaitlist() {
            UserSession userSession = new UserSession();
            DocumentReference user_ref = UserSession.getCurrentUserRef();
            Log.d("Firestore", "Checking waitlist for eventId=" + eventId + ", userId=" + user_ref);
        }
    /** Sends notifications to all users in a specific group field (Waitlist / Selected / Cancelled) */
    private void sendNotificationsToGroup(String fieldName, String title, String message) {
        db.collection("Events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) return;

                    List<DocumentReference> recipients = (List<DocumentReference>) eventDoc.get(fieldName);
                    if (recipients == null || recipients.isEmpty()) {
                        Toast.makeText(this, "No entrants in " + fieldName.toLowerCase() + " list.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentReference userRef : recipients) {
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("title", title);
                        notification.put("message", message);
                        notification.put("eventId", eventId);
                        notification.put("timestamp", Timestamp.now());

                        userRef.collection("Notifications").add(notification)
                                .addOnSuccessListener(aVoid -> Log.d("Notifications", "Sent to: " + userRef.getId()))
                                .addOnFailureListener(e -> Log.e("Notifications", "Failed to send: " + e.getMessage()));
                    }

                    Toast.makeText(this, "Notifications sent to " + recipients.size() + " users.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.e("Notifications", "Error sending notifications: " + e.getMessage()));
    }

    /** Updates join/leave button visibility based on whether user is already on waitlist */
    private void updateWaitlistState() {
        db.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<DocumentReference> waitlist = (List<DocumentReference>) documentSnapshot.get("Waitlist");
                        DocumentReference organizer = documentSnapshot.getDocumentReference("Organizer");

                        boolean userInWaitlist = waitlist != null && waitlist.contains(currentUserRef);
                        Log.d("Firestore", "User in waitlist: " + userInWaitlist);

                        if (userInWaitlist) {
                            joinButton.setVisibility(View.GONE);
                            leaveButton.setVisibility(View.VISIBLE);
                        } else {
                            joinButton.setVisibility(View.VISIBLE);
                            leaveButton.setVisibility(View.GONE);
                        }

                        // ✅ Update waitlist count text
                        if (waitlistCountView != null) {
                            int count = waitlist != null ? waitlist.size() : 0;
                            waitlistCountView.setText(count + " entrants on waitlist");
                        }

                        // ✅ Check if organizer
                        if (organizer != null) showOrganizerControlsIfOwner(organizer);
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
                            if (waitlist != null && waitlistCountView != null) {
                                waitlistCountView.setText(waitlist.size() + " entrants on waitlist");
                            }
                        }
                    });
        }
    }

    protected void populateOrganizer(DocumentReference organizerRef, TextView organizerView) {
        organizerRef.get().addOnSuccessListener(userSnapshot -> {
            if (userSnapshot.exists()) {
                String organizerName = userSnapshot.getString("Name");
                organizerView.setText(organizerName != null ? organizerName : "Unknown Organizer");
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
        } else {
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
