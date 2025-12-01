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
     * Always sets UserType to "entrant" for entrant notifications, regardless of recipient's role.
     * 
     * @param entrantIds the list of entrant user IDs to send notifications to
     * @param title the notification title
     * @param message the notification message body
     * @param eventId the ID of the event this notification is related to
     * @param organizerId the ID of the organizer sending the notification (for admin logs)
     */
    private void sendBulkNotifications(List<String> entrantIds, String title, String message, String eventId, String organizerId) {
        if (entrantIds == null || entrantIds.isEmpty()) return;

        for (String userId : entrantIds) {
            db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                Boolean notificationsEnabled = userDoc.getBoolean("notificationPreference");
                if (notificationsEnabled == null || notificationsEnabled) {
                    System.out.println("Sending notification to user: " + userId);
                    // Always set UserType to "entrant" for entrant notifications, regardless of recipient's role
                    sendNotificationToUser(userDoc, title, message, eventId, organizerId, "entrant");
                }
            });
        }
    }

    /**
     * Actually sends a notification and logs it in Firestore.
     * Creates a notification document in the "Notifications" collection and prepares
     * an FCM payload for push notification delivery.
     * Default: determines UserType from recipient's role.
     * 
     * @param userDoc the Firestore document snapshot of the user
     * @param title the notification title
     * @param message the notification message body
     * @param eventId the ID of the event this notification is related to
     * @param organizerId the ID of the organizer sending the notification (for admin logs)
     */
    private void sendNotificationToUser(DocumentSnapshot userDoc, String title, String message, String eventId, String organizerId) {
        // Default: determine UserType from recipient's role
        String role = userDoc.getString("role");
        String userType = (role != null && role.equals("organizer")) ? "organizer" : "entrant";
        sendNotificationToUser(userDoc, title, message, eventId, organizerId, userType);
    }

    /**
     * Actually sends a notification and logs it in Firestore with explicit UserType.
     * Creates a notification document in the "Notifications" collection and prepares
     * an FCM payload for push notification delivery.
     * 
     * @param userDoc the Firestore document snapshot of the user
     * @param title the notification title
     * @param message the notification message body
     * @param eventId the ID of the event this notification is related to
     * @param organizerId the ID of the organizer sending the notification (for admin logs)
     * @param userType the type of notification: "entrant" or "organizer" (based on context, not recipient's role)
     */
    private void sendNotificationToUser(DocumentSnapshot userDoc, String title, String message, String eventId, String organizerId, String userType) {
        System.out.println("Notification.....");

        // 1. Make sure the user has an FCM token (device registered)
        String fcmToken = userDoc.getString("fcmToken");
//        if (fcmToken == null) {
//            extracted();
//            return; // No device token = cannot send push notification
//        }
        System.out.println("Notification1111");

        // 3. Build a notification object to save
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("Message", message);
        notifData.put("Title", title);
        notifData.put("EventId", db.collection("Events").document(eventId));
        notifData.put("Type", "MESSAGE");
        notifData.put("TimeStamp", new Date());
        notifData.put("UserId", userDoc.getId());
        notifData.put("UserType", userType); // Separate logs for entrants and organizers based on notification context
        // Store organizerId for admin review logs
        if (organizerId != null && !organizerId.isEmpty()) {
            notifData.put("OrganizerId", organizerId);
        }

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
     * Helper method to extract organizerId from event document.
     * Handles both DocumentReference ("Organizer") and string ("organizerId") formats.
     * 
     * @param eventDoc the Firestore document snapshot of the event
     * @return the organizer ID as a string, or null if not found
     */
    private String extractOrganizerId(com.google.firebase.firestore.DocumentSnapshot eventDoc) {
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
     * Sends notifications to all entrants currently on the waiting list for an event.
     * 
     * @param eventId the ID of the event
     * @param title the notification title
     * @param message the notification message body
     */
    public void sendToWaitingList(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> waitingList = (List<String>) eventDoc.get("waitingListEntrantIds");
            String organizerId = extractOrganizerId(eventDoc);
            sendBulkNotifications(waitingList, title, message, eventId, organizerId);
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
            String organizerId = extractOrganizerId(eventDoc);
            sendBulkNotifications(selected, title, message, eventId, organizerId);
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
            String organizerId = extractOrganizerId(eventDoc);
            sendBulkNotifications(cancelled, title, message, eventId, organizerId);
        });
    }

    /**
     * Sends notifications to all entrants who accepted their invitation.
     * 
     * @param eventId the ID of the event
     * @param title the notification title
     * @param message the notification message body
     */
    public void sendToAcceptedEntrants(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<String> accepted = (List<String>) eventDoc.get("acceptedEntrantIds");
            String organizerId = extractOrganizerId(eventDoc);
            sendBulkNotifications(accepted, title, message, eventId, organizerId);
        });
    }

    /**
     * Sends notifications to all entrants who signed up for the event.
     * This includes waiting list, selected, and accepted entrants.
     * 
     * @param eventId the ID of the event
     * @param title the notification title
     * @param message the notification message body
     */
    public void sendToAllSignedUpEntrants(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            String organizerId = extractOrganizerId(eventDoc);
            
            // Send to waiting list
            List<String> waitingList = (List<String>) eventDoc.get("waitingListEntrantIds");
            sendBulkNotifications(waitingList, title, message, eventId, organizerId);
            
            // Send to selected entrants
            List<String> selected = (List<String>) eventDoc.get("selectedEntrantIds");
            sendBulkNotifications(selected, title, message, eventId, organizerId);
            
            // Send to accepted entrants
            List<String> accepted = (List<String>) eventDoc.get("acceptedEntrantIds");
            sendBulkNotifications(accepted, title, message, eventId, organizerId);
        });
    }

    /** Send notification to the organizer of an event */
    public void sendToOrganizer(String eventId, String title, String message) {
        db.collection("Events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            if (eventDoc.exists()) {
                String organizerId = extractOrganizerId(eventDoc);
                if (organizerId != null && !organizerId.isEmpty()) {
                    db.collection("users").document(organizerId).get().addOnSuccessListener(organizerDoc -> {
                        Boolean notificationsEnabled = organizerDoc.getBoolean("notificationPreference");
                        if (notificationsEnabled == null || notificationsEnabled) {
                            System.out.println("Sending notification to organizer: " + organizerId);
                            sendNotificationToUser(organizerDoc, title, message, eventId, null);
                        }
                    });
                }
            }
        });
    }

}
