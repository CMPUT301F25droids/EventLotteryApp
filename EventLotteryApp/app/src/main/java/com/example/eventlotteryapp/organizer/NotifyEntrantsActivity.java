package com.example.eventlotteryapp.organizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.eventlotteryapp.NotificationController;
import com.example.eventlotteryapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for notifying entrants (selected or waitlisted) about event updates.
 */
public class NotifyEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "NotifyEntrantsActivity";
    
    private String eventId;
    private FirebaseFirestore firestore;
    private NotificationController notificationController;
    
    private ImageView eventImage;
    private TextView eventTitle;
    private TextView participantCounts;
    private LinearLayout selectedTab;
    private LinearLayout waitlistedTab;
    private RadioButton selectedRadio;
    private RadioButton waitlistedRadio;
    private TextView messageLabel;
    private EditText messageEditText;
    private LinearLayout recipientsContainer;
    private LinearLayout recipientListHeader;
    private TextView moreRecipientsText;
    private Button sendNotificationsButton;
    
    private boolean isSelectedMode = true;
    private List<String> selectedEntrantIds = new ArrayList<>();
    private List<String> waitlistedEntrantIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notify_entrants);
        
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        firestore = FirebaseFirestore.getInstance();
        notificationController = new NotificationController();
        
        initializeViews();
        setupClickListeners();
        loadEventData();
    }
    
    private void initializeViews() {
        eventImage = findViewById(R.id.event_image);
        eventTitle = findViewById(R.id.event_title);
        participantCounts = findViewById(R.id.participant_counts);
        selectedTab = findViewById(R.id.selected_tab);
        waitlistedTab = findViewById(R.id.waitlisted_tab);
        selectedRadio = findViewById(R.id.selected_radio);
        waitlistedRadio = findViewById(R.id.waitlisted_radio);
        messageLabel = findViewById(R.id.message_label);
        messageEditText = findViewById(R.id.message_edit_text);
        recipientsContainer = findViewById(R.id.recipients_container);
        recipientListHeader = findViewById(R.id.recipient_list_header);
        moreRecipientsText = findViewById(R.id.more_recipients_text);
        sendNotificationsButton = findViewById(R.id.send_notifications_button);
        
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
                    Intent intent = new Intent(NotifyEntrantsActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else if (position == 1) {
                    Intent intent = new Intent(NotifyEntrantsActivity.this, CreateEventActivity.class);
                    startActivity(intent);
                    finish();
                } else if (position == 2) {
                    Intent intent = new Intent(NotifyEntrantsActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
                    intent.putExtra("tab", "notifications");
                    startActivity(intent);
                    finish();
                } else if (position == 3) {
                    Intent intent = new Intent(NotifyEntrantsActivity.this, com.example.eventlotteryapp.OrganizerHomePage.class);
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
        selectedTab.setOnClickListener(v -> setTab(true));
        waitlistedTab.setOnClickListener(v -> setTab(false));
        
        selectedRadio.setOnClickListener(v -> setTab(true));
        waitlistedRadio.setOnClickListener(v -> setTab(false));
        
        sendNotificationsButton.setOnClickListener(v -> sendNotifications());
        
        recipientListHeader.setOnClickListener(v -> showAllRecipients());
    }
    
    private void setTab(boolean selected) {
        isSelectedMode = selected;
        selectedRadio.setChecked(selected);
        waitlistedRadio.setChecked(!selected);
        
        // Update tab styling
        TextView selectedText = null;
        TextView waitlistedText = null;
        
        for (int i = 0; i < selectedTab.getChildCount(); i++) {
            if (selectedTab.getChildAt(i) instanceof TextView) {
                selectedText = (TextView) selectedTab.getChildAt(i);
                break;
            }
        }
        
        for (int i = 0; i < waitlistedTab.getChildCount(); i++) {
            if (waitlistedTab.getChildAt(i) instanceof TextView) {
                waitlistedText = (TextView) waitlistedTab.getChildAt(i);
                break;
            }
        }
        
        if (selected) {
            if (selectedText != null) {
                selectedText.setTextColor(getResources().getColor(R.color.selected_tab_color, null));
                selectedText.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            if (waitlistedText != null) {
                waitlistedText.setTextColor(getResources().getColor(R.color.black, null));
                waitlistedText.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
            
            messageLabel.setText("Message to Selected Entrants");
            messageEditText.setHint("Congratulations! You've been selected to...");
        } else {
            if (waitlistedText != null) {
                waitlistedText.setTextColor(getResources().getColor(R.color.selected_tab_color, null));
                waitlistedText.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            if (selectedText != null) {
                selectedText.setTextColor(getResources().getColor(R.color.black, null));
                selectedText.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
            
            messageLabel.setText("Message to Waitlisted Entrants");
            messageEditText.setHint("Thank you for your interest. The lottery draw has been completed...");
        }
        
        updateRecipients();
    }
    
    private void loadEventData() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                // Load event image
                String posterImageBase64 = document.getString("posterImageBase64");
                if (posterImageBase64 != null && !posterImageBase64.isEmpty()) {
                    try {
                        byte[] decodedBytes = Base64.decode(posterImageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        eventImage.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding event image", e);
                    }
                }
                
                // Load event title
                String title = document.getString("title");
                if (title == null) title = document.getString("Name");
                if (title != null) {
                    eventTitle.setText(title);
                }
                
                // Load entrant lists
                selectedEntrantIds = (List<String>) document.get("selectedEntrantIds");
                waitlistedEntrantIds = (List<String>) document.get("waitingListEntrantIds");
                
                if (selectedEntrantIds == null) selectedEntrantIds = new ArrayList<>();
                if (waitlistedEntrantIds == null) waitlistedEntrantIds = new ArrayList<>();
                
                // Update participant counts
                int selectedCount = selectedEntrantIds.size();
                int waitlistedCount = waitlistedEntrantIds.size();
                participantCounts.setText("Selected: " + selectedCount + ", Waitlisted: " + waitlistedCount);
                
                // Update recipients
                updateRecipients();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event data", e);
                Toast.makeText(this, "Error loading event data", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void updateRecipients() {
        recipientsContainer.removeAllViews();
        
        List<String> currentList = isSelectedMode ? selectedEntrantIds : waitlistedEntrantIds;
        int maxVisible = 4;
        int total = currentList.size();
        
        if (total == 0) {
            TextView noRecipients = new TextView(this);
            noRecipients.setText("No recipients");
            noRecipients.setTextColor(getResources().getColor(R.color.medium_grey, null));
            noRecipients.setPadding(16, 16, 16, 16);
            recipientsContainer.addView(noRecipients);
            moreRecipientsText.setVisibility(View.GONE);
            return;
        }
        
        int visibleCount = Math.min(maxVisible, total);
        int moreCount = total - visibleCount;
        
        if (moreCount > 0) {
            moreRecipientsText.setText("+" + moreCount + " more");
            moreRecipientsText.setVisibility(View.VISIBLE);
        } else {
            moreRecipientsText.setVisibility(View.GONE);
        }
        
        // Load and display recipient avatars
        for (int i = 0; i < visibleCount; i++) {
            String entrantId = currentList.get(i);
            loadRecipientAvatar(entrantId, i == visibleCount - 1);
        }
    }
    
    private void loadRecipientAvatar(String entrantId, boolean isLast) {
        if (entrantId == null || entrantId.isEmpty()) {
            return;
        }
        
        firestore.collection("Users").document(entrantId)
            .get()
            .addOnSuccessListener(document -> {
                if (!document.exists()) {
                    return;
                }
                
                String name = document.getString("Name");
                if (name == null) name = "Unknown";
                
                try {
                    // Create avatar view
                    View avatarView = LayoutInflater.from(this).inflate(R.layout.item_recipient_avatar, recipientsContainer, false);
                    ImageView avatar = avatarView.findViewById(R.id.avatar_image);
                    TextView nameText = avatarView.findViewById(R.id.avatar_name);
                    
                    if (avatar != null && nameText != null) {
                        nameText.setText(name);
                        
                        // Set default avatar (you can load actual profile images here if available)
                        avatar.setImageResource(R.drawable.ic_profile);
                        avatar.setBackgroundResource(R.drawable.circular_background);
                        
                        if (!isLast) {
                            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) avatarView.getLayoutParams();
                            if (params != null) {
                                params.setMarginEnd(8);
                                avatarView.setLayoutParams(params);
                            }
                        }
                        
                        recipientsContainer.addView(avatarView);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating avatar view", e);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading recipient: " + entrantId, e);
            });
    }
    
    private void sendNotifications() {
        String message = messageEditText.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<String> targetList = isSelectedMode ? selectedEntrantIds : waitlistedEntrantIds;
        if (targetList.isEmpty()) {
            Toast.makeText(this, "No recipients to notify", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String title = isSelectedMode ? "Lottery Selection" : "Lottery Results";
        
        if (isSelectedMode) {
            notificationController.sendToSelectedEntrants(eventId, title, message);
        } else {
            notificationController.sendToWaitingList(eventId, title, message);
        }
        
        Toast.makeText(this, "Notifications sent to " + targetList.size() + " recipient(s)", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void showAllRecipients() {
        List<String> currentList = isSelectedMode ? selectedEntrantIds : waitlistedEntrantIds;
        
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(this, "No recipients to display", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Load all recipient names
        final List<String> recipientNames = new ArrayList<>();
        final int[] loaded = {0};
        final int total = currentList.size();
        
        for (String entrantId : currentList) {
            firestore.collection("Users").document(entrantId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("Name");
                        String email = document.getString("email");
                        if (name != null) {
                            recipientNames.add(name + (email != null ? " (" + email + ")" : ""));
                        } else if (email != null) {
                            recipientNames.add(email);
                        } else {
                            recipientNames.add("Unknown");
                        }
                    } else {
                        recipientNames.add("Unknown");
                    }
                    
                    loaded[0]++;
                    if (loaded[0] == total) {
                        // All loaded, show dialog
                        showRecipientsDialog(recipientNames);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading recipient: " + entrantId, e);
                    recipientNames.add("Error loading");
                    loaded[0]++;
                    if (loaded[0] == total) {
                        showRecipientsDialog(recipientNames);
                    }
                });
        }
    }
    
    private void showRecipientsDialog(List<String> recipientNames) {
        String title = isSelectedMode ? "Selected Entrants (" + recipientNames.size() + ")" : "Waitlisted Entrants (" + recipientNames.size() + ")";
        
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < recipientNames.size(); i++) {
            message.append((i + 1)).append(". ").append(recipientNames.get(i));
            if (i < recipientNames.size() - 1) {
                message.append("\n");
            }
        }
        
        new android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message.toString())
            .setPositiveButton("OK", null)
            .show();
    }
}

