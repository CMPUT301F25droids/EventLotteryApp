package com.example.eventlotteryapp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for retrieving event statistics from Firestore.
 * Provides methods to query and calculate various event metrics such as
 * waiting list counts and participant statistics.
 * 
 * @author Droids Team
 */
public class EventStatsController {
    /** Firestore database instance for querying event data. */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Returns the number of entrants currently on the waiting list for a given event.
     * 
     * @param eventId the unique identifier of the event
     * @return a CompletableFuture that will complete with the waiting list count,
     *         or complete exceptionally if an error occurs
     */
    public CompletableFuture<Integer> getWaitingListCount(String eventId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        db.collection("Events").document(eventId).get().addOnSuccessListener(doc -> {
            List<String> waitingList = (List<String>) doc.get("waitingListEntrantIds");
            int count = (waitingList == null) ? 0 : waitingList.size();
            future.complete(count);
        }).addOnFailureListener(future::completeExceptionally);

        return future;
    }
}
