package com.example.eventlotteryapp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EventStatsController {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Returns the number of entrants currently on the waiting list.
     */
    public CompletableFuture<Integer> getWaitingListCount(String eventId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        db.collection("events").document(eventId).get().addOnSuccessListener(doc -> {
            List<String> waitingList = (List<String>) doc.get("waitingListEntrantIds");
            int count = (waitingList == null) ? 0 : waitingList.size();
            future.complete(count);
        }).addOnFailureListener(future::completeExceptionally);

        return future;
    }
}
