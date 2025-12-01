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

            // Create notification
            createNotification(userId, eventId, "invitation_accepted",
                    "You've successfully accepted the invitation!");

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

        db.runTransaction((Transaction.Function<DocumentReference>) transaction -> {
            // Get current event data
            DocumentSnapshot eventDoc = transaction.get(eventRef);
            List<String> selectedEntrants = (List<String>) eventDoc.get("selectedEntrantIds");
            List<String> declinedEntrants = (List<String>) eventDoc.get("declinedEntrantIds");
            DocumentReference organizerRef = eventDoc.getDocumentReference("Organizer");

            if (selectedEntrants == null) selectedEntrants = new ArrayList<>();
            if (declinedEntrants == null) declinedEntrants = new ArrayList<>();

            // Remove from selected, add to declined
            selectedEntrants.remove(userId);
            if (!declinedEntrants.contains(userId)) {
                declinedEntrants.add(userId);
            }

            transaction.update(eventRef, "selectedEntrantIds", selectedEntrants);
            transaction.update(eventRef, "declinedEntrantIds", declinedEntrants);

            return organizerRef;
        }).addOnSuccessListener(organizerRef -> {
            Log.d(TAG, "Invitation declined successfully");
            
            // Create notification for the person who declined
            createNotification(userId, eventId, "invitation_declined",
                    "You've declined the invitation. Thank you for letting us know.");
            
            // Notify the organizer
            if (organizerRef != null) {
                // Get event name and user name for the notification
                eventRef.get().addOnSuccessListener(eventDoc -> {
                    String eventNameRaw = eventDoc.getString("Name");
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
                        
                        // Get organizer's user ID
                        organizerRef.get().addOnSuccessListener(organizerDoc -> {
                            if (organizerDoc.exists()) {
                                String organizerId = organizerDoc.getId();
                                createNotification(organizerId, eventId, "entrant_declined",
                                        notificationMessage);
                                Log.d(TAG, "Organizer notified about decline: " + organizerId);
                            }
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting organizer document", e);
                        });
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting user document", e);
                        // Still notify organizer even if we can't get user name
                        final String fallbackMessage = "An entrant has declined their invitation for " + eventName + ". " +
                                "You may want to run another lottery draw to fill the spot, but it's your choice.";
                        organizerRef.get().addOnSuccessListener(organizerDoc -> {
                            if (organizerDoc.exists()) {
                                String organizerId = organizerDoc.getId();
                                createNotification(organizerId, eventId, "entrant_declined",
                                        fallbackMessage);
                            }
                        });
                    });
                });
            }
            
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error declining invitation", e);
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    /**
     * Helper: Create notification for user (only if notifications are enabled)
     */
    private void createNotification(String userId, String eventId, String type, String message) {
        // Check if user has notifications enabled
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            Boolean notificationsEnabled = userDoc.getBoolean("notificationPreference");
            // If notificationPreference is null or true, send notification
            if (notificationsEnabled == null || notificationsEnabled) {
                // Get user's role to determine UserType
                String role = userDoc.getString("role");
                String userType = (role != null && role.equals("organizer")) ? "organizer" : "entrant";
                
                Map<String, Object> notification = new HashMap<>();
                notification.put("UserId", userId);  // Store as string, not DocumentReference
                notification.put("EventId", db.collection("Events").document(eventId));
                notification.put("Type", type);
                notification.put("Message", message);
                notification.put("TimeStamp", new java.util.Date().toString());
                notification.put("Read", false);
                notification.put("UserType", userType); // Separate logs for entrants and organizers

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