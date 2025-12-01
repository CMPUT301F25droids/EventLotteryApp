package com.example.eventlotteryapp.Admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventlotteryapp.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying notification logs in the admin panel.
 * Shows notifications sent to entrants by organizers.
 */
public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.NotificationLogViewHolder> {

    /**
     * Local model representing one notification log entry.
     */
    public static class NotificationLog {
        public final String notificationId;
        public final String title;
        public final String message;
        public final String recipientUserId;
        public final String organizerId;
        public final String eventId;
        public final Date timestamp;

        public NotificationLog(String notificationId, String title, String message,
                              String recipientUserId, String organizerId, String eventId, Date timestamp) {
            this.notificationId = notificationId;
            this.title = title;
            this.message = message;
            this.recipientUserId = recipientUserId;
            this.organizerId = organizerId;
            this.eventId = eventId;
            this.timestamp = timestamp;
        }
    }

    private final List<NotificationLog> logs = new ArrayList<>();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());

    /**
     * Extracts organizerId from an event document.
     * Handles both DocumentReference ("Organizer") and string ("organizerId") formats.
     */
    private String extractOrganizerIdFromEvent(com.google.firebase.firestore.DocumentSnapshot eventDoc) {
        if (eventDoc == null || !eventDoc.exists()) {
            android.util.Log.w("AdminNotificationLogAdapter", "Event document is null or doesn't exist");
            return null;
        }
        
        // Log all fields in the event document for debugging
        android.util.Log.d("AdminNotificationLogAdapter", "Event document fields: " + eventDoc.getData().keySet());
        
        // Try to get organizerId as a string first
        String organizerId = eventDoc.getString("organizerId");
        android.util.Log.d("AdminNotificationLogAdapter", "organizerId field: " + organizerId);
        
        // If not found, try to get it from DocumentReference
        if (organizerId == null || organizerId.isEmpty()) {
            com.google.firebase.firestore.DocumentReference organizerRef = eventDoc.getDocumentReference("Organizer");
            android.util.Log.d("AdminNotificationLogAdapter", "Organizer DocumentReference: " + organizerRef);
            
            if (organizerRef != null) {
                // Extract organizer ID from DocumentReference path
                String path = organizerRef.getPath();
                android.util.Log.d("AdminNotificationLogAdapter", "Organizer path: " + path);
                
                if (path != null && path.contains("/users/")) {
                    organizerId = path.substring(path.lastIndexOf("/") + 1);
                    android.util.Log.d("AdminNotificationLogAdapter", "Extracted organizerId from path: " + organizerId);
                } else if (path != null) {
                    // Try to extract from any path format
                    String[] parts = path.split("/");
                    for (int i = 0; i < parts.length; i++) {
                        if ("users".equals(parts[i]) && i + 1 < parts.length) {
                            organizerId = parts[i + 1];
                            android.util.Log.d("AdminNotificationLogAdapter", "Extracted organizerId from path parts: " + organizerId);
                            break;
                        }
                    }
                }
            }
        }
        
        android.util.Log.d("AdminNotificationLogAdapter", "Final extracted organizerId: " + organizerId);
        return organizerId;
    }

    /**
     * Updates the list of notification logs and refreshes the RecyclerView.
     *
     * @param newLogs The new list of notification logs.
     */
    public void updateLogs(List<NotificationLog> newLogs) {
        logs.clear();
        logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification_log, parent, false);
        return new NotificationLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationLogViewHolder holder, int position) {
        NotificationLog log = logs.get(position);
        
        // Store position to prevent RecyclerView recycling issues
        holder.position = position;
        
        // Set basic info
        holder.titleText.setText(log.title);
        holder.messageText.setText(log.message);
        holder.timestampText.setText(dateFormat.format(log.timestamp));
        
        // Initialize loading state
        holder.organizerNameText.setText("Loading...");
        holder.recipientNameText.setText("Loading...");
        holder.eventNameText.setText("Loading...");
        
        android.util.Log.d("AdminNotificationLogAdapter", "Loading notification at position " + position + 
                " - organizerId: " + log.organizerId + ", recipientId: " + log.recipientUserId + ", eventId: " + log.eventId);
        
        // Load organizer name
        if (log.organizerId != null && !log.organizerId.isEmpty()) {
            firestore.collection("users").document(log.organizerId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        // Check if this ViewHolder is still bound to the same position
                        if (holder.position != position) {
                            android.util.Log.d("AdminNotificationLogAdapter", "ViewHolder recycled, skipping update for position " + position);
                            return;
                        }
                        
                        if (doc.exists()) {
                            // Try "Name" first (as seen in EntrantListActivity), then "name"
                            String name = doc.getString("Name");
                            if (name == null || name.isEmpty()) name = doc.getString("name");
                            String email = doc.getString("email");
                            if (email == null || email.isEmpty()) email = doc.getString("Email");
                            
                            android.util.Log.d("AdminNotificationLogAdapter", "Organizer found - name: " + name + ", email: " + email);
                            
                            String displayName = (name != null && !name.isEmpty()) ? name : 
                                                (email != null && !email.isEmpty() ? email : "Unknown Organizer");
                            holder.organizerNameText.setText("From: " + displayName);
                        } else {
                            android.util.Log.w("AdminNotificationLogAdapter", "Organizer document not found: " + log.organizerId);
                            holder.organizerNameText.setText("From: Unknown Organizer");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (holder.position != position) return;
                        android.util.Log.e("AdminNotificationLogAdapter", "Error loading organizer: " + log.organizerId, e);
                        holder.organizerNameText.setText("From: Error loading");
                    });
        } else if (log.eventId != null && !log.eventId.isEmpty()) {
            // Try to get organizerId from event if it's missing
            firestore.collection("Events").document(log.eventId)
                    .get()
                    .addOnSuccessListener(eventDoc -> {
                        // Check if this ViewHolder is still bound to the same position
                        if (holder.position != position) {
                            return;
                        }
                        
                        if (eventDoc.exists()) {
                            String extractedOrganizerId = extractOrganizerIdFromEvent(eventDoc);
                            android.util.Log.d("AdminNotificationLogAdapter", "Extracted organizerId from event: " + extractedOrganizerId);
                            
                            if (extractedOrganizerId != null && !extractedOrganizerId.isEmpty()) {
                                // Now load the organizer name
                                firestore.collection("users").document(extractedOrganizerId)
                                        .get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (holder.position != position) return;
                                            
                                            if (userDoc.exists()) {
                                                // Try "Name" first, then "name"
                                                String name = userDoc.getString("Name");
                                                if (name == null || name.isEmpty()) name = userDoc.getString("name");
                                                String email = userDoc.getString("email");
                                                if (email == null || email.isEmpty()) email = userDoc.getString("Email");
                                                
                                                android.util.Log.d("AdminNotificationLogAdapter", "Organizer from event - name: " + name + ", email: " + email);
                                                
                                                String displayName = (name != null && !name.isEmpty()) ? name : 
                                                                    (email != null && !email.isEmpty() ? email : "Unknown Organizer");
                                                holder.organizerNameText.setText("From: " + displayName);
                                            } else {
                                                android.util.Log.w("AdminNotificationLogAdapter", "Organizer user doc not found: " + extractedOrganizerId);
                                                holder.organizerNameText.setText("From: Unknown Organizer");
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            if (holder.position != position) return;
                                            android.util.Log.e("AdminNotificationLogAdapter", "Error loading organizer user", e);
                                            holder.organizerNameText.setText("From: Unknown Organizer");
                                        });
                            } else {
                                android.util.Log.w("AdminNotificationLogAdapter", "Could not extract organizerId from event");
                                holder.organizerNameText.setText("From: Unknown Organizer");
                            }
                        } else {
                            android.util.Log.w("AdminNotificationLogAdapter", "Event document not found: " + log.eventId);
                            holder.organizerNameText.setText("From: Unknown Organizer");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (holder.position != position) return;
                        android.util.Log.e("AdminNotificationLogAdapter", "Error loading event", e);
                        holder.organizerNameText.setText("From: Unknown Organizer");
                    });
        } else {
            holder.organizerNameText.setText("From: Unknown Organizer");
        }
        
        // Load recipient name
        if (log.recipientUserId != null && !log.recipientUserId.isEmpty()) {
            // Remove quotes if present (sometimes userId might be stored with quotes)
            String cleanUserId = log.recipientUserId.replace("\"", "").trim();
            
            firestore.collection("users").document(cleanUserId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        // Check if this ViewHolder is still bound to the same position
                        if (holder.position != position) {
                            return;
                        }
                        
                        if (doc.exists()) {
                            // Try "Name" first, then "name"
                            String name = doc.getString("Name");
                            if (name == null || name.isEmpty()) name = doc.getString("name");
                            String email = doc.getString("email");
                            if (email == null || email.isEmpty()) email = doc.getString("Email");
                            String role = doc.getString("role");
                            
                            android.util.Log.d("AdminNotificationLogAdapter", "Recipient found - name: " + name + ", email: " + email + ", role: " + role);
                            
                            String displayName = (name != null && !name.isEmpty()) ? name : 
                                                (email != null && !email.isEmpty() ? email : "Unknown User");
                            holder.recipientNameText.setText("To: " + displayName);
                        } else {
                            android.util.Log.w("AdminNotificationLogAdapter", "Recipient document not found: " + cleanUserId + " (original: " + log.recipientUserId + ")");
                            // Try to show at least the userId if document doesn't exist
                            holder.recipientNameText.setText("To: User " + cleanUserId.substring(0, Math.min(8, cleanUserId.length())) + "...");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (holder.position != position) return;
                        android.util.Log.e("AdminNotificationLogAdapter", "Error loading recipient: " + cleanUserId, e);
                        holder.recipientNameText.setText("To: Error loading");
                    });
        } else {
            android.util.Log.w("AdminNotificationLogAdapter", "Recipient userId is null or empty");
            holder.recipientNameText.setText("To: Unknown Entrant");
        }
        
        // Load event name
        if (log.eventId != null && !log.eventId.isEmpty()) {
            firestore.collection("Events").document(log.eventId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String eventName = doc.getString("Name");
                            if (eventName == null) eventName = doc.getString("title");
                            holder.eventNameText.setText("Event: " + (eventName != null ? eventName : "Unknown Event"));
                        } else {
                            holder.eventNameText.setText("Event: Unknown Event");
                        }
                    })
                    .addOnFailureListener(e -> {
                        holder.eventNameText.setText("Event: Error loading");
                    });
        } else {
            holder.eventNameText.setText("Event: Unknown Event");
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    /**
     * Holds view references for one notification log row.
     */
    static class NotificationLogViewHolder extends RecyclerView.ViewHolder {

        TextView titleText;
        TextView messageText;
        TextView organizerNameText;
        TextView recipientNameText;
        TextView eventNameText;
        TextView timestampText;
        int position = -1; // Track position to prevent RecyclerView recycling issues

        public NotificationLogViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.adminNotificationLogTitle);
            messageText = itemView.findViewById(R.id.adminNotificationLogMessage);
            organizerNameText = itemView.findViewById(R.id.adminNotificationLogOrganizer);
            recipientNameText = itemView.findViewById(R.id.adminNotificationLogRecipient);
            eventNameText = itemView.findViewById(R.id.adminNotificationLogEvent);
            timestampText = itemView.findViewById(R.id.adminNotificationLogTimestamp);
        }
    }
}

