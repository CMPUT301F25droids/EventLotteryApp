package com.example.eventlotteryapp;

import com.example.eventlotteryapp.Notifications.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing and sending notifications to users.
 * Handles bulk notification sending to different groups of entrants
 * (waiting list, selected, cancelled) while respecting user notification preferences.
 * Notifications are saved to Firestore and can be sent via FCM (Firebase Cloud Messaging).
 * 
 * @author Droids Team
 */
public class NotificationController {

    /** Firestore database instance for saving notifications and checking user preferences. */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Sends notifications to all entrants in a list, respecting their opt-out preferences.
     * Only sends notifications to users who have notifications enabled.
     * 
     * @param entrantIds the list of entrant user IDs to send notifications to
     * @param title the notification title
     * @param message the notification message body
     * @param eventId the ID of the event this notification is related to
     */
    private void sendBulkNotifications(List<String> entrantIds, String title, String message, String eventId) {
        if (entrantIds == null || entrantIds.isEmpty()) return;

        for (String userId : entrantIds) {
            db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                Boolean notificationsEnabled = userDoc.getBoolean("notificationPreference");
                if (notificationsEnabled == null || notificationsEnabled) {
                    System.out.println("Sending notification to user: " + userId);
                    sendNotificationToUser(userDoc, title, message, eventId);
                }
            });
        }
    }

    /**
     * Actually sends a notification and logs it in Firestore.
     * Creates a notification document in the "Notifications" collection and prepares
     * an FCM payload for push notification delivery.
     * 
     * @param userDoc the Firestore document snapshot of the user
     * @param title the notification title
     * @param message the notification message body
     * @param eventId the ID of the event this notification is related to
     */
    private void sendNotificationToUser(DocumentSnapshot userDoc, String title, String message, String eventId) {
        System.out.println("Notification.....");

        // 1. Make sure the user has an FCM token (device registered)
        String fcmToken = userDoc.getString("fcmToken");
//        if (fcmToken == null) {
//            extracted();
//            return; // No device token = cannot send push notification
//        }
        System.out.println("Notification1111");

        // 2. Determine UserType based on recipient's role
        String role = userDoc.getString("role");
        String userType = (role != null && role.equals("organizer")) ? "organizer" : "entrant";

        // 3. Build a notification object to save
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("Message", message);
        notifData.put("Title", title);
        notifData.put("EventId", db.collection("Events").document(eventId));
        notifData.put("Type", "MESSAGE");
        notifData.put("TimeStamp", new Date());
        notifData.put("UserId", userDoc.getId());
        notifData.put("UserType", userType); // Separate logs for entrants and organizers based on recipient's role

        // 3. Save into Firestore under /notifications
        db.collection("Notifications")
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

    private static void extracted() {
        return;
    }

    /**
     * Sends notifications to all entrants currently on the waiting list for an event.
     * 
     * @param eventId the ID of the event
     * @param title the notification title
     * @param message the notification message body
     */
    public void sendToWaitingList(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> waitingList = (List<String>) eventDoc.get("waitingListEntrantIds");
            sendBulkNotifications(waitingList, title, message, eventId);
        });
    }

    /**
     * Sends notifications to all entrants who were selected in the lottery.
     * 
     * @param eventId the ID of the event
     * @param title the notification title
     * @param message the notification message body
     */
    public void sendToSelectedEntrants(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> selected = (List<String>) eventDoc.get("selectedEntrantIds");
            sendBulkNotifications(selected, title, message, eventId);
        });
    }

    /**
     * Sends notifications to all entrants who cancelled their registration.
     * 
     * @param eventId the ID of the event
     * @param title the notification title
     * @param message the notification message body
     */
    public void sendToCancelledEntrants(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> cancelled = (List<String>) eventDoc.get("cancelledEntrantIds");
            sendBulkNotifications(cancelled, title, message, eventId);
        });
    }

    /** Send notification to the organizer of an event */
    public void sendToOrganizer(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            if (eventDoc.exists()) {
                String organizerId = eventDoc.getString("organizerId");
                if (organizerId != null && !organizerId.isEmpty()) {
                    db.collection("users").document(organizerId).get().addOnSuccessListener(organizerDoc -> {
                        Boolean notificationsEnabled = organizerDoc.getBoolean("notificationPreference");
                        if (notificationsEnabled == null || notificationsEnabled) {
                            System.out.println("Sending notification to organizer: " + organizerId);
                            sendNotificationToUser(organizerDoc, title, message, eventId);
                        }
                    });
                }
            }
        });
    }

}
