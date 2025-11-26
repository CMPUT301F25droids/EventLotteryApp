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

        String typeString = doc.getString("type");
        NotificationType type = NotificationType.MESSAGE;
        if (typeString != null && typeString.equalsIgnoreCase("lottery")) {
            type = NotificationType.LOTTERY;
        }

        com.google.firebase.Timestamp ts = doc.getTimestamp("timeStamp");
        Date date = ts != null ? ts.toDate() : new Date();

        return new Notification(date, type, message);
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
