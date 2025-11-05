package com.example.eventlotteryapp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.*;

public class NotificationController {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Sends a notification message to all entrants in the waiting list.
     */
    public void sendToWaitingList(String eventId, String title, String message) {
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> waitingList = (List<String>) eventDoc.get("waitingListEntrantIds");
            sendBulkNotifications(waitingList, title, message, eventId);
        });
    }

    /**
     * Sends a notification message to all selected entrants.
     */
    public void sendToSelectedEntrants(String eventId, String title, String message) {
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> selected = (List<String>) eventDoc.get("selectedEntrantIds");
            sendBulkNotifications(selected, title, message, eventId);
        });
    }

    /**
     * Sends a notification message to all cancelled entrants.
     */
    public void sendToCancelledEntrants(String eventId, String title, String message) {
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> cancelled = (List<String>) eventDoc.get("cancelledEntrantIds");
            sendBulkNotifications(cancelled, title, message, eventId);
        });
    }

    /**
     * Helper function to send a bulk message and skip opted-out entrants.
     */
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

    /**
     * Actually sends the notification and logs it in Firestore.
     */
    private void sendNotificationToEntrant(DocumentSnapshot entrantDoc, String title, String message, String eventId) {
        String entrantId = entrantDoc.getId();
        String entrantEmail = entrantDoc.getString("email");


    }
}
