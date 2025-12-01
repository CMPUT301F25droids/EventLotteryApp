package com.example.eventlotteryapp.Admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Activity that allows administrators to review logs of all notifications
 * sent to entrants by organizers.
 * US 03.08.01: As an administrator, I want to review logs of all notifications
 * sent to entrants by organizers.
 */
public class AdminNotificationLogsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;

    private AdminNotificationLogAdapter adapter;

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final CollectionReference notificationsRef = firestore.collection("Notifications");

    /**
     * Initializes UI components, sets up the RecyclerView adapter,
     * and loads all notification logs from Firestore.
     *
     * @param savedInstanceState Previous activity state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_notification_logs);

        // Back button
        android.widget.ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerAdminNotificationLogs);
        progressBar = findViewById(R.id.adminNotificationLogsProgress);
        emptyView = findViewById(R.id.adminNotificationLogsEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminNotificationLogAdapter();
        recyclerView.setAdapter(adapter);

        loadNotificationLogs();
    }

    /**
     * Loads all notification logs sent to entrants (UserType == "entrant")
     * that were sent by organizers. Organizer-sent notifications always have a Title field.
     * Displays a loading indicator while the data is being fetched.
     */
    private void loadNotificationLogs() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        // Query ALL notifications where UserType is "entrant"
        // Filter for those with Title (organizer-sent ones always have Title)
        // This includes both old and new notifications
        // Use source CACHE to ensure we get fresh data
        notificationsRef
                .whereEqualTo("UserType", "entrant")
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnSuccessListener(query -> {
                    android.util.Log.d("AdminNotificationLogs", "Found " + query.size() + " entrant notifications");
                    
                    // Log all notifications for debugging
                    for (QueryDocumentSnapshot doc : query) {
                        android.util.Log.d("AdminNotificationLogs", "Notification: " + doc.getId() + 
                                " - Title: " + doc.getString("Title") + 
                                ", Type: " + doc.getString("Type") + 
                                ", OrganizerId: " + doc.getString("OrganizerId") +
                                ", UserId: " + doc.getString("UserId") +
                                ", TimeStamp: " + doc.get("TimeStamp"));
                    }
                    
                    List<AdminNotificationLogAdapter.NotificationLog> pendingLogs = new ArrayList<>();
                    final java.util.concurrent.atomic.AtomicInteger processedCount = new java.util.concurrent.atomic.AtomicInteger(0);
                    final java.util.concurrent.atomic.AtomicInteger totalToProcess = new java.util.concurrent.atomic.AtomicInteger(0);

                    // First, count how many we'll actually process
                    for (QueryDocumentSnapshot doc : query) {
                        String title = doc.getString("Title");
                        String type = doc.getString("Type");
                        String organizerId = doc.getString("OrganizerId");
                        String userId = doc.getString("UserId");
                        
                        boolean hasTitle = title != null && !title.isEmpty();
                        boolean isMessage = "MESSAGE".equals(type);
                        boolean hasOrganizerId = organizerId != null && !organizerId.isEmpty();
                        boolean hasUserId = userId != null && !userId.isEmpty();
                        
                        boolean matches = hasTitle || isMessage || hasOrganizerId;
                        
                        android.util.Log.d("AdminNotificationLogs", "Checking notification " + doc.getId() + 
                                " - hasTitle: " + hasTitle + ", isMessage: " + isMessage + 
                                ", hasOrganizerId: " + hasOrganizerId + ", hasUserId: " + hasUserId + 
                                ", matches: " + matches);
                        
                        // Only count if it matches our criteria AND has a valid userId
                        if (hasUserId && matches) {
                            totalToProcess.incrementAndGet();
                            android.util.Log.d("AdminNotificationLogs", "Including notification " + doc.getId());
                        } else {
                            android.util.Log.d("AdminNotificationLogs", "Excluding notification " + doc.getId() + 
                                    " - hasUserId: " + hasUserId + ", matches: " + matches);
                        }
                    }
                    
                    final int finalTotal = totalToProcess.get();
                    android.util.Log.d("AdminNotificationLogs", "Will process " + finalTotal + " notifications out of " + query.size() + " total");
                    
                    if (finalTotal == 0) {
                        progressBar.setVisibility(View.GONE);
                        emptyView.setText("No notification logs found. Organizers need to send notifications to entrants first.");
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Now process each notification
                    for (QueryDocumentSnapshot doc : query) {
                        // Create final copies for use in lambdas
                        final String notificationId = doc.getId();
                        final String message = doc.getString("Message");
                        final String title = doc.getString("Title");
                        final String userId = doc.getString("UserId");
                        final String organizerId = doc.getString("OrganizerId");
                        final String type = doc.getString("Type");
                        
                        // Get event reference
                        com.google.firebase.firestore.DocumentReference eventRef = doc.getDocumentReference("EventId");
                        final String eventId = eventRef != null ? eventRef.getId() : null;
                        
                        android.util.Log.d("AdminNotificationLogs", "Processing notification - userId: " + userId + 
                                ", organizerId: " + organizerId + ", eventId: " + eventId);
                        
                        // Skip if not organizer-sent
                        if ((title == null || title.isEmpty()) && !"MESSAGE".equals(type) && 
                            (organizerId == null || organizerId.isEmpty())) {
                            continue;
                        }
                        
                        // Skip if userId is null or empty (invalid notification)
                        if (userId == null || userId.isEmpty()) {
                            android.util.Log.w("AdminNotificationLogs", "Skipping notification with null/empty userId");
                            continue;
                        }
                        
                        // Handle timestamp - create final copy
                        Object timestampObj = doc.get("TimeStamp");
                        java.util.Date timestampValue;
                        if (timestampObj instanceof java.util.Date) {
                            timestampValue = (java.util.Date) timestampObj;
                        } else if (timestampObj instanceof com.google.firebase.Timestamp) {
                            timestampValue = ((com.google.firebase.Timestamp) timestampObj).toDate();
                        } else if (timestampObj instanceof String) {
                            try {
                                timestampValue = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.US).parse((String) timestampObj);
                            } catch (Exception e) {
                                timestampValue = new java.util.Date();
                            }
                        } else {
                            timestampValue = new java.util.Date();
                        }
                        final java.util.Date timestamp = timestampValue;
                        
                        // Process the notification entry directly
                        // Note: Organizers can receive notifications in entrant mode, so we don't filter by role
                        processNotificationEntry(notificationId, message, title, userId, organizerId, 
                                type, eventId, timestamp, pendingLogs, processedCount, finalTotal);
                    }
                    // Note: finishProcessing will be called by processNotificationEntry when all are done
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminNotificationLogs", "Error loading notifications", e);
                    progressBar.setVisibility(View.GONE);
                    emptyView.setText("Error loading notification logs: " + e.getMessage());
                    emptyView.setVisibility(View.VISIBLE);
                });
    }

    /**
     * Processes a notification entry after verifying the recipient is an entrant.
     */
    private void processNotificationEntry(String notificationId, String message, String title, String userId,
                                         String organizerId, String type, String eventId, java.util.Date timestamp,
                                         List<AdminNotificationLogAdapter.NotificationLog> pendingLogs,
                                         java.util.concurrent.atomic.AtomicInteger processedCount, int totalToProcess) {
        // If organizerId is missing, try to get it from the event
        if (organizerId == null || organizerId.isEmpty()) {
            if (eventId != null) {
                firestore.collection("Events").document(eventId)
                        .get()
                        .addOnSuccessListener(eventDoc -> {
                            String extractedOrganizerId = extractOrganizerIdFromEvent(eventDoc);
                            synchronized (pendingLogs) {
                                pendingLogs.add(new AdminNotificationLogAdapter.NotificationLog(
                                        notificationId,
                                        title != null ? title : "No Title",
                                        message != null ? message : "No Message",
                                        userId,
                                        extractedOrganizerId,
                                        eventId,
                                        timestamp
                                ));
                                if (processedCount.incrementAndGet() == totalToProcess) {
                                    finishProcessing(pendingLogs);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            // If event lookup fails, still add the log without organizerId
                            synchronized (pendingLogs) {
                                pendingLogs.add(new AdminNotificationLogAdapter.NotificationLog(
                                        notificationId,
                                        title != null ? title : "No Title",
                                        message != null ? message : "No Message",
                                        userId,
                                        null,
                                        eventId,
                                        timestamp
                                ));
                                if (processedCount.incrementAndGet() == totalToProcess) {
                                    finishProcessing(pendingLogs);
                                }
                            }
                        });
            } else {
                // No eventId, add without organizerId
                synchronized (pendingLogs) {
                    pendingLogs.add(new AdminNotificationLogAdapter.NotificationLog(
                            notificationId,
                            title != null ? title : "No Title",
                            message != null ? message : "No Message",
                            userId,
                            null,
                            null,
                            timestamp
                    ));
                    if (processedCount.incrementAndGet() == totalToProcess) {
                        finishProcessing(pendingLogs);
                    }
                }
            }
        } else {
            // Has organizerId, add directly
            synchronized (pendingLogs) {
                pendingLogs.add(new AdminNotificationLogAdapter.NotificationLog(
                        notificationId,
                        title != null ? title : "No Title",
                        message != null ? message : "No Message",
                        userId,
                        organizerId,
                        eventId,
                        timestamp
                ));
                if (processedCount.incrementAndGet() == totalToProcess) {
                    finishProcessing(pendingLogs);
                }
            }
        }
    }

    /**
     * Extracts organizerId from an event document.
     * Handles both DocumentReference ("Organizer") and string ("organizerId") formats.
     */
    private String extractOrganizerIdFromEvent(com.google.firebase.firestore.DocumentSnapshot eventDoc) {
        if (eventDoc == null || !eventDoc.exists()) {
            return null;
        }
        
        // Try to get organizerId as a string first
        String organizerId = eventDoc.getString("organizerId");
        
        // If not found, try to get it from DocumentReference
        if (organizerId == null || organizerId.isEmpty()) {
            com.google.firebase.firestore.DocumentReference organizerRef = eventDoc.getDocumentReference("Organizer");
            if (organizerRef != null) {
                // Extract organizer ID from DocumentReference path
                String path = organizerRef.getPath();
                if (path != null && path.contains("/users/")) {
                    organizerId = path.substring(path.lastIndexOf("/") + 1);
                }
            }
        }
        
        return organizerId;
    }

    /**
     * Finishes processing the notification logs by sorting and updating the UI.
     */
    private void finishProcessing(List<AdminNotificationLogAdapter.NotificationLog> logs) {
        // Sort by timestamp descending (newest first)
        Collections.sort(logs, new Comparator<AdminNotificationLogAdapter.NotificationLog>() {
            @Override
            public int compare(AdminNotificationLogAdapter.NotificationLog o1, AdminNotificationLogAdapter.NotificationLog o2) {
                // Compare in reverse order (descending)
                return o2.timestamp.compareTo(o1.timestamp);
            }
        });

        progressBar.setVisibility(View.GONE);

        if (logs.isEmpty()) {
            emptyView.setText("No notification logs found");
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            adapter.updateLogs(logs);
        }
    }
}

