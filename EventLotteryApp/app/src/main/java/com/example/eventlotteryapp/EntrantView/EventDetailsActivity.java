package com.example.eventlotteryapp.EntrantView;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.UserSession;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EventDetailsActivity extends AppCompatActivity {
    private String eventId;
    private FirebaseFirestore db;

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

        Button join_button = findViewById(R.id.join_waitlist_button);
        join_button.setOnClickListener(v -> {
            // Handle join button click
            JoinConfirmationFragment confirmation = new JoinConfirmationFragment().newInstance(eventId);;
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
            userInWaitlist();
        });

        Button leave_button = findViewById(R.id.leave_waitlist_button);
        leave_button.setOnClickListener(v -> {
            // Handle leave button click
            LeaveConfirmationFragment confirmation = new LeaveConfirmationFragment().newInstance(eventId);;
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
            userInWaitlist();
        });
        eventId = getIntent().getStringExtra("eventId");
        Log.d("EventDetails", "Event ID: " + eventId);
        db = FirebaseFirestore.getInstance();

        userInWaitlist();

        populateUI();

    }
    protected void userInWaitlist(){
        UserSession userSession = new UserSession();
        DocumentReference user_ref = UserSession.getCurrentUserRef();
        Log.d("Firestore", "Checking waitlist for eventId=" + eventId + ", userId=" + user_ref);

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
        else {
        }

    }
}