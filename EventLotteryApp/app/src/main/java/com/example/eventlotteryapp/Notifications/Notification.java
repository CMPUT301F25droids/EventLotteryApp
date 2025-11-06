package com.example.eventlotteryapp.Notifications;

import com.example.eventlotteryapp.Helpers.DateTimeFormat;
import com.example.eventlotteryapp.Helpers.RelativeTime;
import com.google.firebase.firestore.DocumentSnapshot;

import java.time.LocalDateTime;
import java.util.Date;

public class Notification implements Comparable<Notification>{
    public static Object Helpers;

    public enum NotificationType{
        LOTTERY,
        MESSAGE
    }
    private final Date timeStamp;
    private final NotificationType type;
    private final String message;

    public Notification(Date timeStamp, NotificationType type, String message){
        this.timeStamp = timeStamp;
        this.type = type;
        this.message = message;
    }

    public static Notification fromDocument(DocumentSnapshot doc) {
        String message = doc.getString("message");
        NotificationType type = doc.getString("type").equals("Message") ?
                NotificationType.MESSAGE : NotificationType.LOTTERY;
        Date ts = DateTimeFormat.toDate(doc.getString("timeStamp"));

        return new Notification(ts, type, message);
    }
    
    public String getRelevantTime() {
        return RelativeTime.getRelativeTime(timeStamp);
    }

    // Return type as string
    public String getType() {
        if (type == Notification.NotificationType.MESSAGE) {
            return "Message";
        } else {
            return "Lottery";
        }
    }

    public String getMessage() {
        return message;
    }

    // Implement compareTo for ordering by relevant time
    @Override
    public int compareTo(Notification other) {
        // Sort by timestamp descending (newest first)
        return other.timeStamp.compareTo(this.timeStamp);
    }
}
