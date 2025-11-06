package com.example.eventlotteryapp.EntrantView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.UserSession;
import com.example.eventlotteryapp.Controllers.LotteryController;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity for responding to lottery invitations
 * Supports:
 * - US 01.05.02: Accept invitation
 * - US 01.05.03: Decline invitation
 * - US 01.05.05: Display lottery criteria
 * @author Rayyan
 */
public class InvitationResponseActivity extends AppCompatActivity {
    private static final String TAG = "InvitationResponse";

    private String eventId;
    private String eventName;
    private FirebaseFirestore db;
    private LotteryController lotteryController;

    private TextView tvEventName;
    private TextView tvMessage;
    private TextView tvLotteryCriteria;
    private Button btnAccept;
    private Button btnDecline;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitation_response);

        // Get event ID from intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Error: No event specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        lotteryController = new LotteryController();

        initializeViews();
        loadEventDetails();
        setupButtons();
    }

    private void initializeViews() {
        tvEventName = findViewById(R.id.tv_event_name);
        tvMessage = findViewById(R.id.tv_invitation_message);
        tvLotteryCriteria = findViewById(R.id.tv_lottery_criteria);
        btnAccept = findViewById(R.id.btn_accept_invitation);
        btnDecline = findViewById(R.id.btn_decline_invitation);
        progressBar = findViewById(R.id.progress_bar);
    }

    /**
     * Load event details including lottery criteria (US 01.05.05)
     */
    private void loadEventDetails() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("Events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        eventName = documentSnapshot.getString("Name");
                        String lotteryInfo = documentSnapshot.getString("LotteryInfo");

                        tvEventName.setText(eventName);
                        tvMessage.setText("Congratulations! You've been selected for " + eventName + "!");

                        // US 01.05.05: Display lottery criteria
                        if (lotteryInfo != null && !lotteryInfo.isEmpty()) {
                            tvLotteryCriteria.setText("Selection Process: " + lotteryInfo);
                            tvLotteryCriteria.setVisibility(View.VISIBLE);
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading event details", e);
                    Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void setupButtons() {
        // US 01.05.02: Accept invitation button
        btnAccept.setOnClickListener(v -> acceptInvitation());

        // US 01.05.03: Decline invitation button
        btnDecline.setOnClickListener(v -> declineInvitation());
    }

    /**
     * US 01.05.02: Handle invitation acceptance
     */
    private void acceptInvitation() {
        setButtonsEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        String userId = UserSession.getCurrentUserId();

        lotteryController.acceptInvitation(eventId, userId, new LotteryController.AcceptCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(InvitationResponseActivity.this,
                            "Successfully accepted! You're registered for " + eventName,
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setButtonsEnabled(true);
                    Toast.makeText(InvitationResponseActivity.this,
                            "Error accepting invitation: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * US 01.05.03: Handle invitation decline
     */
    private void declineInvitation() {
        setButtonsEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        String userId = UserSession.getCurrentUserId();

        lotteryController.declineInvitation(eventId, userId, new LotteryController.DeclineCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(InvitationResponseActivity.this,
                            "Invitation declined. Thank you for your response.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setButtonsEnabled(true);
                    Toast.makeText(InvitationResponseActivity.this,
                            "Error declining invitation: " + error,
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnAccept.setEnabled(enabled);
        btnDecline.setEnabled(enabled);
    }
}