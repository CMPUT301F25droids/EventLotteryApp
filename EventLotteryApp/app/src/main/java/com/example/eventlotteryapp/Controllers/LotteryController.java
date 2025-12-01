package com.example.eventlotteryapp.Controllers;

import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing lottery operations
 * User stories:
 * - US 01.05.02: Accept invitation
 * - US 01.05.03: Decline invitation
 * @author Rayyan
 */
public class LotteryController {
    private static final String TAG = "LotteryController";
    private final FirebaseFirestore db;

    /**
     * Constructs a new LotteryController instance.
     * Initializes the Firestore database connection.
     */
    public LotteryController() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * US 01.05.02: Accept invitation to participate in event
     * Moves user from selectedEntrantIds to acceptedEntrantIds list
     */
    public void acceptInvitation(String eventId, String userId, AcceptCallback callback) {
        DocumentReference eventRef = db.collection("Events").document(eventId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Get current event data
            List<String> selectedEntrants = (List<String>) transaction.get(eventRef).get("selectedEntrantIds");
            List<String> acceptedEntrants = (List<String>) transaction.get(eventRef).get("acceptedEntrantIds");

            if (selectedEntrants == null) selectedEntrants = new ArrayList<>();
            if (acceptedEntrants == null) acceptedEntrants = new ArrayList<>();

            // Remove from selected, add to accepted
            selectedEntrants.remove(userId);
            if (!acceptedEntrants.contains(userId)) {
                acceptedEntrants.add(userId);
            }

            transaction.update(eventRef, "selectedEntrantIds", selectedEntrants);
            transaction.update(eventRef, "acceptedEntrantIds", acceptedEntrants);

            // Create notification (entrant notification)
            createNotification(userId, eventId, "invitation_accepted",
                    "You've successfully accepted the invitation!", "entrant");

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Invitation accepted successfully");
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error accepting invitation", e);
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    /**
     * US 01.05.03: Decline invitation to participate in event
     * Moves user from selectedEntrantIds to declinedEntrantIds
     * Notifies the organizer that someone declined and suggests they redraw
     */
    public void declineInvitation(String eventId, String userId, DeclineCallback callback) {
        DocumentReference eventRef = db.collection("Events").document(eventId);

        db.runTransaction((Transaction.Function<String>) transaction -> {
            // Get current event data
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            List<String> selectedEntrants = (List<String>) eventDoc.get("selectedEntrantIds");
            List<String> declinedEntrants = (List<String>) eventDoc.get("declinedEntrantIds");
            List<String> waitingListEntrants = (List<String>) eventDoc.get("waitingListEntrantIds");
            
            // Try to get organizer ID from DocumentReference or string field
            String organizerId = null;
            DocumentReference organizerRef = eventDoc.getDocumentReference("Organizer");
            if (organizerRef != null) {
                // Extract organizer ID from DocumentReference path
                String path = organizerRef.getPath();
                if (path != null && path.contains("/users/")) {
                    organizerId = path.substring(path.lastIndexOf("/") + 1);
                }
            }
            
            // Fallback: try to get organizerId as a string field
            if (organizerId == null || organizerId.isEmpty()) {
                organizerId = eventDoc.getString("organizerId");
            }

            if (selectedEntrants == null) selectedEntrants = new ArrayList<>();
            if (declinedEntrants == null) declinedEntrants = new ArrayList<>();
            if (waitingListEntrants == null) waitingListEntrants = new ArrayList<>();

            // Remove from selected and waiting list, add to declined
            selectedEntrants.remove(userId);
            waitingListEntrants.remove(userId);
            if (!declinedEntrants.contains(userId)) {
                declinedEntrants.add(userId);
            }

            transaction.update(eventRef, "selectedEntrantIds", selectedEntrants);
            transaction.update(eventRef, "waitingListEntrantIds", waitingListEntrants);
            transaction.update(eventRef, "declinedEntrantIds", declinedEntrants);

            return organizerId;
        }).addOnSuccessListener(organizerId -> {
            Log.d(TAG, "Invitation declined successfully");
            
            // Create notification for the person who declined (entrant notification)
            createNotification(userId, eventId, "invitation_declined",
                    "You've declined the invitation. Thank you for letting us know.", "entrant");
            
            // Notify the organizer
            if (organizerId != null && !organizerId.isEmpty()) {
                // Get event name and user name for the notification
                eventRef.get().addOnSuccessListener(eventDoc -> {
                    String eventNameRaw = eventDoc.getString("Name");
                    if (eventNameRaw == null || eventNameRaw.isEmpty()) {
                        eventNameRaw = eventDoc.getString("title");
                    }
                    final String eventName = (eventNameRaw == null || eventNameRaw.isEmpty()) 
                            ? "your event" 
                            : eventNameRaw;
                    
                    // Get the user's name who declined
                    db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                        String userNameRaw = userDoc.getString("name");
                        final String userName = (userNameRaw == null || userNameRaw.isEmpty()) 
                                ? "An entrant" 
                                : userNameRaw;
                        
                        final String notificationMessage = userName + " has declined their invitation for " + eventName + ". " +
                                "You may want to run another lottery draw to fill the spot, but it's your choice.";
                        
                        // Notify organizer directly using the organizer ID (organizer notification)
                        createNotification(organizerId, eventId, "entrant_declined", notificationMessage, "organizer");
                        Log.d(TAG, "Organizer notified about decline: " + organizerId);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting user document", e);
                        // Still notify organizer even if we can't get user name
                        final String fallbackMessage = "An entrant has declined their invitation for " + eventName + ". " +
                                "You may want to run another lottery draw to fill the spot, but it's your choice.";
                        createNotification(organizerId, eventId, "entrant_declined", fallbackMessage, "organizer");
                        Log.d(TAG, "Organizer notified about decline (fallback): " + organizerId);
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting event document for notification", e);
                    // Still try to notify organizer with minimal info
                    final String fallbackMessage = "An entrant has declined their invitation. " +
                            "You may want to run another lottery draw to fill the spot, but it's your choice.";
                    createNotification(organizerId, eventId, "entrant_declined", fallbackMessage, "organizer");
                    Log.d(TAG, "Organizer notified about decline (minimal info): " + organizerId);
                });
            } else {
                Log.w(TAG, "Cannot notify organizer: organizerId is null or empty for event " + eventId);
            }
            
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error declining invitation", e);
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    /**
     * Helper: Create notification for user (only if notifications are enabled)
     * @param userType The type of notification: "entrant" or "organizer". 
     *                 This should be based on the context of the notification, not the recipient's role field.
     */
    private void createNotification(String userId, String eventId, String type, String message, String userType) {
        // Check if user has notifications enabled
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            Boolean notificationsEnabled = userDoc.getBoolean("notificationPreference");
            // If notificationPreference is null or true, send notification
            if (notificationsEnabled == null || notificationsEnabled) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("UserId", userId);  // Store as string, not DocumentReference
                notification.put("EventId", db.collection("Events").document(eventId));
                notification.put("Type", type);
                notification.put("Message", message);
                notification.put("TimeStamp", new java.util.Date().toString());
                notification.put("Read", false);
                notification.put("UserType", userType); // Use the explicitly provided UserType

                db.collection("Notifications").add(notification)
                        .addOnSuccessListener(ref -> Log.d(TAG, "Notification created: " + ref.getId()))
                        .addOnFailureListener(e -> Log.e(TAG, "Error creating notification", e));
            } else {
                Log.d(TAG, "Notification not sent - user has notifications disabled");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking notification preference", e);
        });
    }

    // Callback interfaces
    public interface AcceptCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface DeclineCallback {
        void onSuccess();
        void onFailure(String error);
    }
}