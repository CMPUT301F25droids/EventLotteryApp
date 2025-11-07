package com.example.eventlotteryapp.Controllers;

import android.util.Log;
import com.google.firebase.firestore.DocumentReference;
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

    public LotteryController() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * US 01.05.02: Accept invitation to participate in event
     * Moves user from SelectedEntrants to AcceptedEntrants list
     */
    public void acceptInvitation(String eventId, String userId, AcceptCallback callback) {
        DocumentReference eventRef = db.collection("Events").document(eventId);
        DocumentReference userRef = db.collection("Users").document(userId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Get current event data
            List<DocumentReference> selectedEntrants =
                    (List<DocumentReference>) transaction.get(eventRef).get("SelectedEntrants");
            List<DocumentReference> acceptedEntrants =
                    (List<DocumentReference>) transaction.get(eventRef).get("AcceptedEntrants");

            if (selectedEntrants == null) selectedEntrants = new ArrayList<>();
            if (acceptedEntrants == null) acceptedEntrants = new ArrayList<>();

            // Remove from selected, add to accepted
            selectedEntrants.remove(userRef);
            acceptedEntrants.add(userRef);

            transaction.update(eventRef, "SelectedEntrants", selectedEntrants);
            transaction.update(eventRef, "AcceptedEntrants", acceptedEntrants);

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
     * Moves user to DeclinedEntrants
     */
    public void declineInvitation(String eventId, String userId, DeclineCallback callback) {
        DocumentReference eventRef = db.collection("Events").document(eventId);
        DocumentReference userRef = db.collection("Users").document(userId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Get current event data
            List<DocumentReference> selectedEntrants =
                    (List<DocumentReference>) transaction.get(eventRef).get("SelectedEntrants");
            List<DocumentReference> declinedEntrants =
                    (List<DocumentReference>) transaction.get(eventRef).get("DeclinedEntrants");

            if (selectedEntrants == null) selectedEntrants = new ArrayList<>();
            if (declinedEntrants == null) declinedEntrants = new ArrayList<>();

            // Remove from selected, add to declined
            selectedEntrants.remove(userRef);
            declinedEntrants.add(userRef);

            transaction.update(eventRef, "SelectedEntrants", selectedEntrants);
            transaction.update(eventRef, "DeclinedEntrants", declinedEntrants);

            // Create notification
            createNotification(userId, eventId, "invitation_declined",
                    "You've declined the invitation. Thank you for letting us know.");

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Invitation declined successfully");
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error declining invitation", e);
            if (callback != null) callback.onFailure(e.getMessage());
        });
    }

    /**
     * Helper: Create notification for user
     */
    private void createNotification(String userId, String eventId, String type, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("UserId", db.collection("Users").document(userId));
        notification.put("EventId", db.collection("Events").document(eventId));
        notification.put("Type", type);
        notification.put("Message", message);
        notification.put("TimeStamp", new java.util.Date().toString());
        notification.put("Read", false);

        db.collection("Notifications").add(notification)
                .addOnSuccessListener(ref -> Log.d(TAG, "Notification created: " + ref.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error creating notification", e));
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