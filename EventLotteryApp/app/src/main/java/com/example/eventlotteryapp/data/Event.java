package com.example.eventlotteryapp.data;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public record Event(
    String title,
    String description,
    String location,
    double price,
    Date eventStartDate,
    Date eventEndDate,
    Date registrationOpenDate,
    Date registrationCloseDate,
    int maxParticipants,
    String organizerId,
    @ServerTimestamp Date createdAt
) {
    // No-arg constructor for Firestore
    public Event() {
        this("", "", "", 0.0, null, null, null, null, 0, "", null);
    }
}
