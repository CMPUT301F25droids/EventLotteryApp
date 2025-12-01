package com.example.eventlotteryapp.organizer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.eventlotteryapp.CsvExportController;
import com.example.eventlotteryapp.EventStatsController;
import com.example.eventlotteryapp.NotificationController;
import com.example.eventlotteryapp.R;
import com.example.eventlotteryapp.models.Entrant;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class OrganizerEventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrganizerEventDetails";
    
    private String eventId;
    private FirebaseFirestore firestore;
    private EventStatsController statsController;
    private NotificationController notificationController;
    private CsvExportController csvExportController;
    
    // UI Components
    private ImageView eventImage;
    private TextView eventTitle;
    private TextView eventDateRange;
    private TextView eventLocation;
    private TextView eventPrice;
    private TextView eventDescription;
    private TextView eventStatusTag;
    private TextView entrantsJoinedText;
    private TextView slotsAvailableText;
    private TextView daysLeftRegistrationText;
    private TextView lotteryDrawDateText;
    
    private Button manageWaitingListButton;
    private Button notifyEntrantsButton;
    private Button finalizedListButton;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    private SimpleDateFormat dateFormatWithYear = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_event_details);
        
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        firestore = FirebaseFirestore.getInstance();
        statsController = new EventStatsController();
        notificationController = new NotificationController();
        csvExportController = new CsvExportController();
        
        initializeViews();
        setupClickListeners();
        loadEventData();
    }
    
    private void initializeViews() {
        eventImage = findViewById(R.id.event_image);
        eventTitle = findViewById(R.id.event_title);
        eventDateRange = findViewById(R.id.event_date_range);
        eventLocation = findViewById(R.id.event_location);
        eventPrice = findViewById(R.id.event_price);
        eventDescription = findViewById(R.id.event_description);
        eventStatusTag = findViewById(R.id.event_status_tag);
        entrantsJoinedText = findViewById(R.id.entrants_joined_text);
        slotsAvailableText = findViewById(R.id.slots_available_text);
        daysLeftRegistrationText = findViewById(R.id.days_left_registration_text);
        lotteryDrawDateText = findViewById(R.id.lottery_draw_date_text);
        
        manageWaitingListButton = findViewById(R.id.manage_waiting_list_button);
        notifyEntrantsButton = findViewById(R.id.notify_entrants_button);
        finalizedListButton = findViewById(R.id.finalized_list_button);
        
        // Back button
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
        
        // Menu button (3 dots)
        ImageView menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> showMenu(v));
    }
    
    private void setupClickListeners() {
        manageWaitingListButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
        
        notifyEntrantsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotifyEntrantsActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
        
        finalizedListButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, FinalizedListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });
    }
    
    private void showMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.event_details_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_edit_event) {
                Intent intent = new Intent(this, CreateEventActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.menu_cancel_event) {
                showCancelEventDialog();
                return true;
            } else if (itemId == R.id.menu_share_event) {
                Intent intent = new Intent(this, ShareQrCodeActivity.class);
                intent.putExtra(ShareQrCodeActivity.EXTRA_EVENT_ID, eventId);
                startActivity(intent);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    private void loadEventData() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                populateUI(documentSnapshot);
                updateStatistics();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event", e);
                Toast.makeText(this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void populateUI(DocumentSnapshot document) {
        // Title
        String title = document.getString("title");
        if (title == null) title = document.getString("Name");
        if (title != null) {
            eventTitle.setText(title);
        }
        
        // Date range
        Date eventStartDate = document.getDate("eventStartDate");
        Date eventEndDate = document.getDate("eventEndDate");
        if (eventStartDate != null && eventEndDate != null) {
            String dateRange = dateFormat.format(eventStartDate) + " – " + dateFormatWithYear.format(eventEndDate);
            eventDateRange.setText(dateRange);
        } else {
            eventDateRange.setText("Date TBD");
        }
        
        // Location
        String location = document.getString("location");
        if (location != null && !location.isEmpty()) {
            eventLocation.setText(location);
        } else {
            eventLocation.setText("Location TBD");
        }
        
        // Price
        Double price = document.getDouble("price");
        if (price == null) {
            String costStr = document.getString("Cost");
            if (costStr != null && costStr.startsWith("$")) {
                try {
                    price = Double.parseDouble(costStr.substring(1));
                } catch (NumberFormatException e) {
                    price = 0.0;
                }
            }
        }
        if (price != null && price > 0) {
            eventPrice.setText(String.format(Locale.getDefault(), "$%.0f per participant", price));
        } else {
            eventPrice.setText("Free");
        }
        
        // Description
        String description = document.getString("description");
        if (description != null && !description.isEmpty()) {
            eventDescription.setText(description);
        } else {
            eventDescription.setText("No description available.");
        }
        
        // Image
        String imageBase64 = document.getString("Image");
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            populateImage(imageBase64, eventImage);
        }
    }
    
    private void populateImage(String base64Image, ImageView imageView) {
        try {
            // Remove the "data:image/jpeg;base64," or similar prefix
            if (base64Image.startsWith("data:image")) {
                base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
            }
            
            byte[] decodedBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode image", e);
        }
    }
    
    private void updateStatistics() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                // Entrants Joined (from waitingListEntrantIds)
                List<String> waitingList = (List<String>) document.get("waitingListEntrantIds");
                int entrantsJoined = (waitingList != null) ? waitingList.size() : 0;
                entrantsJoinedText.setText("Entrants Joined: " + entrantsJoined);
                
                // Slots Available (maxParticipants - acceptedEntrantIds count)
                // Use acceptedEntrantIds as the finalized count since those are users who accepted invitations
                Long maxParticipantsLong = document.getLong("maxParticipants");
                int maxParticipants = (maxParticipantsLong != null) ? maxParticipantsLong.intValue() : 0;
                
                List<String> acceptedEntrants = (List<String>) document.get("acceptedEntrantIds");
                int acceptedCount = (acceptedEntrants != null) ? acceptedEntrants.size() : 0;
                
                int slotsAvailable = Math.max(0, maxParticipants - acceptedCount);
                slotsAvailableText.setText("Slots Available: " + slotsAvailable);
                
                // Days Left in Registration
                Date registrationCloseDate = document.getDate("registrationCloseDate");
                Date registrationOpenDate = document.getDate("registrationOpenDate");
                Date now = new Date();
                
                if (registrationCloseDate != null) {
                    long diffInMillis = registrationCloseDate.getTime() - now.getTime();
                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
                    
                    if (diffInDays > 0) {
                        daysLeftRegistrationText.setText("Days Left: " + diffInDays);
                    } else if (diffInDays == 0) {
                        daysLeftRegistrationText.setText("Days Left: 0");
                    } else {
                        daysLeftRegistrationText.setText("Days Left: Closed");
                    }
                } else {
                    daysLeftRegistrationText.setText("Days Left: N/A");
                }
                
                // Lottery Draw Date (using registrationCloseDate)
                if (registrationCloseDate != null) {
                    String drawDate = dateFormatWithYear.format(registrationCloseDate);
                    lotteryDrawDateText.setText("Draw: " + drawDate);
                } else {
                    lotteryDrawDateText.setText("Draw: TBD");
                }
                
                // Update status tag
                boolean isOpen = true;
                if (registrationOpenDate != null && now.before(registrationOpenDate)) {
                    isOpen = false; // Registration not open yet
                } else if (registrationCloseDate != null && now.after(registrationCloseDate)) {
                    isOpen = false; // Registration closed
                }
                
                // Check if waiting list is limited
                Boolean limitWaitingList = document.getBoolean("limitWaitingList");
                boolean isLimitEnabled = (limitWaitingList != null && limitWaitingList);
                Long waitingListSizeLong = document.getLong("waitingListSize");
                int waitingListSize = (waitingListSizeLong != null) ? waitingListSizeLong.intValue() : 0;
                
                // Build status text - use waiting list size as denominator if limited, otherwise show just numerator
                // Numerator is the current waiting list count (entrantsJoined), denominator is waitingListSize
                String statusText;
                String countText;
                if (isLimitEnabled && waitingListSize > 0) {
                    // Waiting list is limited - show numerator/denominator
                    countText = entrantsJoined + "/" + waitingListSize;
                } else {
                    // Waiting list is infinite - show just numerator without slash
                    countText = String.valueOf(entrantsJoined);
                }
                
                if (isOpen) {
                    statusText = "🟢 Open - " + countText;
                } else {
                    statusText = "🔴 Closed - " + countText;
                }
                
                eventStatusTag.setText(statusText);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating statistics", e);
            });
    }
    
    private void runLottery() {
        Intent intent = new Intent(this, RunLotteryActivity.class);
        intent.putExtra("eventId", eventId);
        startActivity(intent);
    }
    
    private void showNotifyEntrantsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Notify Entrants")
            .setMessage("Send notification to which group?")
            .setPositiveButton("Waiting List", (dialog, which) -> {
                String title = "Event Update";
                String message = "There's an update regarding the event you're registered for.";
                notificationController.sendToWaitingList(eventId, title, message);
                Toast.makeText(this, "Notification sent to waiting list", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Selected Entrants", (dialog, which) -> {
                String title = "Event Update";
                String message = "There's an update regarding the event you've been selected for.";
                notificationController.sendToSelectedEntrants(eventId, title, message);
                Toast.makeText(this, "Notification sent to selected entrants", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    
    private void exportCsv() {
        firestore.collection("Events").document(eventId)
            .get()
            .addOnSuccessListener(document -> {
                // Export accepted entrants (finalized list) and selected entrants (pending acceptance)
                List<String> acceptedIds = (List<String>) document.get("acceptedEntrantIds");
                List<String> selectedIds = (List<String>) document.get("selectedEntrantIds");
                
                if (acceptedIds == null) acceptedIds = new ArrayList<>();
                if (selectedIds == null) selectedIds = new ArrayList<>();
                
                // Combine accepted and selected (remove duplicates)
                java.util.Set<String> allFinalizedIds = new java.util.LinkedHashSet<>();
                allFinalizedIds.addAll(acceptedIds);
                allFinalizedIds.addAll(selectedIds);
                List<String> finalizedIds = new ArrayList<>(allFinalizedIds);
                
                if (finalizedIds.isEmpty()) {
                    Toast.makeText(this, "No finalized entrants to export", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Determine event title - make it final for use in lambda
                String titleFromDoc = document.getString("title");
                if (titleFromDoc == null) titleFromDoc = document.getString("Name");
                final String eventTitle = (titleFromDoc != null) ? titleFromDoc : "Event";
                
                // Load all entrant details
                List<Entrant> entrants = new ArrayList<>();
                final int[] loaded = {0};
                
                for (String entrantId : finalizedIds) {
                    firestore.collection("users").document(entrantId)
                        .get()
                        .addOnSuccessListener(userDoc -> {
                            String name = userDoc.getString("Name");
                            if (name == null) name = userDoc.getString("name");
                            String email = userDoc.getString("email");
                            if (name != null && email != null) {
                                entrants.add(new Entrant(entrantId, name, email));
                            }
                            
                            loaded[0]++;
                            if (loaded[0] == finalizedIds.size()) {
                                // All loaded, export CSV
                                csvExportController.exportFinalListToCSV(this, eventTitle, entrants);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error loading entrant: " + entrantId, e);
                            loaded[0]++;
                            if (loaded[0] == finalizedIds.size()) {
                                csvExportController.exportFinalListToCSV(this, eventTitle, entrants);
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading event for export", e);
                Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showCancelEventDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Cancel Event")
            .setMessage("Are you sure you want to cancel this event? This action cannot be undone.")
            .setPositiveButton("Cancel Event", (dialog, which) -> {
                firestore.collection("Events").document(eventId)
                    .update("cancelled", true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Event cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error cancelling event", e);
                        Toast.makeText(this, "Error cancelling event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .setNegativeButton("Keep Event", null)
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to this activity
        if (eventId != null) {
            updateStatistics();
        }
    }
}

