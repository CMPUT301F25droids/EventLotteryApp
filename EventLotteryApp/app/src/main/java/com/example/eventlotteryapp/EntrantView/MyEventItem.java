package com.example.eventlotteryapp.EntrantView;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Represents an event item with status information for the current user.
 * Extends EventItem to include lottery status (pending, selected, not selected, or unknown).
 * Used in the "My Events" view to show the user's status for each event they've joined.
 * 
 * @author Droids Team
 */
public class MyEventItem extends EventItem{
    /**
     * Enumeration of possible statuses for an event from the user's perspective.
     */
    public enum Status {
        /** User is on the waiting list, lottery has not run yet. */
        PENDING,
        
        /** User has been selected in the lottery or has accepted an invitation. */
        SELECTED,
        
        /** User was not selected in the lottery or has declined/cancelled. */
        NOT_SELECTED,
        
        /** Status could not be determined. */
        UNKNOWN
    }

    /** The current status of this event for the user. */
    private Status status;

    /**
     * Default constructor required for Firestore deserialization.
     */
    public MyEventItem() {
        super();
    }

    /**
     * Creates a MyEventItem from a Firestore document snapshot.
     * This method is currently not implemented.
     * 
     * @param doc the Firestore document snapshot
     */
    public static void fromDocument(DocumentSnapshot doc) {

    }

    /**
     * Sets the status of this event for the user.
     * 
     * @param status the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the status of this event for the user.
     * 
     * @return the current status
     */
    public Status getStatus() {
        return status;
    }
}
