package com.example.eventlotteryapp.Notifications;

import com.example.eventlotteryapp.Helpers.DateTimeFormat;
import com.example.eventlotteryapp.Helpers.RelativeTime;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a notification in the Event Lottery application.
 * Contains notification information including type, message, timestamp, and associated event.
 * Implements Comparable to allow sorting by timestamp (newest first).
 * 
 * @author Droids Team
 */
public class Notification implements Comparable<Notification>{
    /** The type of notification (e.g., "lottery", "invitation_accepted", "MESSAGE"). */
    private final String type;
    
    /** The timestamp when the notification was created. */
    private final Date timeStamp;
    
    /** The notification message content. */
    private final String message;
    
    /** The name of the event this notification is related to. */
    private String eventName;
    
    /** The unique identifier of the event this notification is related to. */
    private String eventId;
    
    /** The Firestore document ID of this notification (used for deletion). */
    private String documentId;

    /**
     * Constructs a new Notification with the specified information.
     * 
     * @param timeStamp the timestamp when the notification was created
     * @param type the type of notification
     * @param message the notification message content
     */
    public Notification(Date timeStamp, String type, String message){
        this.timeStamp = timeStamp;
        this.type = type;
        this.message = message;
        this.eventName = "You have a notification!";
        this.eventId = null;
        this.documentId = null;
    }

    /**
     * Callback interface for asynchronous notification loading from Firestore.
     */
    public interface NotificationLoadCallback {
        /**
         * Called when a notification has been successfully loaded from a Firestore document.
         * 
         * @param notification the loaded notification object
         */
        void onNotificationLoaded(Notification notification);
    }

    /**
     * Creates a Notification object from a Firestore document snapshot.
     * Asynchronously loads the associated event name if an event reference exists.
     * 
     * @param doc the Firestore document snapshot containing notification data
     * @param callback the callback to invoke when the notification is fully loaded
     */
    public static void fromDocument(DocumentSnapshot doc, NotificationLoadCallback callback) {
        String message = doc.getString("Message");
        String type = doc.getString("Type");
        Date date;
        try {
            date = DateTimeFormat.toDate(doc.getString("TimeStamp"));
        } catch (Exception e)  {
            date = doc.getDate("TimeStamp");
        }
        DocumentReference eventRef = doc.getDocumentReference("EventId");
        Notification notification = new Notification(date, type, message);
        // Store the document ID for deletion
        notification.setDocumentId(doc.getId());

        // Try to get EventId as DocumentReference first
        if (eventRef != null) {
            // Store the eventId from the DocumentReference
            notification.setEventId(eventRef.getId());
            eventRef.get().addOnSuccessListener(eventSnap -> {
                if (eventSnap.exists()) {
                    String eventNameRaw = eventSnap.getString("Name");
                    if (eventNameRaw == null || eventNameRaw.isEmpty()) {
                        eventNameRaw = eventSnap.getString("title");
                    }
                    if (eventNameRaw != null && !eventNameRaw.isEmpty()) {
                        notification.setEventName(eventNameRaw);
                    }
                }
                callback.onNotificationLoaded(notification);
            }).addOnFailureListener(e -> {
                callback.onNotificationLoaded(notification);
            });
        } else {
            // Fallback: try to get EventId as a string
            String eventIdString = doc.getString("EventId");
            if (eventIdString != null && !eventIdString.isEmpty()) {
                notification.setEventId(eventIdString);
                // Try to load event name from the event ID
                FirebaseFirestore.getInstance().collection("Events").document(eventIdString)
                        .get()
                        .addOnSuccessListener(eventSnap -> {
                            if (eventSnap.exists()) {
                                String eventNameRaw = eventSnap.getString("Name");
                                if (eventNameRaw == null || eventNameRaw.isEmpty()) {
                                    eventNameRaw = eventSnap.getString("title");
                                }
                                if (eventNameRaw != null && !eventNameRaw.isEmpty()) {
                                    notification.setEventName(eventNameRaw);
                                }
                            }
                            callback.onNotificationLoaded(notification);
                        })
                        .addOnFailureListener(e -> {
                            callback.onNotificationLoaded(notification);
                        });
            } else {
                callback.onNotificationLoaded(notification);
            }
        }
    }

    public String getRelevantTime() {
        return RelativeTime.getRelativeTime(timeStamp);
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    // Return type as string
    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventId() {
        return eventId;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // Implement compareTo for ordering by relevant time
    @Override
    public int compareTo(Notification other) {
        // Sort by timestamp descending (newest first)
        return other.timeStamp.compareTo(this.timeStamp);
    }
}
