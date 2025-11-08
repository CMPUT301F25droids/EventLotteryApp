package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.UserSession;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EventDetailsActivity extends AppCompatActivity {

    private String eventId;
    private FirebaseFirestore db;
    private TextView tvLotteryInfo;
    private SwitchMaterial notificationSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId");

        Log.d("EventDetails", "Event ID: " + eventId);

        ImageView backButton = findViewById(R.id.back_button);
        Button joinButton = findViewById(R.id.join_waitlist_button);
        Button leaveButton = findViewById(R.id.leave_waitlist_button);
        Button testButton = findViewById(R.id.btn_test_invitation);
        tvLotteryInfo = findViewById(R.id.tv_lottery_info);
        notificationSwitch = findViewById(R.id.switch_notifications);

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Join button
        joinButton.setOnClickListener(v -> {
            JoinConfirmationFragment confirmation = new JoinConfirmationFragment().newInstance(eventId);
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
        });

        // Leave button
        leaveButton.setOnClickListener(v -> {
            LeaveConfirmationFragment confirmation = new LeaveConfirmationFragment().newInstance(eventId);
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
            userInWaitlist();
        });

        // Test invitation button (for dev)
        testButton.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailsActivity.this, InvitationResponseActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        // Initialize UI content
        populateUI();
        userInWaitlist();

        // Setup notification preference toggle
        setupNotificationToggle();
    }

    /**
     * US 01.04.03 | Opt out of notifications | Entrant
     * UI: Toggle button; Backend: Update preference
     */
    private void setupNotificationToggle() {
        DocumentReference userRef = UserSession.getCurrentUserRef();

        // Load saved preference from Firestore
        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Boolean notificationsEnabled = doc.getBoolean("notificationsEnabled");
                if (notificationsEnabled == null) notificationsEnabled = true;
                notificationSwitch.setChecked(notificationsEnabled);
            }
        });

        // Update preference when toggle changes
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userRef.update("notificationsEnabled", isChecked)
                    .addOnSuccessListener(aVoid -> Log.d("Notifications", "Preference updated: " + isChecked))
                    .addOnFailureListener(e -> Log.e("Notifications", "Failed to update preference", e));
        });
    }

    /**
     * Check if current user is in event waitlist
     */
    protected void userInWaitlist() {
        DocumentReference userRef = UserSession.getCurrentUserRef();

        db.collection("Events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    List<DocumentReference> waitlist = (List<DocumentReference>) documentSnapshot.get("Waitlist");
                    Button joinButton = findViewById(R.id.join_waitlist_button);
                    Button leaveButton = findViewById(R.id.leave_waitlist_button);

                    if (waitlist != null && waitlist.contains(userRef)) {
                        joinButton.setEnabled(false);
                        leaveButton.setEnabled(true);
                        joinButton.setVisibility(View.GONE);
                        leaveButton.setVisibility(View.VISIBLE);
                        joinButton.setAlpha(0.5f);
                        leaveButton.setAlpha(1f);
                    } else {
                        joinButton.setEnabled(true);
                        leaveButton.setEnabled(false);
                        joinButton.setVisibility(View.VISIBLE);
                        leaveButton.setVisibility(View.GONE);
                        joinButton.setAlpha(1f);
                        leaveButton.setAlpha(0.5f);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error checking waitlist", e));
    }

    /**
     * Populate event details in the UI
     */
    protected void populateUI() {
        if (eventId == null) return;

        db.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    String name = documentSnapshot.getString("Name");
                    String cost = documentSnapshot.getString("Cost");
                    DocumentReference organizer = documentSnapshot.getDocumentReference("Organizer");
                    String image = documentSnapshot.getString("Image");
                    String lotteryInfo = documentSnapshot.getString("LotteryInfo");

                    TextView nameView = findViewById(R.id.event_name);
                    TextView costView = findViewById(R.id.event_cost);
                    TextView organizerView = findViewById(R.id.event_organizer);
                    ImageView imageView = findViewById(R.id.event_poster);

                    nameView.setText(name);
                    costView.setText("Cost: " + cost);

                    if (organizer != null) populateOrganizer(organizer, organizerView);
                    if (image != null) populateImage(image, imageView);

                    displayLotteryCriteria(lotteryInfo);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading event details", e));
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
        if (base64Image == null || base64Image.isEmpty()) return;

        try {
            if (base64Image.startsWith("data:image")) {
                base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
            }
            byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
            Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e("EventDetails", "Image decode failed: " + e.getMessage());
        }
    }

    /**
     * US 01.05.05: Display lottery selection criteria to entrants
     */
    private void displayLotteryCriteria(String lotteryInfo) {
        if (lotteryInfo != null && !lotteryInfo.isEmpty()) {
            tvLotteryInfo.setText("Lottery Info: " + lotteryInfo);
        } else {
            tvLotteryInfo.setText("Lottery Info: Random selection. All entrants have equal chance.");
        }
        tvLotteryInfo.setVisibility(View.VISIBLE);
    }
}
