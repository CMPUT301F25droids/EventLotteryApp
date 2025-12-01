package com.example.eventlotteryapp.EntrantView;

import com.google.firebase.firestore.DocumentReference;

import java.util.Date;

/**
 * Represents an event item for display in entrant views.
 * Contains event information including name, cost, organizer, dates, and images.
 * This class is used for Firestore deserialization and display in RecyclerViews.
 * 
 * @author Droids Team
 */
public class EventItem {
    /** The unique identifier of the event (Firestore document ID). */
    private String id;
    
    /** The name or title of the event. */
    private String Name;
    
    /** Reference to the organizer's user document in Firestore. */
    private DocumentReference Organizer;
    
    /** The cost of the event as a formatted string (e.g., "$10.00" or "Free"). */
    private String Cost;
    
    /** The event poster image as a base64-encoded string. */
    private String Image;
    
    /** The date and time when the event starts. */
    private Date eventStartDate;

    /** The date and time when the event ends. */
    private Date eventEndDate;
    
    /** The date and time when registration opens. */
    private Date registrationOpenDate;
    
    /** The date and time when registration closes. */
    private Date registrationCloseDate;
    
    /** The maximum number of participants allowed. */
    private int maxParticipants;

    /** A detailed description of the event. */
    private String description;

    /**
     * Default constructor required for Firestore deserialization.
     */
    public EventItem() {
    }

    /**
     * Gets the event name.
     * 
     * @return the event name
     */
    public String getName() {
        return Name;
    }

    /**
     * Gets the organizer's Firestore document reference.
     * 
     * @return the DocumentReference to the organizer's user document
     */
    public DocumentReference getOrganizer() {
        return Organizer;
    }

    /**
     * Gets the event cost as a formatted string.
     * 
     * @return the cost string (e.g., "$10.00" or "Free")
     */
    public String getCost() {
        return Cost;
    }

    /**
     * Gets the event poster image as a base64-encoded string.
     * 
     * @return the base64-encoded image string, or null if no image
     */
    public String getImage() {
        return Image;
    }
    
    /**
     * Gets the event's unique identifier.
     * 
     * @return the event ID
     */
    public String getId() { return id; }
    
    /**
     * Sets the event's unique identifier.
     * 
     * @param id the event ID to set
     */
    public void setId(String id) { this.id = id; }
    
    /**
     * Gets the event description.
     * 
     * @return the event description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the event start date and time.
     * 
     * @return the event start date, or null if not set
     */
    public Date getEventStartDate() {
        return eventStartDate;
    }

    /**
     * Gets the event end date and time.
     * 
     * @return the event end date, or null if not set
     */
    public Date getEventEndDate() {
        return eventEndDate;
    }

}
