package com.example.eventlotteryapp;

import com.example.eventlotteryapp.Notifications.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationController {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Send notifications to all entrants in a list, respecting opt-out */
    private void sendBulkNotifications(List<String> entrantIds, String title, String message, String eventId) {
        if (entrantIds == null || entrantIds.isEmpty()) return;

        for (String entrantId : entrantIds) {
            db.collection("entrants").document(entrantId).get().addOnSuccessListener(entrantDoc -> {
                Boolean notificationsEnabled = entrantDoc.getBoolean("notificationsEnabled");
                if (notificationsEnabled != null && notificationsEnabled) {
                    sendNotificationToEntrant(entrantDoc, title, message, eventId);
                }
            });
        }
    }

    //** Actually sends a notification AND logs it in Firestore */
    private void sendNotificationToEntrant(DocumentSnapshot entrantDoc, String title, String message, String eventId) {

        // 1. Make sure the user has an FCM token (device registered)
        String fcmToken = entrantDoc.getString("fcmToken");
        if (fcmToken == null) {
            return; // No device token = cannot send push notification
        }

        // 2. Build a notification object to save
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("message", message);
        notifData.put("title", title);
        notifData.put("eventId", eventId);
        notifData.put("type", "MESSAGE");
        notifData.put("timeStamp", new Date());

        // 3. Save into Firestore under /notifications
        db.collection("notifications")
                .add(notifData)
                .addOnSuccessListener(docRef -> {
                    // Optional: log success
                    System.out.println("Notification saved: " + docRef.getId());
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error saving notification: " + e.getMessage());
                });

        // 4. Send push notification via FCM
        Map<String, String> fcmPayload = new HashMap<>();
        fcmPayload.put("title", title);
        fcmPayload.put("body", message);

        // Here you would send using your backend or Cloud Function
    }

    /** Public methods to send notifications to different event groups */
    public void sendToWaitingList(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> waitingList = (List<String>) eventDoc.get("waitingListEntrantIds");
            sendBulkNotifications(waitingList, title, message, eventId);
        });
    }

    public void sendToSelectedEntrants(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> selected = (List<String>) eventDoc.get("selectedEntrantIds");
            sendBulkNotifications(selected, title, message, eventId);
        });
    }

    public void sendToCancelledEntrants(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> cancelled = (List<String>) eventDoc.get("cancelledEntrantIds");
            sendBulkNotifications(cancelled, title, message, eventId);
        });
    }

}
