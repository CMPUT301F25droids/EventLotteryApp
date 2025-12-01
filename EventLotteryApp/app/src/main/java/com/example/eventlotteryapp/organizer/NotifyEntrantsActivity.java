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
 * Activity for notifying entrants (selected, waitlisted, or cancelled) about event updates.
 */
public class NotifyEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "NotifyEntrantsActivity";
    
    private static final int MODE_SELECTED = 0;
    private static final int MODE_WAITLISTED = 1;
    private static final int MODE_CANCELLED = 2;
    
    private String eventId;
    private FirebaseFirestore firestore;
    private NotificationController notificationController;
    
    private ImageView eventImage;
    private TextView eventTitle;
    private TextView participantCounts;
    private LinearLayout selectedTab;
    private LinearLayout waitlistedTab;
    private LinearLayout cancelledTab;
    private RadioButton selectedRadio;
    private RadioButton waitlistedRadio;
    private RadioButton cancelledRadio;
    private TextView selectedText;
    private TextView waitlistedText;
    private TextView cancelledText;
    private TextView messageLabel;
    private EditText messageEditText;
    private LinearLayout recipientsContainer;
    private LinearLayout recipientListHeader;
    private TextView moreRecipientsText;
    private Button sendNotificationsButton;
    
    private int currentMode = MODE_SELECTED;
    private List<String> selectedEntrantIds = new ArrayList<>();
    private List<String> waitlistedEntrantIds = new ArrayList<>();
    private List<String> cancelledEntrantIds = new ArrayList<>();

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
        cancelledTab = findViewById(R.id.cancelled_tab);
        selectedRadio = findViewById(R.id.selected_radio);
        waitlistedRadio = findViewById(R.id.waitlisted_radio);
        cancelledRadio = findViewById(R.id.cancelled_radio);
        selectedText = findViewById(R.id.selected_text);
        waitlistedText = findViewById(R.id.waitlisted_text);
        cancelledText = findViewById(R.id.cancelled_text);
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
        selectedTab.setOnClickListener(v -> setTab(MODE_SELECTED));
        waitlistedTab.setOnClickListener(v -> setTab(MODE_WAITLISTED));
        cancelledTab.setOnClickListener(v -> setTab(MODE_CANCELLED));
        
        selectedRadio.setOnClickListener(v -> setTab(MODE_SELECTED));
        waitlistedRadio.setOnClickListener(v -> setTab(MODE_WAITLISTED));
        cancelledRadio.setOnClickListener(v -> setTab(MODE_CANCELLED));
        
        sendNotificationsButton.setOnClickListener(v -> sendNotifications());
        
        recipientListHeader.setOnClickListener(v -> showAllRecipients());
    }
    
    private void setTab(int mode) {
        currentMode = mode;
        selectedRadio.setChecked(mode == MODE_SELECTED);
        waitlistedRadio.setChecked(mode == MODE_WAITLISTED);
        cancelledRadio.setChecked(mode == MODE_CANCELLED);
        
        // Reset all tabs to inactive state
        selectedText.setTextColor(getResources().getColor(R.color.black, null));
        selectedText.setTypeface(null, android.graphics.Typeface.NORMAL);
        selectedRadio.setButtonTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.medium_grey, null)));
        
        waitlistedText.setTextColor(getResources().getColor(R.color.black, null));
        waitlistedText.setTypeface(null, android.graphics.Typeface.NORMAL);
        waitlistedRadio.setButtonTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.medium_grey, null)));
        
        cancelledText.setTextColor(getResources().getColor(R.color.black, null));
        cancelledText.setTypeface(null, android.graphics.Typeface.NORMAL);
        cancelledRadio.setButtonTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.medium_grey, null)));
        
        // Set active tab
        if (mode == MODE_SELECTED) {
            selectedText.setTextColor(getResources().getColor(R.color.selected_tab_color, null));
            selectedText.setTypeface(null, android.graphics.Typeface.BOLD);
            selectedRadio.setButtonTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.selected_tab_color, null)));
            
            messageLabel.setText("Message to Selected Entrants");
            messageEditText.setHint("Congratulations! You've been selected to...");
        } else if (mode == MODE_WAITLISTED) {
            waitlistedText.setTextColor(getResources().getColor(R.color.selected_tab_color, null));
            waitlistedText.setTypeface(null, android.graphics.Typeface.BOLD);
            waitlistedRadio.setButtonTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.selected_tab_color, null)));
            
            messageLabel.setText("Message to Waitlisted Entrants");
            messageEditText.setHint("Thank you for your interest. The lottery draw has been completed...");
        } else if (mode == MODE_CANCELLED) {
            cancelledText.setTextColor(getResources().getColor(R.color.selected_tab_color, null));
            cancelledText.setTypeface(null, android.graphics.Typeface.BOLD);
            cancelledRadio.setButtonTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.selected_tab_color, null)));
            
            messageLabel.setText("Message to Cancelled Entrants");
            messageEditText.setHint("We wanted to inform you about an update regarding your cancelled registration...");
        }
        
        updateRecipients();
    }
    
    private void loadEventData() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                // Load event image
                String imageBase64 = document.getString("Image");
                if (imageBase64 != null && !imageBase64.isEmpty()) {
                    populateImage(imageBase64, eventImage);
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
                cancelledEntrantIds = (List<String>) document.get("cancelledEntrantIds");
                
                if (selectedEntrantIds == null) selectedEntrantIds = new ArrayList<>();
                if (waitlistedEntrantIds == null) waitlistedEntrantIds = new ArrayList<>();
                if (cancelledEntrantIds == null) cancelledEntrantIds = new ArrayList<>();
                
                // Update participant counts
                int selectedCount = selectedEntrantIds.size();
                int waitlistedCount = waitlistedEntrantIds.size();
                int cancelledCount = cancelledEntrantIds.size();
                participantCounts.setText("Selected: " + selectedCount + ", Waitlisted: " + waitlistedCount + ", Cancelled: " + cancelledCount);
                
                // Update recipients
                updateRecipients();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event data", e);
                Toast.makeText(this, "Error loading event data", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void populateImage(String base64Image, ImageView imageView) {
        try {
            // Remove the "data:image/jpeg;base64," or similar prefix
            if (base64Image.startsWith("data:image")) {
                base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
            }
            
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode image", e);
        }
    }
    
    private void updateRecipients() {
        recipientsContainer.removeAllViews();
        
        List<String> currentList;
        if (currentMode == MODE_SELECTED) {
            currentList = selectedEntrantIds;
        } else if (currentMode == MODE_WAITLISTED) {
            currentList = waitlistedEntrantIds;
        } else {
            currentList = cancelledEntrantIds;
        }
        
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
        
        firestore.collection("users").document(entrantId)
            .get()
            .addOnSuccessListener(document -> {
                if (!document.exists()) {
                    return;
                }
                
                String name = document.getString("name");
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
        
        List<String> targetList;
        String title;
        
        if (currentMode == MODE_SELECTED) {
            targetList = selectedEntrantIds;
            title = "Lottery Selection";
        } else if (currentMode == MODE_WAITLISTED) {
            targetList = waitlistedEntrantIds;
            title = "Lottery Results";
        } else {
            targetList = cancelledEntrantIds;
            title = "Event Update";
        }
        
        if (targetList.isEmpty()) {
            Toast.makeText(this, "No recipients to notify", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentMode == MODE_SELECTED) {
            notificationController.sendToSelectedEntrants(eventId, title, message);
        } else if (currentMode == MODE_WAITLISTED) {
            notificationController.sendToWaitingList(eventId, title, message);
        } else {
            notificationController.sendToCancelledEntrants(eventId, title, message);
        }
        
        Toast.makeText(this, "Notifications sent to " + targetList.size() + " recipient(s)", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void showAllRecipients() {
        List<String> currentList;
        if (currentMode == MODE_SELECTED) {
            currentList = selectedEntrantIds;
        } else if (currentMode == MODE_WAITLISTED) {
            currentList = waitlistedEntrantIds;
        } else {
            currentList = cancelledEntrantIds;
        }
        
        if (currentList == null || currentList.isEmpty()) {
            Toast.makeText(this, "No recipients to display", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Load all recipient names
        final List<String> recipientNames = new ArrayList<>();
        final int[] loaded = {0};
        final int total = currentList.size();
        
        for (String entrantId : currentList) {
            firestore.collection("users").document(entrantId)
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
        String title;
        if (currentMode == MODE_SELECTED) {
            title = "Selected Entrants (" + recipientNames.size() + ")";
        } else if (currentMode == MODE_WAITLISTED) {
            title = "Waitlisted Entrants (" + recipientNames.size() + ")";
        } else {
            title = "Cancelled Entrants (" + recipientNames.size() + ")";
        }
        
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

