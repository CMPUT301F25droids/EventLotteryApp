package com.example.eventlotteryapp.EntrantView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.content.Intent;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.UserSession;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class EventDetailsActivity extends AppCompatActivity {
    private String eventId;
    private FirebaseFirestore db;

    private TextView tvLotteryInfo;

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
            JoinConfirmationFragment confirmation = new JoinConfirmationFragment().newInstance(eventId);
            ;
            confirmation.show(getSupportFragmentManager(), confirmation.getTag());
            userInWaitlist();
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

    }

    protected void userInWaitlist() {
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
                        Log.d("Firestore", "Waitlist from DB: " + waitlist);

                        if (waitlist != null && waitlist.contains(user_ref)) {
                            join_button.setText("Already in waitlist");
                            join_button.setEnabled(false);
                            join_button.setAlpha(0.5f);

                        } else {
                            join_button.setText("Join Waiting List");
                            join_button.setEnabled(true);
                            join_button.setAlpha(1f);

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