package com.example.eventlotteryapp.Notifications;

import com.example.eventlotteryapp.Helpers.DateTimeFormat;
import com.example.eventlotteryapp.Helpers.RelativeTime;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

public class Notification implements Comparable<Notification>{
    private final String type;
    private final Date timeStamp;
    private final String message;
    private String eventName;

    public Notification(Date timeStamp, String type, String message){
        this.timeStamp = timeStamp;
        this.type = type;
        this.message = message;
        this.eventName = "Unknown";
    }

    public interface NotificationLoadCallback {
        void onNotificationLoaded(Notification notification);
    }

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

        if (eventRef != null) {
            eventRef.get().addOnSuccessListener(eventSnap -> {
                if (eventSnap.exists()) {
                    notification.setEventName(eventSnap.getString("Name"));
                }
                callback.onNotificationLoaded(notification);
            }).addOnFailureListener(e -> {
                callback.onNotificationLoaded(notification);
            });
        } else {
            callback.onNotificationLoaded(notification);
        }
    }

    public String getRelevantTime() {
        return RelativeTime.getRelativeTime(timeStamp);
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
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

    public Date getTimeStamp() {
        return timeStamp;
    }

    // Implement compareTo for ordering by relevant time
    @Override
    public int compareTo(Notification other) {
        // Sort by timestamp descending (newest first)
        return other.timeStamp.compareTo(this.timeStamp);
    }
}
