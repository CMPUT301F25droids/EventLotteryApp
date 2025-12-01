package com.example.eventlotteryapp.Admin;

/**
 * Represents a simplified admin-side view of an event.
 * This model is used when displaying event summaries
 * inside the Admin panel (e.g., Browse Events).
 */
public class AdminEvent {

    private final String eventId;
    private final String title;
    private final String location;

    /**
     * Creates a new AdminEvent instance.
     *
     * @param eventId  The Firestore document ID of the event.
     * @param title    The title of the event.
     * @param location The event's location.
     */
    public AdminEvent(String eventId, String title, String location) {
        this.eventId = eventId;
        this.title = title;
        this.location = location;
    }

    /**
     * @return The Firestore document ID for this event.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * @return The title of the event.
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return The location of the event.
     */
    public String getLocation() {
        return location;
    }
}
