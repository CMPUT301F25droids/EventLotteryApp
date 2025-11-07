package com.example.eventlotteryapp.EntrantView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

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
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
        });

        // Leave button click
        leaveButton.setOnClickListener(v -> {
            LeaveConfirmationFragment confirmation = new LeaveConfirmationFragment().newInstance(eventId);
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
        });

        // Organizer notification buttons
        notifyWaitlistButton.setOnClickListener(v -> sendNotificationsToGroup("Waitlist", "Event Update", "You’re on the waiting list for this event."));
        notifySelectedButton.setOnClickListener(v -> sendNotificationsToGroup("Selected", "Congratulations!", "You have been selected for the event!"));
        notifyCancelledButton.setOnClickListener(v -> sendNotificationsToGroup("Cancelled", "Update", "Unfortunately, your entry has been cancelled."));

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

    /** Loads event info from Firestore and updates UI */
    private void populateUI() {
        db.collection("Events").document(eventId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Error fetching event data", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("Name");
                        String cost = documentSnapshot.getString("Cost");
                        DocumentReference organizer = documentSnapshot.getDocumentReference("Organizer");
                        String image = documentSnapshot.getString("Image");

                        // Set text fields
                        TextView nameView = findViewById(R.id.event_name);
                        TextView costView = findViewById(R.id.event_cost);
                        TextView organizerView = findViewById(R.id.event_organizer);

                        nameView.setText(name != null ? name : "Unnamed Event");
                        costView.setText(cost != null ? "Cost: " + cost : "Cost: -");

                        if (organizer != null) {
                            populateOrganizer(organizer, organizerView);
                            showOrganizerControlsIfOwner(organizer);
                        }
                        if (image != null) populateImage(image, findViewById(R.id.event_poster));

                        // Update waitlist count dynamically
                        List<DocumentReference> waitlist = (List<DocumentReference>) documentSnapshot.get("Waitlist");
                        if (waitlist != null && waitlistCountView != null) {
                            waitlistCountView.setText(waitlist.size() + " entrants on waitlist");
                        }
                    }
                });
    }

    /** Retrieves and displays organizer name */
    private void populateOrganizer(DocumentReference organizerRef, TextView organizerView) {
        organizerRef.get().addOnSuccessListener(userSnapshot -> {
            if (userSnapshot.exists()) {
                String organizerName = userSnapshot.getString("Name");
                organizerView.setText(organizerName != null ? organizerName : "Unknown Organizer");
            }
        });
    }

    /** Decodes base64 event image and displays it */
    private void populateImage(String base64Image, ImageView holder) {
        if (base64Image == null || base64Image.isEmpty()) return;

        try {
            if (base64Image.startsWith("data:image")) {
                base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
            }

            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.setImageBitmap(bitmap);

        } catch (Exception e) {
            Log.e("EventDetailsActivity", "Failed to decode image: " + e.getMessage());
        }
    }
}
